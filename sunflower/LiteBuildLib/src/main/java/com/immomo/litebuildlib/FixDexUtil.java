package com.immomo.litebuildlib;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class FixDexUtil {

    private static final String DEX_SUFFIX = ".dex";
    private static final String APK_SUFFIX = ".apk";
    private static final String JAR_SUFFIX = ".jar";
    private static final String ZIP_SUFFIX = ".zip";
    public static final String DEX_DIR = "odex";
    private static final String OPTIMIZE_DEX_DIR = "opt_dex";
    private static HashSet<File> loadedDex = new HashSet<>();

    static {
        loadedDex.clear();
    }

    /**
     * 加载补丁，使用默认目录：data/data/包名/files/odex
     *
     * @param context
     */
    public static void loadFixedDex(Context context) {
        loadFixedDex(context, null);
    }

    /**
     * 加载补丁
     *
     * @param context       上下文
     * @param patchFilesDir 补丁所在目录
     */
    public static void loadFixedDex(Context context, File patchFilesDir) {
        loadedDex.add(patchFilesDir);// 存入集合
        // dex合并之前的dex
        doDexInject(context, loadedDex);
    }

    /**
     *@author Minuit
     *@time 2018/6/25 0025 15:51
     *@desc 验证是否需要热修复
     */
    public static boolean isGoingToFix(Context context) {
        boolean canFix = false;
//        var dexFilePath: String = filesDir.path + "/classes.dex"
        File externalStorageDirectory = new File(context.getFilesDir().getPath() + "/classes.dex");

        // 遍历所有的修复dex , 因为可能是多个dex修复包
        File fileDir = externalStorageDirectory != null ?
                externalStorageDirectory :
                new File(context.getFilesDir(), DEX_DIR);// data/data/包名/files/odex（这个可以任意位置）
        File[] listFiles = fileDir.listFiles();
        for (File file : listFiles) {
            if (file.getName().startsWith("classes") &&
                    (file.getName().endsWith(DEX_SUFFIX)
                            || file.getName().endsWith(APK_SUFFIX)
                            || file.getName().endsWith(JAR_SUFFIX)
                            || file.getName().endsWith(ZIP_SUFFIX))) {

                loadedDex.add(file);// 存入集合
                //有目标dex文件, 需要修复
                canFix = true;
            }
        }
        return canFix;
    }

    private static void doDexInject(Context appContext, HashSet<File> loadedDex) {
        String optimizeDir = appContext.getFilesDir().getAbsolutePath() +
                File.separator + OPTIMIZE_DEX_DIR;
        // data/data/包名/files/optimize_dex（这个必须是自己程序下的目录）

        File fopt = new File(optimizeDir);
        if (!fopt.exists()) {
            fopt.mkdirs();
        }
        try {
            // 1.加载应用程序dex的Loader
            PathClassLoader pathLoader = (PathClassLoader) appContext.getClassLoader();
            for (File dex : loadedDex) {
                // 2.加载指定的修复的dex文件的Loader
                DexClassLoader dexLoader = new DexClassLoader(
                        dex.getAbsolutePath(),// 修复好的dex（补丁）所在目录
                        fopt.getAbsolutePath(),// 存放dex的解压目录（用于jar、zip、apk格式的补丁）
                        null,// 加载dex时需要的库
                        pathLoader// 父类加载器
                );
                // 3.开始合并
                // 合并的目标是Element[],重新赋值它的值即可

                /**
                 * BaseDexClassLoader中有 变量: DexPathList pathList
                 * DexPathList中有 变量 Element[] dexElements
                 * 依次反射即可
                 */

                //3.1 准备好pathList的引用
                Object dexPathList = getPathList(dexLoader);
                Object pathPathList = getPathList(pathLoader);
                //3.2 从pathList中反射出element集合
                Object leftDexElements = getDexElements(dexPathList);
                Object rightDexElements = getDexElements(pathPathList);
                //3.3 合并两个dex数组
                Object dexElements = combineArray(leftDexElements, rightDexElements);

                // 重写给PathList里面的Element[] dexElements;赋值
                Object pathList = getPathList(pathLoader);// 一定要重新获取，不要用pathPathList，会报错
                setField(pathList, pathList.getClass(), "dexElements", dexElements);
            }
            Toast.makeText(appContext, "修复完成", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    /**
     * 反射给对象中的属性重新赋值
     */
    private static void setField(Object obj, Class<?> cl, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = cl.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(obj, value);
    }

    /**
     * 反射得到对象中的属性值
     */
    private static Object getField(Object obj, Class<?> cl, String field) throws NoSuchFieldException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }


    /**
     * 反射得到类加载器中的pathList对象
     */
    private static Object getPathList(Object baseDexClassLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    /**
     * 反射得到pathList中的dexElements
     */
    private static Object getDexElements(Object pathList) throws NoSuchFieldException, IllegalAccessException {
        return getField(pathList, pathList.getClass(), "dexElements");
    }

    /**
     * 数组合并
     */
    private static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> clazz = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);// 得到左数组长度（补丁数组）
        int j = Array.getLength(arrayRhs);// 得到原dex数组长度
        int k = i + j;// 得到总数组长度（补丁数组+原dex数组）
        Object result = Array.newInstance(clazz, k);// 创建一个类型为clazz，长度为k的新数组
        System.arraycopy(arrayLhs, 0, result, 0, i);
        System.arraycopy(arrayRhs, 0, result, i, j);
        return result;
    }

    static String sPatchVersion = null;
    public static String getPatchVersion(Context context) {
        if (sPatchVersion != null && !sPatchVersion.isEmpty()) {
            return sPatchVersion;
        }

        return (String) getBuildConfigValue(context.getPackageName(), "LITEBUILD_VERSION");
    }

    public static File getDexPatchFile(Context context) {
        String patchName = FixDexUtil.getPatchVersion(context) + "_patch.png";
        String dexPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/" + context.getPackageName() + "/patch_file/" + patchName;

        Log.e("weijiangnan", "version" + patchName);
        File dexFile = new File(dexPath);
        if (dexFile.exists()) {
            return dexFile;
        }

        // 拷贝
        File dexFile2 = new File("/sdcard/litebuild/patch_file/" + patchName);
        if (dexFile2.exists()) {
            return dexFile2;
        }

        return null;
    }

    public static File getResourcesPatchFile(Context context) {
        String patchVersion = FixDexUtil.getPatchVersion(context);
        File patchFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/" + context.getApplicationContext().getPackageName() + "/patch_file/apk/"
                + patchVersion + "_resources-debug.png");
        if (patchFile.exists()) {
            return patchFile;
        }

        patchFile = new File("/sdcard/litebuild/patch_file/apk/"
                + patchVersion + "_resources-debug.png");
        if (patchFile.exists()) {
            return patchFile;
        }

        return null;
    }

    public static Object getBuildConfigValue(String packageName, String fieldName) {
        try {
            Class<?> clazz = Class.forName(packageName + ".BuildConfig");
            Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (ClassNotFoundException e) {
            int idx = packageName.lastIndexOf(".");
            if (idx > 0) {
                return getBuildConfigValue(packageName.substring(0, idx), fieldName);
            }

            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
