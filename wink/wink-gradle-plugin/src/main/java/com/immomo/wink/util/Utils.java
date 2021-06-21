/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.immomo.wink.util;

import com.immomo.wink.Settings;

import org.gradle.api.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static List<String> runShell(String shStr) {
//        WinkLog.v("准备运行shell : " + shStr);
        List<String> strList = new ArrayList<String>();
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "`" + shStr + "`"}, null, null);
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            LineNumberReader input = new LineNumberReader(ir);
            String line;
            process.waitFor();
            while ((line = input.readLine()) != null) {
                strList.add(line);
            }

            while ((line = errorReader.readLine()) != null) {
                WinkLog.i(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String str : strList) {
            System.out.print(str);
        }
//        WinkLog.v("结束运行shell : " + shStr);
        return strList;
    }

    public static void executeScript(String cmd) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "`" + cmd + "`"}, null, null);
        p.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));


        String line = "";
        while ((line = reader.readLine()) != null) {
            WinkLog.i(line);
        }

        line = "";
        while ((line = errorReader.readLine()) != null) {
            WinkLog.i(line);
        }
    }

    public static class ShellResult {
        List<String> result = new ArrayList<>();
        List<String> errorResult = new ArrayList<>();
        Exception e;

        public List<String> getResult() {
            return result;
        }

        public ShellResult setResult(List<String> result) {
            this.result = result;
            return this;
        }

        public List<String> getErrorResult() {
            return errorResult;
        }

        public ShellResult setErrorResult(List<String> errorResult) {
            this.errorResult = errorResult;
            return this;
        }

        public Exception getE() {
            return e;
        }

        public ShellResult setE(Exception e) {
            this.e = e;
            return this;
        }
    }

    /**
     * 运行shell并获得结果，注意：如果sh中含有awk,一定要按new String[]{"/bin/sh","-c",shStr}写,才可以获得流
     *
     * @param
     * @return
     */
    public static ShellResult runShells(String... cmds) {
        ShellResult result = new ShellResult();

        List<String> cmdArray = new ArrayList<>();

        cmdArray.add("/bin/sh");
        cmdArray.add("-c");
        for (String cmd: cmds) {
//            cmdArray.add("`" + cmd + "`");
            cmdArray.add(cmd);
            WinkLog.i("Execute shell: ", cmd);
        }

        try {
            Process process = Runtime.getRuntime().exec(cmdArray.toArray(new String[cmdArray.size()]));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            process.waitFor();

            while ((line = reader.readLine()) != null) {
                result.getResult().add(line);
                WinkLog.i("Shell result: ", line);
            }

            while ((line = errorReader.readLine()) != null) {
                result.getErrorResult().add(line);
                WinkLog.i("Shell error: ", line);
            }

        } catch (Exception e) {
            WinkLog.i("Shell exception: ", e.getMessage());
            result.setE(e);
        }

        return result;
    }

    /**
     * 获取单个文件的MD5值
     *
     * @param file  文件
     * @param radix 位 16 32 64
     * @return MD5
     */
    public static String getFileMD5s(File file, int radix) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(radix);
    }

    public static boolean isStableFileExist(Project project) {
        String path = project.getRootProject().getProjectDir().getAbsolutePath() + "/.idea/" + Settings.NAME + "/stableIds.txt";
        WinkLog.i("isStableFileExist === Settings.env.tmpPath : " + Settings.env.tmpPath);
        File f = new File(path);
        return f.exists();
    }

    public static String upperCaseFirst(String val) {
        char[] arr = val.toCharArray();
        arr[0] = Character.toUpperCase(arr[0]);
        return new String(arr);
    }
}