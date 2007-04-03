package org.lastbamboo.shoot.turn.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.lastbamboo.common.protocol.ReaderWriter;

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
     * @throws IOException If we could not enable writing on this channel.
     * @return <code>true</code> if the data was send to an existing binding.
     * Otherwise, this returns <code>false</code> to indicate that there was
     * no existing matching 5-tuple for the remote host and that we're
     * attempting to connect to that remote host.
     */
    boolean write(final InetSocketAddress remoteAddress, final ByteBuffer data)
        throws IOException;

    /**
     * Checks whether or not the specified address has permission to send
     * incoming data to this TURN client.  It has permission only if the client
     * has issued a SEND-REQUEST to the remote address.
     * @param address The remote address to check for permissions.
     * @return <code>true</code> if the remote address has permission to send
     * incoming data, otherwise <code>false</code>.
     */
    boolean hasIncomingPermission(final InetAddress address);

    /**
     * Accessor for the IP and port this server has allocated on behalf of the
     * TURN client.  This is the endpoint other hosts can contact this client on.
     *
     * @return The IP address and port this server has allocated for the client.
     */
    InetSocketAddress getAllocatedSocketAddress();

    /**
     * Closes the client and all associated connections.
     */
    void close();

    /**
     * Accessor for the handler for reading and writing data with this client.
     * @return The handler for reading and writing data with this client.
     */
    ReaderWriter getReaderWriter();

    /**
     * Returns whether or not the TURN client has set its "active destination"
     * turning off TURN messaging on that connection.
     * @return <code>true</code> if the client has set the active destination,
     * otherwise <code>false</code>.
     */
    boolean hasActiveDestination();

    /**
     * Adds the specified connection for this TURN client.  This is typically
     * called when a remote host makes a connection to the TURN server.
     *
     * @param destinationAddress The IP address fo the remote host.
     * @param readerWriter The class for reading and writing data with the
     * remote host.
     */
    void addConnection(final InetSocketAddress destinationAddress,
        final ReaderWriter readerWriter);

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
    }
