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

package com.immomo.litebuild.util;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    /**
     * 运行shell并获得结果，注意：如果sh中含有awk,一定要按new String[]{"/bin/sh","-c",shStr}写,才可以获得流
     *
     * @param shStr
     *            需要执行的shell
     * @return
     */
    public static List<String> runShell(String shStr) {
        List<String> strList = new ArrayList<String>();
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh","-c", "`" + shStr + "`"},null,null);
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line;
            process.waitFor();
            while ((line = input.readLine()) != null){
                strList.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String str : strList) {
            System.out.print(str);
        }

        return strList;
    }

//    /**
//     * 运行shell并获得结果，注意：如果sh中含有awk,一定要按new String[]{"/bin/sh","-c",shStr}写,才可以获得流
//     *
//     * @param
//     * @return
//     */
//    public static List<String> runShell(String... cmds) {
//        List<String> strList = new ArrayList<>();
//        List<String> cmdArray = new ArrayList<>();
//
//        cmdArray.add("/bin/sh");
//        cmdArray.add("-c");
//        for (String cmd: cmdArray) {
//            cmdArray.add("`" + cmd + "`");
//        }
//
//        try {
//            Process process = Runtime.getRuntime().exec((String[]) cmdArray.toArray());
//            InputStreamReader ir = new InputStreamReader(process.getInputStream());
//            LineNumberReader input = new LineNumberReader(ir);
//            String line;
//            process.waitFor();
//            while ((line = input.readLine()) != null){
//                strList.add(line);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return strList;
//    }


}
