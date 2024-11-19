package main;

import java.io.Serializable;

/**
 * Represents a message that can be sent between members of the election process.
 * This class implements Serializable to allow for object transmission over a network.
 */
public class Message implements Serializable {

    // The type of the message (e.g., PROPOSAL, ACCEPT, etc.)
    private final MessageType type;

    // The value associated with the message (could represent a proposal value, etc.)
    private final String value;

    // The proposal number associated with this message (used in election protocol)
    private final int proposalNumber;

    // The sender's unique identifier
    private final String id;

    /**
     * Constructs a new Message with the specified parameters.
     *
     * @param type The type of the message (e.g., PROPOSER, ACCEPTOR).
     * @param value The value associated with the message (could be a proposal or other data).
     * @param proposalNumber The proposal number for the election process.
     * @param id The unique identifier of the sender.
     */
    public Message(MessageType type, String value, int proposalNumber, String id) {
        this.type = type;
        this.value = value;
        this.proposalNumber = proposalNumber;
        this.id = id;
    }

    /**
     * Gets the type of the message.
     *
     * @return The type of the message.
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Gets the value associated with the message.
     *
     * @return The value of the message.
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the proposal number associated with the message.
     *
     * @return The proposal number.
     */
    public int getProposalNumber() {
        return proposalNumber;
    }

    /**
     * Gets the sender's unique identifier.
     *
     * @return The sender's id.
     */
    public String getSenderId() {
        return id;
    }
}
