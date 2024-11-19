package main;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Represents a proposer in the election process. A proposer is responsible for initiating
 * proposals, receiving promises from acceptors, and eventually achieving consensus.
 * The proposer communicates with other council members (acceptors, learners) to reach a decision.
 */
public class Proposer extends CouncilMember {

    // Proposal number, initialized to 0 and incremented for each new proposal
    private int proposalNumber = 0;

    // Count of rejections received for the current proposal
    private int numRejections = 0;

    // Set of acceptors who have promised to consider this proposal
    private final Set<String> promisedSet;

    // Set of acceptors who have accepted this proposal
    private final Set<String> acceptedSet;

    // Flag to indicate whether the proposer has received the required promises
    private boolean receivedPromises;

    // The winner of the election (if consensus is achieved)
    private String electionWinner;

    // Flag to check if the proposer is responsive based on responseDelay
    private boolean responsive = true;

    /**
     * Constructs a Proposer object with the given parameters.
     *
     * @param id Unique identifier for the proposer.
     * @param responseDelay Delay before responding to a message, simulating network latency.
     * @param port The network port the proposer listens on.
     * @param electionLatch The latch used to synchronize the completion of the election.
     */
    public Proposer(String id, int responseDelay, int port, CountDownLatch electionLatch) {
        super(id, "PROPOSER", responseDelay, port, electionLatch);
        this.promisedSet = new HashSet<>();
        this.acceptedSet = new HashSet<>();
        this.receivedPromises = false;

        // If response delay is set to a special value, make the proposer unresponsive
        if (responseDelay == 12345) {
            responsive = false;
        }
    }

    /**
     * Generates a new proposal number, ensuring that the number is unique.
     *
     * @return The new proposal number.
     */
    private int generateProposalNumber() {
        proposalNumber++;
        return proposalNumber;
    }

    /**
     * Initiates a new proposal by sending a "PREPARE" message to all peers.
     * This starts the process of proposing a value to the acceptors.
     */
    public void initiateProposal() {
        Logger.info(id, "*** Initiating proposal for " + id + ". Sending Broadcast ***");
        int proposalNum = generateProposalNumber();

        // Create a "PREPARE" message with the current proposal number
        Message prepareMessage = new Message(MessageType.PREPARE, id, proposalNum, id);

        // Clear previous promise and accepted sets for a new proposal
        promisedSet.clear();
        acceptedSet.clear();
        receivedPromises = false;
        numRejections = 0;

        // Send the prepare message to each peer
        for (CouncilMember peer : peers) {
            sendMessage(prepareMessage, peer);
        }
    }

    @Override
    public void start() {
        // Start the server to listen for incoming messages
        startServer();

        // Start a thread to process incoming messages from the message queue
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Take a message from the queue and process it
                    Message message = messageQueue.take();
                    processMessage(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Wait a short period before starting the first proposal
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Initiate the first proposal
        initiateProposal();
    }

    @Override
    protected void processMessage(Message message) {
        if (!responsive) {
            return;
        }

        // Simulate network delay by sleeping for the specified response delay
        try {
            Thread.sleep(responseDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Process different types of messages based on their type
        switch (message.getType()) {
            case PROMISE:
                handlePromise(message);
                break;
            case REJECT:
                handleReject(message);
                break;
            case ACCEPTED:
                handleAccepted(message);
                break;
            case LEARN:
                handleLearn(message);
                break;
        }
    }

    /**
     * Handles receiving a "PROMISE" message from an acceptor. The proposer records
     * the promise and checks if a majority of promises have been received.
     * If so, it sends "ACCEPT_REQUEST" messages to the acceptors.
     *
     * @param message The promise message received from an acceptor.
     */
    private void handlePromise(Message message) {
        promisedSet.add(message.getSenderId());

        // Only proceed if the majority of promises have been received
        if (receivedPromises) {
            return;
        }

        if (promisedSet.size() >= peers.size() / 2) {
            receivedPromises = true;
            Logger.info(id, "*** Majority of Promises Received. Sending Accept Requests ***");

            // Send "ACCEPT_REQUEST" to all peers after receiving a majority of promises
            Message acceptRequest = new Message(MessageType.ACCEPT_REQUEST, id, proposalNumber, id);
            for (CouncilMember peer : peers) {
                sendMessage(acceptRequest, peer);
            }
        }
    }

    /**
     * Handles receiving a "REJECT" message. The proposer increments the proposal number
     * if the rejection is due to a higher proposal and retries if necessary after backoff.
     *
     * @param message The reject message received from an acceptor.
     */
    private void handleReject(Message message) {
        numRejections++;

        // Ensure the proposal number is higher than the rejected one
        proposalNumber = Math.max(proposalNumber, message.getProposalNumber() + 1);

        // If rejected by a majority, retry the proposal with a higher number
        if (numRejections >= peers.size() / 2) {
            numRejections = 0;
            try {
                int backoff = 1000; // Backoff time for retrying the proposal
                Logger.info(id, "Backoff for " + backoff + "ms");
                Thread.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            initiateProposal();
        }
    }

    /**
     * Handles receiving an "ACCEPTED" message. The proposer checks if a majority of acceptors
     * have accepted the proposal. If consensus is reached, it broadcasts a "LEARN" message.
     *
     * @param message The accepted message received from an acceptor.
     */
    private void handleAccepted(Message message) {
        acceptedSet.add(message.getSenderId());

        // If a majority of acceptors have accepted, consensus is achieved
        if (acceptedSet.size() >= peers.size() / 2) {
            acceptedSet.clear();
            Logger.info(id, "*** Consensus Achieved. " + id + " has been elected. *** ");

            // Notify all peers that consensus is reached
            for (CouncilMember peer : peers) {
                Message learnMessage = new Message(MessageType.LEARN, id, proposalNumber, id);
                broadcast(learnMessage, peer);
            }
            shutdown();
        }
    }

    /**
     * Handles receiving a "LEARN" message, indicating the election winner.
     * This marks the end of the election process and initiates the shutdown.
     *
     * @param message The learn message received from an acceptor.
     */
    private void handleLearn(Message message) {
        electionWinner = message.getSenderId();
        shutdown();
    }
}
