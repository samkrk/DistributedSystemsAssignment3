package main;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ElectionServer {

    // List to store all the council members involved in the election
    private final List<CouncilMember> members;

    // CountDownLatch to synchronize the completion of all members' tasks
    private final CountDownLatch electionLatch;

    /**
     * Constructor to initialize the ElectionServer with the given member configurations.
     *
     * @param memberConfigs List of JSON objects containing configuration for each member
     */
    public ElectionServer(List<JsonObject> memberConfigs) {
        // Initialize the list of members and the election latch based on the number of members
        this.members = new ArrayList<>();
        this.electionLatch = new CountDownLatch(memberConfigs.size()); // One latch for each member

        // Initialize each council member based on the provided configurations
        for (JsonObject config : memberConfigs) {
            String role = config.get("role").getAsString().toUpperCase(); // Get role and ensure it's uppercase
            int responseDelay = config.get("responseDelay").getAsInt();  // Get response delay
            String id = config.get("id").getAsString();                   // Get member ID
            int port = config.get("port").getAsInt();                     // Get port number

            // Declare the council member to be instantiated
            CouncilMember member;

            // Instantiate member based on the role defined in the config
            switch (role) {
                case "PROPOSER":
                    member = new Proposer(id, responseDelay, port, electionLatch);
                    break;
                case "ACCEPTOR":
                    member = new Acceptor(id, responseDelay, port, electionLatch);
                    break;
                case "LEARNER":
                    // For now, we instantiate a member as an Acceptor since Learner isn't implemented
                    member = new Acceptor(id, responseDelay, port, electionLatch);
                    Logger.error(id, "Learner not yet implemented"); // Log that Learner role is not yet implemented
                    break;
                default:
                    // If the role is unknown, throw an exception
                    throw new IllegalArgumentException("Unknown role: " + role);
            }

            // Add the newly created member to the list of members
            members.add(member);
        }

        // After members are created, connect each member to all other members for peer-to-peer communication
        for (CouncilMember member : members) {
            member.addPeers(members);
        }
    }

    /**
     * Starts the election process by launching each member in its own thread and
     * waiting for all members to finish their tasks.
     */
    public void start() {
        // Start each council member's task in a separate thread
        for (CouncilMember member : members) {
            new Thread(member).start();
        }

        // Wait for all threads to signal that they are done
        try {
            electionLatch.await(); // Blocks until the latch count reaches zero (all members are done)
            Logger.info("MAIN", "*** ELECTION COMPLETE ***"); // Log when the election process is complete
        } catch (InterruptedException e) {
            // Handle any interruption during the await phase and log the error
            Logger.error("MAIN", "Election process interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Preserve the interrupt status
        }
    }
}
