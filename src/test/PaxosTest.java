package test;

import org.junit.Test;
import java.io.*;

import static org.junit.Assert.*;

public class PaxosTest {

    private final String CONFIG_DIR = "src/main/resources/";

    // Helper method to run the main program
    private String runPaxos(String configFile) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java", "-cp", "bin:lib/gson-2.8.9.jar", "main.Main", configFile
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Capture the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    @Test
    public void testScenario1() throws IOException {
        String output = runPaxos(CONFIG_DIR + "test1.json");
        System.out.println(output);

        // Verify consensus was achieved
        assertTrue(output.contains("Consensus Achieved"));
    }

    @Test
    public void testScenario2() throws IOException {
        String output = runPaxos(CONFIG_DIR + "test2.json");
        System.out.println(output);

        assertTrue(output.contains("Consensus Achieved"));
    }

    @Test
    public void testScenario3() throws IOException {
        String output = runPaxos(CONFIG_DIR + "test3.json");
        System.out.println(output);

        assertTrue(output.contains("Consensus Achieved"));
    }

}
