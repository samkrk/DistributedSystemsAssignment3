package main;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    // Method to log messages
    public static void log(String level, String component, String message) {
        // Get the current timestamp
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        // Format the log message
        String logMessage = String.format("[%s] [%s] [%s] %s", timestamp, level, component, message);
        // Print the log message
        System.out.println(logMessage);
    }

    // Methods for specific log levels
    public static void info(String component, String message) {
        log("INFO", component, message);
    }

    public static void error(String component, String message) {
        log("ERROR", component, message);
    }
}
