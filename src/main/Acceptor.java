package main;

import java.util.concurrent.CountDownLatch;

/**
 * The Acceptor class represents a member of the council responsible for
 * handling the Paxos algorithm's acceptor role. It processes PREPARE and
 * ACCEPT_REQUEST messages and manages the proposal acceptance state.
 */
public class Acceptor extends CouncilMember {

    private int promisedProposalNumber = -1; // Highest proposal number promised not to reject
    private int acceptedProposalNumber = -1; // Last accepted proposal number
    private String acceptedValue = null;    // Last accepted value
    private String electionWinner = null;    // ID of the winner in the election

    /**
     * Constructor for the Acceptor class, initializing the acceptor with required details.
     *
     * @param id The unique identifier for the Acceptor.
     * @param responseDelay Delay (in milliseconds) to simulate response time.
     * @param port The network port the Acceptor will listen on.
     * @param electionLatch The latch to synchronize the election process.
     */
    public Acceptor(String id, int responseDelay, int port, CountDownLatch electionLatch) {
        super(id, "ACCEPTOR", responseDelay, port, electionLatch);
    }

    /**
     * Starts the Acceptor by initializing the server to listen for incoming messages
     * and processing the messages in a separate thread.
     */
    @Override
    public void start() {
        // Start the server to listen for incoming messages
        startServer();

        // Start a new thread to process messages from the message queue
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Take a message from the queue and process it
                    Message message = messageQueue.take();
                    processMessage(message);
                } catch (InterruptedException e) {
                    // Handle thread interruption
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Processes incoming messages based on their type (PREPARE, ACCEPT_REQUEST, LEARN).
     * Each message is handled by a corresponding method.
     *
     * @param message The incoming message to process.
     */
    @Override
    protected void processMessage(Message message) {
        try {
            // Simulate response delay for the Acceptor
            Thread.sleep(responseDelay);
        } catch (InterruptedException e) {
            // Handle interruption while sleeping
            Thread.currentThread().interrupt();
        }

        // Handle different message types by delegating to respective handlers
        switch (message.getType()) {
            case PREPARE:
                handlePrepare(message);
                break;
            case ACCEPT_REQUEST:
                handleAcceptRequest(message);
                break;
            case LEARN:
                handleLearn(message);
                break;
            default:
                break;
        }
    }

    /**
     * Handles the PREPARE message by comparing the proposal number with the
     * current promisedProposalNumber and sending a PROMISE or REJECT response.
     *
     * @param message The PREPARE message to handle.
     */
    private void handlePrepare(Message message) {
        int proposalNumber = message.getProposalNumber();

        if (proposalNumber > promisedProposalNumber) {
            // Update the promised proposal number to the new higher proposal number
            promisedProposalNumber = proposalNumber;

            // Create and send a PROMISE response, including the last accepted value and proposal number (if any)
            Message promiseMessage = new Message(
                    MessageType.PROMISE,
                    acceptedValue,
                    acceptedProposalNumber,
                    id
            );
            sendMessage(promiseMessage, findPeerById(message.getSenderId()));
        } else {
            // Send a REJECT response if the proposal number is lower than promised
            Message rejectMessage = new Message(MessageType.REJECT, null, promisedProposalNumber, id);
            sendMessage(rejectMessage, findPeerById(message.getSenderId()));
        }
    }

    /**
     * Handles the ACCEPT_REQUEST message by checking if the proposal number is
     * greater than or equal to the promisedProposalNumber, and if so, updates the
     * accepted proposal number and value. It then sends an ACCEPTED response.
     *
     * @param message The ACCEPT_REQUEST message to handle.
     */
    private void handleAcceptRequest(Message message) {
        int proposalNumber = message.getProposalNumber();

        if (proposalNumber >= promisedProposalNumber) {
            // Update the accepted proposal number and value to the new proposal and value
            acceptedProposalNumber = proposalNumber;
            acceptedValue = message.getValue();

            // Send an ACCEPTED response with the updated value and proposal number
            Message acceptedMessage = new Message(
                    MessageType.ACCEPTED,
                    acceptedValue,
                    acceptedProposalNumber,
                    id
            );
            sendMessage(acceptedMessage, findPeerById(message.getSenderId()));
        }
    }

    /**
     * Handles the LEARN message, which signifies that the election process has
     * completed, and records the ID of the election winner. The Acceptor then
     * shuts down.
     *
     * @param message The LEARN message to handle.
     */
    private void handleLearn(Message message) {
        electionWinner = message.getSenderId();
        shutdown(); // Shut down the server after learning the election result
    }
}
