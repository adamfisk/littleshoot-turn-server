package org.lastbamboo.common.turn.server.allocated;

import java.net.InetSocketAddress;

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
    
    /**
     * Accessor for the address the server is listening on.
     * 
     * @return The address the server is listening on.
     */
    InetSocketAddress getSocketAddress();
    }
