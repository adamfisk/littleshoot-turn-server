package org.lastbamboo.common.turn.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.map.LRUMap;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.attributes.turn.ConnectionStatus;
import org.lastbamboo.common.stun.stack.message.turn.ConnectionStatusIndication;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.turn.server.allocated.TcpAllocatedTurnServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default TURN client implementation.  Starts the server on behalf of the
 * TURN client and keeps track of remote hosts that have permission to send
 * incoming data to this client.<p>
 *
 * Note that this class does not implement <code>CloseListener</code> because
 * doing so would require us to keep track of a separate data structure of
 * "cancelled" connections.  Instead, we simply allow the write attempt to
 * throw an <code>IOException</code>, allowing the caller to respond with
 * "Send Error Response" messages to the TURN client as appropriate.<p>
 *
 * Note that the lifetime of TURN allocations is handled externally 
 * according to how the connection has been idle.<p>
 * 
 * We deviate from the TURN spec here because it just seems to make sense
 * to only close TURN sessions when there's been no data, not after fixed
 * periods regardless of whether there's data or not.
 */
public final class TurnClientImpl implements TurnClient
    {

    /**
     * Logger for this class.
     */
    private static final Logger LOG = 
        LoggerFactory.getLogger(TurnClientImpl.class);

    private final InetSocketAddress m_allocatedAddress;

    /**
     * <code>Map</code> of <code>InetSocketAddress</code>es to handler for
     * writing to those connections.
     */
    private final Map<InetSocketAddress, IoSession> m_connections = 
        new ConcurrentHashMap<InetSocketAddress, IoSession>();

    /**
     * The addresses that the TURN client has issues SEND-REQUESTS to, giving
     * them permission to send incoming data to the client.  These are all
     * <code>InetAddress</code>es.  This is a <code>Map</code> only to
     * reuse the LRU map functionality in Jakarta Commons Collections.  There's
     * unfortunately no LRUSet.  We limit the size instead of worrying about
     * the TURN LIFETIME attribute for now.
     */
    private final Map<InetAddress, InetAddress> m_permittedAddresses =
        Collections.synchronizedMap(new LRUMap(100));
    
    /**
     * The set of remote hosts the client has issued connect requests for.
     */
    private final Set<InetAddress> m_trackedRemoteHosts =
        Collections.synchronizedSet(new HashSet<InetAddress>());

    private final IoSession m_ioSession;

    private TcpAllocatedTurnServer m_allocatedTurnServer;

    /**
     * Creates a new TURN client abstraction for the specified TURN client
     * address and port.
     * @param allocatedAddress The address and port this server has allocated
     * on behalf of the TURN client.  This is the address and port the client
     * will report as its own address and port when communicating with other
     * clients.
     * @param ioSession The handler for writing data back to the TURN client.
     */
    public TurnClientImpl(final InetSocketAddress allocatedAddress,
        final IoSession ioSession)
        {
        this.m_allocatedAddress = allocatedAddress;
        this.m_ioSession = ioSession;
        }

    public void startServer()
        {
        this.m_allocatedTurnServer = 
            new TcpAllocatedTurnServer(this, this.m_allocatedAddress.getPort());
        this.m_allocatedTurnServer.start();
        }

    public boolean write(final InetSocketAddress remoteAddress,
        final ByteBuffer data) 
        {
        final IoSession session = this.m_connections.get(remoteAddress);
        if (session == null)
            {
            // The remote host likely just disconnected, and we should have
            // properly informed the TURN client with a connection status
            // message.  This will happen periodically if the TURN client
            // has sent a little extra data.
            LOG.debug("Attempting to send data to host that's not there: "+ 
                remoteAddress);
            return false;
            }
        else
            {
            LOG.debug("Writing data using existing connection...");
            session.write(data);
            return true;
            }
        }
    
    public void onRemoteHostData(final InetSocketAddress remoteAddress,
        final byte[] data)
        {
        final DataIndication indication = 
            new DataIndication(remoteAddress, data);
        this.m_ioSession.write(indication);
        }
    
    public void handleConnect(final InetSocketAddress remoteAddress)
        {
        LOG.debug("Adding connect permission for: {}  {}", remoteAddress, this);
        final InetAddress address = remoteAddress.getAddress();
        this.m_permittedAddresses.put(address, address);
        this.m_trackedRemoteHosts.add(address);
        updateConnectionStatus(remoteAddress, ConnectionStatus.LISTEN);
        }

    public InetSocketAddress getAllocatedSocketAddress()
        {
        return this.m_allocatedAddress;
        }

    public void close()
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Closing client at: " + this.m_ioSession);
            }
        closeAllConnections();
        this.m_allocatedTurnServer.stop();
        this.m_connections.clear();
        this.m_permittedAddresses.clear();
        
        // The session is probably already closed, but just make sure.
        this.m_ioSession.close();
        }

    /**
     * Closes all connections to remote hosts associated with this TURN client.
     */
    private void closeAllConnections()
        {
        final Iterator<IoSession> iter = this.m_connections.values().iterator();
        while (iter.hasNext())
            {
            final IoSession readerWriter = iter.next();
            readerWriter.close();
            }
        }

    public IoSession getIoSession()
        {
        return this.m_ioSession;
        }

    public boolean hasActiveDestination()
        {
        // TODO Implement active destination handling.
        return false;
        }

    public void removeConnection(final IoSession session)
        {
        LOG.debug("Removing connection to: {}", session);
        final InetSocketAddress remoteAddress = normalizeSocketAddress(session);
        final IoSession connection = this.m_connections.remove(remoteAddress);

        // The connection can be null if a host attempted to connect that 
        // never had permission to, and we've closed it.  That will generate
        // this event.
        if (connection != null)
            {
            // It's probably already closed, but just in case.
            connection.close();
            updateConnectionStatus(remoteAddress, ConnectionStatus.CLOSED);
            }
        }
    
    public void addConnection(final IoSession session) 
        {
        final InetSocketAddress socketAddress = normalizeSocketAddress(session);
        // Make sure the host has permissions.
        /*
        if (!hasIncomingPermission(session))
            {
            // This could possibly happen if another thread removed 
            // the host's incoming permission.  Just close the session here.
            LOG.debug("No permissions for host: "+socketAddress.getAddress());
            session.close();
            }
        else
        */
            {
            this.m_connections.put(socketAddress, session);
            updateConnectionStatus(socketAddress, ConnectionStatus.ESTABLISHED);
            if (LOG.isDebugEnabled())
                {
                LOG.debug("Now "+this.m_connections.size()+" connection(s)...");
                }
            }
        }

    private void updateConnectionStatus(final InetSocketAddress remoteAddress, 
        final ConnectionStatus status)
        {
        final ConnectionStatusIndication indication = 
            new ConnectionStatusIndication(remoteAddress, status);
        this.m_ioSession.write(indication);
        }

    public boolean hasIncomingPermission(final IoSession session)
        {
        LOG.debug("Checking permissions for: {}", session);
        final InetSocketAddress socketAddress = normalizeSocketAddress(session);
        
        final boolean hasPermission = 
            m_trackedRemoteHosts.contains(socketAddress.getAddress());
        LOG.debug("{} returning permission: {}", this, 
            new Boolean(hasPermission));
        return hasPermission;
        }

    /**
     * This method takes the {@link SocketAddress} from the session and 
     * normalizes it.  This is necessary because {@link InetSocketAddress}es
     * can contain textual information in addition to the IP address and port.
     * This information may or may not be present depending on where the
     * information came from, requiring it to be normalized so that socket
     * addresses match as {@link Map} keyps.
     * 
     * @param session The session containing the {@link InetSocketAddress} to
     * normalize.
     * 
     * @return The normalized {@link InetSocketAddress}.
     */
    private InetSocketAddress normalizeSocketAddress(final IoSession session)
        {
        return (InetSocketAddress) session.getRemoteAddress();
        }

    /**
     * {@inheritDoc}
     */
    public int getNumConnections()
        {
        return m_connections.size();
        }

    }
