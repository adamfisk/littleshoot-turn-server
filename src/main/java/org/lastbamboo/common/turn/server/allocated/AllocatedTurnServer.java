package org.lastbamboo.common.turn.server.allocated;

/**
 * Interface for a TURN server created in response to a TURN Allocate Request
 * from a client.
 */
public interface AllocatedTurnServer
    {

    /**
     * Starts the server.
     */
    void start();

    /**
     * Stops the server.
     */
    void stop();
    }
