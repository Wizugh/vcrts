package db;

import java.util.*; 
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.nio.file.*;

public class FileManager {
    private static final Logger logger = Logger.getLogger(FileManager.class.getName());
    private static final String data_dir = "data";
    
    static {
        try {
            Files.createDirectories(Paths.get(data_dir));
        }
        catch (IOException e){
            logger.log(Level.SEVERE, "Failed to create data directory", e);
        }
    }
    
    public static List<String> readAllLines(String fileName){
        Path filePath = Paths.get(data_dir, fileName);
        try {
            if (!Files.exists(filePath)){
                Files.createFile(filePath);
                return new ArrayList<>();
            }
            return Files.readAllLines(filePath);
        } catch (IOException e){
            logger.log(Level.SEVERE, "Error reading file: " + fileName, e);
            return new ArrayList<>();
        }
    }
    
    public static boolean writeAllLines(String fileName, List<String> lines){
        Path filePath = Paths.get(data_dir, fileName);
        try {
            Files.write(filePath, lines);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error writing to file: " + fileName, e);
            return false;
        }
    }
    
    public static boolean appendLine(String fileName, String line) {
        Path filePath = Paths.get(data_dir, fileName);
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            Files.write(filePath, (line + System.lineSeparator()).getBytes(),
                    StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error appending to file: " + fileName, e);
            return false;
        }
    }
    
    public static String generateUniqueId(String fileName, String idPrefix) {
        return idPrefix + System.currentTimeMillis();
    }
    
    public static int generateUniqueNumericId(String fileName) {
        List<String> lines = readAllLines(fileName);
        int maxId = 0;
        for (String line : lines) {
            try {
                String[] parts = line.split("\\|");
                if (parts.length > 0) {
                    int id = Integer.parseInt(parts[0]);
                    if (id > maxId) {
                        maxId = id;
                    }
                }
            } catch (NumberFormatException e) {
            }
        }
        return maxId + 1;
    }
}