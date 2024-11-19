package main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.CountDownLatch;

/**
 * The CouncilMember class represents a member of a council (such as a Proposer or Acceptor)
 * in a distributed system. Each member can send and receive messages to/from other members,
 * manage incoming connections, and process messages based on their role in the system.
 * This class implements Runnable for concurrent execution.
 */
public abstract class CouncilMember implements Runnable {
    protected final String id;           // Unique identifier for the council member
    protected final String role;         // Role of the council member (e.g., Proposer, Acceptor)
    protected final int responseDelay;   // Delay in response (for simulation purposes)
    protected final int port;            // Port for network communication
    protected List<CouncilMember> peers = new ArrayList<>();  // List of other council members (peers)
    protected ExecutorService executor;  // Executor service to handle threads
    protected BlockingQueue<Message> messageQueue;  // Queue to hold incoming messages
    private ServerSocket serverSocket;  // Server socket for receiving incoming connections
    private final CountDownLatch latch; // CountDownLatch to coordinate shutdown
    private static boolean completed = false;  // Flag indicating whether the process is completed

    /**
     * Constructor for initializing a council member.
     *
     * @param id Unique identifier for the council member
     * @param role The role of the council member (e.g., "ACCEPTOR", "PROPOSER")
     * @param responseDelay Time delay for simulating response latency
     * @param port Port number for network communication
     * @param latch CountDownLatch to signal shutdown completion
     */
    public CouncilMember(String id, String role, int responseDelay, int port, CountDownLatch latch) {
        this.id = id;
        this.role = role;
        this.responseDelay = responseDelay;
        this.port = port;
        this.executor = Executors.newCachedThreadPool();  // Create a thread pool for managing tasks
        this.messageQueue = new LinkedBlockingQueue<>();  // Create a blocking queue for messages
        this.latch = latch;  // Initialize the latch for shutdown coordination
    }

    /**
     * Adds a list of peers (other council members) to this council member's peer list.
     *
     * @param peers A list of other council members to be added as peers
     */
    public void addPeers(List<CouncilMember> peers) {
        this.peers.addAll(peers);
    }

    /**
     * Sends a message to a specific peer.
     * Proposers only send messages to acceptors. If the process is completed, no messages are sent.
     *
     * @param message The message to be sent
     * @param peer The target peer (council member) to send the message to
     */
    public void sendMessage(Message message, CouncilMember peer) {
        if (this instanceof Proposer && !(peer instanceof Acceptor)) {
            return; // Proposers only send to acceptors
        }
        if (completed) {
            return; // If process is completed, do not send any more messages
        }

        try (Socket socket = new Socket(peer.getAddress(), peer.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            // Write the message to the peer's socket
            out.writeObject(message);
            // Log the message type sent, if relevant
            if (message.getType().equals(MessageType.ACCEPTED) ||
                    message.getType().equals(MessageType.PROMISE)) {
                Logger.info(id, "Sent " + message.getType() + " to " + peer.getId());
            }
        } catch (IOException e) {
            Logger.error(id, "Failed to send message to " + peer.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all peers except the sender.
     * The message is sent to each peer in the network.
     *
     * @param message The message to be broadcasted
     * @param peer The peer sending the message (used for avoiding self-sending)
     */
    public void broadcast(Message message, CouncilMember peer) {
        if (message.getSenderId().equals(peer.getId())) {
            return; // Prevent broadcasting to the sender itself
        }
        try (Socket socket = new Socket(peer.getAddress(), peer.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            // Write the message to the peer's socket
            out.writeObject(message);
            // Log the message type sent, if relevant
            if (message.getType().equals(MessageType.ACCEPTED) ||
                    message.getType().equals(MessageType.PROMISE)) {
                Logger.info(id, "Sent " + message.getType() + " to " + peer.getId());
            }
        } catch (IOException e) {
            Logger.error(id, "Failed to send message to " + peer.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Starts the server to listen for incoming connections on the specified port.
     * This method runs in a separate thread to allow for non-blocking behavior.
     */
    protected void startServer() {
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);  // Create a server socket on the specified port
                Logger.info(role + " " + id, "Listening on port " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Accept incoming connections from peers
                        Socket clientSocket = serverSocket.accept();
                        executor.execute(() -> handleIncomingConnection(clientSocket));  // Handle the incoming connection
                    } catch (SocketException e) {
                        // Server socket was closed, shutting down gracefully
                        break;
                    }
                }
            } catch (IOException e) {
                Logger.error(role + " " + id, "Failed to listen on port " + port + ": " + e.getMessage());
            }
        });
    }

    /**
     * Handles an incoming connection from a peer.
     * The method reads the incoming message and puts it into the message queue for processing.
     *
     * @param clientSocket The socket representing the incoming connection
     */
    private void handleIncomingConnection(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            // Read the message from the peer
            Message message = (Message) in.readObject();
            messageQueue.put(message);  // Place the message into the queue for processing
        } catch (InterruptedException e) {
            Logger.info(id, "Interrupted while processing incoming connection");
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } catch (IOException | ClassNotFoundException e) {
            Logger.error(id, "Failed to process incoming message: " + e.getMessage());
        }
    }

    /**
     * Shuts down the council member's processes, including closing the server socket
     * and stopping the executor service.
     */
    protected void shutdown() {
        completed = true;  // Mark the process as completed
        executor.shutdownNow();  // Immediately shut down the executor
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();  // Close the server socket
            }
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                Logger.error(id, "Executor service did not terminate in time");
            }
        } catch (IOException e) {
            // Logger.error(id, "Failed to close server socket: " + e.getMessage());
        } catch (InterruptedException e) {
            // Logger.error(id, "Interrupted during executor shutdown");
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();  // Decrement the latch to signal shutdown completion
        }
    }

    /**
     * Abstract method for processing incoming messages.
     * Each subclass must implement this method to handle the messages it receives.
     *
     * @param message The message to be processed
     */
    protected abstract void processMessage(Message message);

    /**
     * Starts the council member's process. This method is implemented by subclasses.
     */
    public abstract void start();

    @Override
    public void run() {
        start();  // Run the start method to begin processing
    }

    /**
     * Finds a peer by their ID.
     *
     * @param peerId The ID of the peer to find
     * @return The council member with the specified ID, or null if not found
     */
    public CouncilMember findPeerById(String peerId) {
        return peers.stream()
                .filter(peer -> peer.getId().equals(peerId))
                .findFirst()
                .orElse(null);  // Return the first matching peer or null if not found
    }

    public String getId() { return id; }
    public String getAddress() { return "localhost"; }
    public int getPort() { return port; }
}
