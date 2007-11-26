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
    int getNumTurnClients();
    
    /**
     * Accessor for the maximum number of TURN clients seen.
     * 
     * @return The maximum number of TURN clients seen.
     */
    int getMaxNumTurnClients();
    
    /**
     * Accessor for the total number of remote clients accessing the server.
     * 
     * @return The total number of remote clients accessing the server.
     */
    int getNumRemoteTurnClients();
    
    /**
     * Accessor for the maximum number of remote clients at any one time.<p>  
     * 
     * Note that this number is not necessarily precisely accurate.  Rather 
     * it reflects the maximum number for any time we've taken a reading, 
     * which should be close to the <code>true</code> value.
     * 
     * @return The maximum number of remote clients at any one time.
     */
    int getMaxNumRemoteTurnClients();
    
    /**
     * Get the maximum number of remote host connections we've seen for any
     * single client.<p>  
     * 
     * Note that this number is not necessarily precisely accurate.  Rather 
     * it reflects the maximum number for any time we've taken a reading, 
     * which should be close to the <code>true</code> value.
     * 
     * @return The maximum number of remote host connections we've seen for
     * any single client. 
     */
    int getMaxNumRemoteSingleTurnClient();

    }
