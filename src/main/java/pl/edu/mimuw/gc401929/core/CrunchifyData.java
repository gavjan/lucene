package pl.edu.mimuw.gc401929.core;
import java.io.FileWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;


// CrunchifyData Class
    public class CrunchifyData {

        private static String crunchify_file_location = System.getProperty("user.home")
                + File.separatorChar+ ".index" + File.separatorChar + "crunchify.txt";
        public static Gson gson = new Gson();
        private ArrayList<String> catalog;
        //private ArrayList<IndexWriter> writers;
        public ArrayList<String> getCatalog() {
            return catalog;
        }

        public void setCatalog(ArrayList<String> catalog) {
            this.catalog = catalog;
        }

        public static void main(String[] args) {
            // Retrieve data from file
            CrunchifyData crunchify = crunchifyReadFromFile();
            if(crunchify==null) return;
        }

        // Save to file Utility
        public void WriteToFile() {
            try {
                Files.deleteIfExists(Paths.get(crunchify_file_location));
            } catch (IOException e) {
                e.printStackTrace();
            }
            crunchifyWriteToFile(gson.toJson(this));
        }
        private static void crunchifyWriteToFile(String myData) {
            File crunchifyFile = new File(crunchify_file_location);
            if (!crunchifyFile.exists()) {
                try {
                    File directory = new File(crunchifyFile.getParent());
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    crunchifyFile.createNewFile();
                } catch (IOException e) {
                    err("Excepton Occured: " + e.toString());
                }
            }

            try {
                // Convenience class for writing character files
                FileWriter crunchifyWriter;
                crunchifyWriter = new FileWriter(crunchifyFile.getAbsoluteFile(), true);

                // Writes text to a character-output stream
                BufferedWriter bufferWriter = new BufferedWriter(crunchifyWriter);
                bufferWriter.write(myData.toString());
                bufferWriter.close();

                log("data saved at file location: " + crunchify_file_location + " Data: " + myData + "\n");
            } catch (IOException e) {
                err("Got an error while saving data to file " + e.toString());
            }
        }

        // Read From File Utility
        public static CrunchifyData crunchifyReadFromFile() {
            File crunchifyFile = new File(crunchify_file_location);
            if (!crunchifyFile.exists())
                err("File doesn't exist");

            InputStreamReader isReader;
            try {
                isReader = new InputStreamReader(new FileInputStream(crunchifyFile), "UTF-8");

                JsonReader myReader = new JsonReader(isReader);
                CrunchifyData crunchify = gson.fromJson(myReader, CrunchifyData.class);
                return crunchify;

            } catch (Exception e) {
                err("error load cache from file " + e.toString());
            }

            return null;
        }

        private static void log(String string) {
            System.out.println("[CRUNCH] " + string);
        }
        private static void err(String string) {
            System.err.println("[ERROR] [CRUNCH] " + string);
        }
        public boolean addPath(String path) {
            if(catalog.contains(path)) {return false;}
            catalog.add(path);
            return true;
        }
        public boolean deletePath(String path) {
            if(!catalog.contains(path)) {return false;}
            catalog.remove(path);
            return true;
        }



}

    // Main Method