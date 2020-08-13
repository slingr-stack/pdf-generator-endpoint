package io.slingr.endpoints.pdfGenerator;


import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Date;

public class PdfFilesUtils {

    public void executeCommands() throws IOException, InterruptedException {

        File tempScript = createTempScript();

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } finally {
            tempScript.delete();
        }
    }

    public File createTempScript() throws IOException {

        File tempScript = File.createTempFile("script", null);

        Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));
        PrintWriter printWriter = new PrintWriter(streamWriter);

        printWriter.println("#!/bin/bash");
        printWriter.println("apt-get update -y && apt-get install -y xvfb libfontconfig libxrender1");

        printWriter.close();

        return tempScript;
    }

    public String exportResource(String resourceName) throws Exception {

        InputStream stream = null;
        OutputStream resStreamOut = null;

        String destFolder = "/usr/bin/";

        try {
            stream = PdfGenerator.class.getClassLoader().getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];

            resStreamOut = new FileOutputStream(destFolder + resourceName);

            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }


        } finally {
            if (stream != null) {
                stream.close();
            }
            if (resStreamOut != null) {
                resStreamOut.close();
            }
        }

        File f = new File(destFolder + resourceName);
        f.setExecutable(true);
        f.setReadable(true);
        f.setWritable(false);


        return destFolder + resourceName;

    }

    public static String getFileName(String prefix, Json settings) {
        String fileName = prefix + "-" + new Date().getTime();
        if (settings.contains("name") && StringUtils.isNotBlank(settings.string("name"))) {
            fileName = settings.string("name");
        }
        return fileName;
    }

}
