package org.lastbamboo.common.turn.server;

import java.io.IOException;

/**
 * Interface for creating a new TURN server.
 */
public interface TurnServer
    {

    /**
     * Starts the server.
     * @throws IOException If we cannot start the server for any reason. 
     */
    void start() throws IOException;
    
    /**
     * Stops the server.
     */
    void stop();
    }
