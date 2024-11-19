package main;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The ConfigLoader class is responsible for loading and parsing a JSON configuration
 * file. It reads the file from the provided file path, parses it into JSON objects,
 * and returns a list of JsonObject representing the configuration data.
 */
public class ConfigLoader {

    /**
     * Loads the configuration from a JSON file located at the specified file path.
     * It expects the JSON file to contain an array of objects, where each object
     * represents a configuration item.
     *
     * @param filePath The path to the configuration file to be loaded.
     * @return A list of JsonObject representing the configuration data.
     */
    public static List<JsonObject> loadConfig(String filePath) {
        // Create a list to hold the parsed JsonObject configurations
        List<JsonObject> configList = new ArrayList<>();

        // Attempt to read and parse the file
        try (FileReader reader = new FileReader(filePath)) {
            // Parse the JSON content of the file into a JsonArray
            JsonArray membersArray = JsonParser.parseReader(reader).getAsJsonArray();

            // Iterate through each element of the array and add it to the configList
            for (var element : membersArray) {
                configList.add(element.getAsJsonObject());
            }
        } catch (IOException e) {
            // Catch any IO exceptions (e.g., file not found, file access issues)
            System.out.println("Failed to load config: " + e.getMessage());
        } catch (Exception e) {
            // Catch any other exceptions (e.g., JSON parsing errors)
            System.out.println("Error parsing config: " + e.getMessage());
        }

        // Return the list of parsed configuration objects
        return configList;
    }
}
