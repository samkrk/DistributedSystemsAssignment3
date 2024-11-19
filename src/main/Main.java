package main;

import com.google.gson.JsonObject;
import java.util.List;

public class Main {

    /**
     * Main entry point for the ElectionServer application.
     *
     * @param args Command-line arguments, expecting a configuration file path as the first argument
     */
    public static void main(String[] args) {

        // Check if a config file path was provided as a command-line argument
        if (args.length == 0) {
            System.out.println("No config file path provided.");
            return; // Exit if no config file path is given
        }

        // Extract the config file path from the command-line arguments
        String configFile = args[0];
        System.out.println("Config file path: " + configFile);

        // Load configurations from the JSON file specified by the config file path
        List<JsonObject> memberConfigs = ConfigLoader.loadConfig(configFile);

        // Check if the config file was loaded successfully and is not empty
        if (memberConfigs.isEmpty()) {
            System.out.println("Failed to load config or config is empty.");
            return; // Exit if the configuration is invalid or empty
        } else {
            // If config is successfully loaded, print the details of each member configuration
            System.out.println("\n *** CONFIG LOADED SUCCESSFULLY: *** ");
            for (JsonObject member : memberConfigs) {
                System.out.println(member); // Print each member's configuration
            }
            System.out.println(); // Add an empty line for readability
        }

        // Start the ElectionServer with the loaded member configurations
        ElectionServer server = new ElectionServer(memberConfigs);
        server.start(); // Start the election process on the server
    }
}
