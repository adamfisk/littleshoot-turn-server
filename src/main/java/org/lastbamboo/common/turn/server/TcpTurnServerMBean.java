package org.lastbamboo.common.turn.server;

/**
 * MBean interface for the TURN server class.
 */
public interface TcpTurnServerMBean
    {
    
    /**
     * Accessor for the port the TURN server is running on.
     * 
     * @return The port the server is running on.
     */
    int getTurnPort();
    }
