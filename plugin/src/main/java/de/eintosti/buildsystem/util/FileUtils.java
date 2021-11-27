/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.util;

import com.google.common.collect.Sets;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Set;

/**
 * @author einTosti
 */
public class FileUtils {

    public static void copy(File source, File target) {
        try {
            Set<String> ignore = Sets.newHashSet("uid.dat", "session.lock");
            if (ignore.contains(source.getName())) {
                return;
            }

            if (source.isDirectory()) {
                if (!target.exists() && !target.mkdirs()) {
                    throw new IOException("Couldn't create world directory!");
                }

                for (String fileName : source.list()) {
                    if (ignore.contains(fileName)) {
                        continue;
                    }

                    File srcFile = new File(source, fileName);
                    File trgFile = new File(target, fileName);
                    copy(srcFile, trgFile);
                }
            } else {
                InputStream inputStream = new FileInputStream(source);
                OutputStream outputStream = new FileOutputStream(target);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteDirectory(File folder) {
        File[] allContents = folder.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return folder.delete();
    }

    public static long getDirectoryCreation(File file) {
        long creation = System.currentTimeMillis();
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            FileTime time = attrs.creationTime();
            creation = time.toMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return creation;
    }
}
