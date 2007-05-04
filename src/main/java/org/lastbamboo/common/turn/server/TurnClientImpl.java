package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.AcceptorListener;
import org.lastbamboo.common.nio.NioServer;
import org.lastbamboo.common.nio.NioServerImpl;
import org.lastbamboo.common.nio.ReadWriteConnectorImpl;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.protocol.ReadWriteConnector;
import org.lastbamboo.common.protocol.ReadWriteConnectorListener;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.message.TurnMessageFactory;


/**
 * Default TURN client implementation.  Starts the server on behalf of the
 * TURN client and keeps track of remote hosts that have permission to send
 * incoming data to this client.
 *
 * Note that this class does not implement <code>CloseListener</code> because
 * doing so would require us to keep track of a separate data structure of
 * "cancelled" connections.  Instead, we simply allow the write attempt to
 * throw an <code>IOException</code>, allowing the caller to respond with
 * "Send Error Response" messages to the TURN client as appropriate.
 *
 * TODO: Only allow permissions for a fixed LIFETIME (I think that's the
 * TURN attribute).
 */
public final class TurnClientImpl implements TurnClient,
    ReadWriteConnectorListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(TurnClientImpl.class);

    private final InetSocketAddress m_allocatedAddress;

    /**
     * <code>Map</code> of <code>InetSocketAddress</code>es to handler for
     * writing to those connections.
     */
    private final Map m_connections = new ConcurrentHashMap();

    /**
     * <code>Map</code> of <code>InetSocketAddress</code>es to
     * <code>ByteBuffer</code>s for sending data to hosts we don't have a
     * connection to yet. This is cleared if we're unable to connect to the
     * remote host.
     */
    private final Map m_pendingData = new ConcurrentHashMap();

    private final SelectorManager m_selectorManager;

    /**
     * The addresses that the TURN client has issues SEND-REQUESTS to, giving
     * them permission to send incoming data to the client.  These are all
     * <code>InetAddress</code>es.  This is a <code>Map</code> only to
     * reuse the LRU map functionality in Jakarta Commons Collections.  There's
     * unfortunately no LRUSet.  We limit the size instead of worrying about
     * the TURN LIFETIME attribute for now.
     */
    private final Map m_permittedAddresses =
        Collections.synchronizedMap(new LRUMap(100));

    private final ReaderWriter m_readerWriter;

    private final TurnMessageFactory m_turnMessageFactory;

    /**
     * Server that processing incoming connections to the TURN client.
     */
    private NioServer m_nioServer;

    /**
     * Creates a new TURN client abstraction for the specified TURN client
     * address and port.
     * @param allocatedAddress The address and port this server has allocated
     * on behalf of the TURN client.  This is the address and port the client
     * will report as its own address and port when communicating with other
     * clients.
     * @param readerWriter The handler for writing data back to the TURN client.
     * @param manager Manager for the NIO selector.
     * @param messageFactory The factory for creating TURN messages.
     */
    public TurnClientImpl(final InetSocketAddress allocatedAddress,
        final ReaderWriter readerWriter, final SelectorManager manager,
        final TurnMessageFactory messageFactory)
        {
        this.m_allocatedAddress = allocatedAddress;
        this.m_readerWriter = readerWriter;
        this.m_selectorManager = manager;
        this.m_turnMessageFactory = messageFactory;
        }

    public void startServer()
        {
        LOG.trace("Starting server on thread: "+
            Thread.currentThread().getName());

        // Note that we use the same selector for all servers.
        final AcceptorListener listener =
            new RemoteHostAcceptorListener(this, this.m_selectorManager,
                this.m_turnMessageFactory);
        this.m_nioServer = new NioServerImpl(this.m_allocatedAddress.getPort(),
            this.m_selectorManager, listener);
        try
            {
            this.m_nioServer.startServer();
            }
        catch (final IOException e)
            {
            // Should never happen.
            LOG.fatal("Could not start server", e);
            }
        }

    public boolean write(final InetSocketAddress remoteAddress,
        final ByteBuffer data) throws IOException
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Writing " + data.remaining() + " bytes of data to: " +
                remoteAddress);
            }
        try
            {
            return writeToRemoteHost(remoteAddress, data);
            }
        catch (final IOException e)
            {
            LOG.debug("Could not write data to remote host", e);
            // Remove all connection to the remote address internally.
            remove(remoteAddress);
            throw e;
            }
        }

    private boolean writeToRemoteHost(final InetSocketAddress remoteAddress,
        final ByteBuffer data) throws IOException
        {
        final ReaderWriter readerWriter =
            (ReaderWriter) this.m_connections.get(remoteAddress);
        if (readerWriter == null)
            {
            // This will happen when clients send empty TURN Send Requests to
            // open permissions for that user.  This is the TURN server
            // acting like a cone NAT, where it will only allow incoming data
            // from remote hosts that TURN clients have sent outgoing data
            // to.  So, an empty Send Request should always be the TURN
            // client opening permissions to a specific host.
            //
            // If there is data, and we're opening a connection, then there
            // might be something wrong, and we issue a warning.
            LOG.trace("Opening new connection to host...");
            if (data.capacity() != 0)
                {
                LOG.warn("Opening new connection to host with data!!!");
                }
            addPermission(remoteAddress.getAddress());
            addPendingData(remoteAddress, data);
            connect(remoteAddress);
            return false;
            }
        else
            {
            LOG.trace("Writing data using existing connection...");
            readerWriter.write(data);
            return true;
            }
        }

    private void remove(final InetSocketAddress remoteAddress)
        {
        LOG.trace("Removing remote address: "+remoteAddress);

        // We don't remove the permission for this host here because we still
        // want to allow connections from it.  This should technically be
        // tied to the binding's LIFETIME attribute, but we don't yet
        // implement that.
        this.m_pendingData.remove(remoteAddress);
        this.m_connections.remove(remoteAddress);
        }

    private void addPermission(final InetAddress address)
        {
        this.m_permittedAddresses.put(address, address);
        }

    private void addPendingData(final InetSocketAddress remoteAddress,
        final ByteBuffer data)
        {
        if (data.capacity() != 0)
            {
            this.m_pendingData.put(remoteAddress, data);
            }
        }

    private void connect(final InetSocketAddress remoteAddress)
        throws IOException
        {
        final ReadWriteConnector connector =
            new ReadWriteConnectorImpl(this.m_selectorManager, remoteAddress,
                this);
        connector.connect();

        }

    public void onConnect(final ReaderWriter readerWriter) throws IOException
        {
        LOG.trace("Connected to server...");
        final InetSocketAddress remoteAddress =
            readerWriter.getRemoteSocketAddress();
        final ByteBuffer data =
            (ByteBuffer) this.m_pendingData.remove(remoteAddress);

        if (data != null)
            {
            LOG.trace("Writing pending data...");
            readerWriter.write(data);
            }
        addConnection(remoteAddress, readerWriter);
        }

    public void onConnectFailed(final InetSocketAddress remoteAddress)
        {
        // Just remove the host from any pending writes.
        this.m_pendingData.remove(remoteAddress);
        }

    public boolean hasIncomingPermission(final InetAddress address)
        {
        return this.m_permittedAddresses.containsKey(address);
        }

    public InetSocketAddress getAllocatedSocketAddress()
        {
        return this.m_allocatedAddress;
        }

    public void close()
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Closing client at: " + this.m_readerWriter);
            }
        this.m_nioServer.close();
        this.m_pendingData.clear();
        this.m_permittedAddresses.clear();

        closeAllConnections();
        this.m_connections.clear();
        }

    /**
     * Closes all connections to remote hosts associated with this TURN client.
     */
    private void closeAllConnections()
        {
        final Iterator iter = this.m_connections.values().iterator();
        while (iter.hasNext())
            {
            final ReaderWriter readerWriter = (ReaderWriter) iter.next();
            readerWriter.close();
            }
        }

    public ReaderWriter getReaderWriter()
        {
        return this.m_readerWriter;
        }

    public boolean hasActiveDestination()
        {
        // TODO Implement active destination handling.
        return false;
        }

    public void addConnection(final InetSocketAddress socketAddress,
        final ReaderWriter readerWriter)
        {
        LOG.trace("Adding connection to: "+socketAddress);

        // Make sure the host has permissions.
        /*
        if (!hasIncomingPermission(socketAddress.getAddress()))
            {
            LOG.warn("No permissions for host: "+socketAddress.getAddress());
            throw new IllegalArgumentException("No permissions for host: "+
                socketAddress.getAddress());
            }
            */
        this.m_connections.put(socketAddress, readerWriter);
        addPermission(socketAddress.getAddress());
        LOG.trace("Now "+this.m_connections.size()+" connection(s)...");
        }

    /**
     * {@inheritDoc}
     */
    public int getNumConnections()
        {
        return m_connections.size();
        }

    public String toString()
        {
        return "TurnClientImpl managing connections for: "+this.m_readerWriter;
        }
    }
