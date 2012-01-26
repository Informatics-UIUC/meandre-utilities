/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */

package org.meandre.tools.components;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.meandre.annotations.CreateComponentDescriptor;
import org.seasr.meandre.support.generic.io.FileUtils;

/**
 *
 * @author Boris Capitanu
 *
 * Dependencies required:
 *
 * meandre-annotation-1.4.10.jar
 * meandre-kernel-1.4.10.jar
 * seasr-commons.jar
 * jena-2.6.3.jar
 * slf4j-api-1.5.8.jar
 * slf4j-log4j12-1.5.8.jar
 * log4j-1.2.13.jar
 * xercesImpl-2.7.1.jar
 * iri-0.8.jar
 * icu4j-3.4.4.jar
 *
 */

public class CreateDescriptors {

    private static final FileFilter CLASS_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".class") || pathname.getName().endsWith(".CLASS");
        }
    };

    private static final FileFilter JAR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".jar") || pathname.getName().endsWith(".JAR");
        }
    };

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println(String.format("Usage: %s <dotclass_folder> <lib_folder> <descriptors_folder>", CreateDescriptors.class.getSimpleName()));
            System.exit(-1);
        }

        File classesDir = new File(args[0]);
        if (!classesDir.isDirectory())
            throw new IllegalArgumentException(classesDir.toString());

        File libDir = new File(args[1]);
        if (!libDir.isDirectory())
            throw new IllegalArgumentException(libDir.toString());

        File descriptorDir = new File(args[2]);
        if (!descriptorDir.isDirectory()) {
            // attempt to create it
            if (!descriptorDir.mkdirs())
                throw new IllegalArgumentException(descriptorDir.toString());
        }

        List<File> classFiles = new ArrayList<File>();
        FileUtils.findFiles(classesDir, CLASS_FILTER, true, classFiles);

        List<File> jarFiles = new ArrayList<File>();
        FileUtils.findFiles(libDir, JAR_FILTER, true, jarFiles);

        URL[] classPathUrls = new URL[jarFiles.size() + 1];
        classPathUrls[0] = classesDir.toURI().toURL();
        for (int i = 1, iMax = classPathUrls.length; i < iMax; i++)
            classPathUrls[i] = jarFiles.get(i-1).toURI().toURL();

        URLClassLoader classLoader = new URLClassLoader(classPathUrls);
        CreateComponentDescriptor.setClassLoader(classLoader);

        CreateComponentDescriptor.setMakeSubs(true);

        System.out.println(String.format("Processing %d files...", classFiles.size()));

        int n = 0;
        for (int i = 0, iMax = classFiles.size(); i < iMax; i++) {
            File f = classFiles.get(i);

            try {
                CreateComponentDescriptor ccd = new CreateComponentDescriptor();
                ccd.setComponentClassName(getClassName(classesDir, f));
                ccd.setComponentDescriptorFolderName(descriptorDir.getAbsolutePath());
                ccd.setComponentDescriptorFile(descriptorDir);
                ccd.processComponentDescriptor();
                n++;
            }
            catch (Exception e) {
            }

//            float percentComplete = ((i+1) / (float)iMax) * 100f;
//            System.out.print(String.format("\rProcessing files... [%d/%d], %d components found, %3.0f%% complete", (i+1), iMax, n, percentComplete));
//            System.out.flush();
        }

        System.out.println(String.format("%d components processed successfully, descriptors created at %s", n, descriptorDir));
    }

    private static String getClassName(File srcDir, File f) throws IOException {
        String sSrcDir = srcDir.getCanonicalPath();
        String sFile = f.getCanonicalPath();

        if (!sFile.startsWith(sSrcDir))
            throw new RuntimeException("srcDir: " + sSrcDir + "  sFile: " + sFile);

        sFile = sFile.substring(sSrcDir.length(), sFile.length() - 6);
        if (sFile.startsWith(File.separator)) sFile = sFile.substring(1);

        sFile = sFile.replaceAll(File.separator, ".");

        return sFile;
    }

}
