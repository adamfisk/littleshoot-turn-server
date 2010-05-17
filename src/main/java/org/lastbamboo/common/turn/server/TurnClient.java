package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;

/**
 * Interface for classes that keep track of data for TURN clients that this
 * server has allocated addresses for.  Keeps track of permissions to external
 * hosts, the IP address of the client, and the means of contacting the client.
 */
public interface TurnClient
    {

    /**
     * Writes data from this TURN client to the specified remote address.  If
     * there's already a connection to the remote address, this uses that.
     * Otherwise, it opens a connection and begins allowing incoming data from
     * that connection.
     *
     * @param remoteAddress The IP address and port of the remote host to send
     * data to.
     * @param data The data to send the remote host.
     * @return <code>true</code> if the data was send to an existing binding.
     * Otherwise, this returns <code>false</code> to indicate that there was
     * no existing matching 5-tuple for the remote host and that we're
     * attempting to connect to that remote host.
     */
    boolean write(InetSocketAddress remoteAddress, ByteBuffer data);
    
    /**
     * Tells the client to appropriately handle a connect request to the 
     * specified remote host.
     * 
     * @param socketAddress The address to handle.
     */
    void handleConnect(InetSocketAddress socketAddress);

    /**
     * Accessor for the IP and port this server has allocated on behalf of the
     * TURN client.  This is the endpoint other hosts can contact this client on.
     *
     * @return The IP address and port this server has allocated for the client.
     */
    InetSocketAddress getRelayAddress();

    /**
     * Closes the client and all associated connections.
     */
    void close();

    /**
     * Accessor for the handler for reading and writing data with this client.
     * @return The handler for reading and writing data with this client.
     */
    IoSession getIoSession();

    /**
     * Returns whether or not the TURN client has set its "active destination"
     * turning off TURN messaging on that connection.
     * @return <code>true</code> if the client has set the active destination,
     * otherwise <code>false</code>.
     */
    boolean hasActiveDestination();
    
    /**
     * Returns whether or not the remote host who created the specified 
     * session has permission to open a connection to this TURN client.  The
     * remote host has permission if the TURN client has issued a connect
     * request to that host that is still active.
     * 
     * @param session The connection from the remote host.
     * @return <code>true</code> if the remote host has permission to connect
     * to the TURN client, otherwise <code>false</code>.
     */
    boolean hasIncomingPermission(IoSession session);

    /**
     * Adds the specified connection for this TURN client.  This is typically
     * called when a remote host makes a connection to the TURN server.
     *
     * @param session The class for reading and writing data with the
     * remote host.
     */
    void addConnection(IoSession session);

    /**
     * Removes the connection.
     * 
     * @param session The class for reading and writing data with the
     * remote host.
     */
    void removeConnection(IoSession session);
    
    /**
     * Returns the number of connections for this TURN client.
     *
     * @return The number of connections for this TURN client.
     */
    int getNumConnections();

    /**
     * Starts the separate server running on the client's allocated address
     * for accepting connections from remote hosts.
     */
    void startServer();
    
    /**
     * Accessor for the MAPPED ADDRESS, otherwise known as the server
     * reflexive address.
     * 
     * @return The MAPPED ADDRESS.
     */
    InetSocketAddress getMappedAddress();

    }
