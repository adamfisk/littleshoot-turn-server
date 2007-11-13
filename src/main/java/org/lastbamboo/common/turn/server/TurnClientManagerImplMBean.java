package org.lastbamboo.common.turn.server;

/**
 * MBean for accessing data about the {@link TurnClientManagerImpl}.
 */
public interface TurnClientManagerImplMBean
    {

    /**
     * Accessor for the number of clients currently connected to this 
     * TURN server.
     * 
     * @return The number of clients currently connected to this TURN server.
     */
    int getNumClients();
    }
