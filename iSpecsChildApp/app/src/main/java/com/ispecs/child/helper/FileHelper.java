package com.ispecs.child.helper;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {

    public static void saveTextFile(Context context, String fileName, String content) {
        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendToFile(Context context, String fileName, String content) {
        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_APPEND);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean doesFileExist(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        return file.exists();
    }

    public static String readTextFile(Context context, String fileName) {
        FileInputStream inputStream;
        StringBuilder content = new StringBuilder();

        try {
            inputStream = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }

            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content.toString();
    }

    public static void downloadFile(Context context, String fileName) {
        // Assuming 'fileName' is the name of the file you want to download

        // Get the directory of the file in the app's file directory
        File sourceLocation = new File(context.getFilesDir(), fileName);

        // Create a destination location in external storage (public directory)
        File destinationLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        // Ensure the Downloads directory exists
        if (!destinationLocation.getParentFile().exists()) {
            destinationLocation.getParentFile().mkdirs();
        }

        // Using FileChannel to copy the file
        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceLocation).getChannel();
            destination = new FileOutputStream(destinationLocation).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the channels
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> getAllTextFileNames(Context context) {
        List<String> fileNames = new ArrayList<>();

        File[] files = context.getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    fileNames.add(file.getName());
                }
            }
        }

        return fileNames;
    }
}
