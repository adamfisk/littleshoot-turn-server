package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.turn.RandomNonCollidingPortGenerator;
import org.lastbamboo.common.util.NetworkUtils;

/**
 * Manages endpoint bindings for TURN clients.  This includes allocating
 * bindings, timing out bindings, etc.
 */
public final class TurnClientManagerImpl implements TurnClientManager
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(TurnClientManagerImpl.class);
    
    /**
     * Map of <code>InetSocketAddress</code>es to TURN clients. 
     */
    private final Map<IoSession, TurnClient> m_clientMappings = 
        new ConcurrentHashMap<IoSession, TurnClient>();

    private final RandomNonCollidingPortGenerator m_portGenerator;
    
    /**
     * Manager for clients this TURN server is relaying data on behalf of.
     * 
     * @param portGenerator Class for generating ports to use for new clients.
     */
    public TurnClientManagerImpl( 
        final RandomNonCollidingPortGenerator portGenerator)
        {
        this.m_portGenerator = portGenerator;
        }
    
    public TurnClient allocateBinding(final IoSession ioSession) 
        {
        final int newPort = this.m_portGenerator.createRandomPort();
        try
            {
            final InetSocketAddress allocatedAddress = 
                new InetSocketAddress(NetworkUtils.getLocalHost(), newPort);
            
            final TurnClient turnClient = 
                new TurnClientImpl(allocatedAddress, ioSession);
            turnClient.startServer();
            this.m_clientMappings.put(ioSession, turnClient);
            
            return turnClient;
            }
        catch (final UnknownHostException e)
            {
            // Should never happen.
            LOG.error("Could not resolve host", e);
            this.m_portGenerator.removePort(newPort);
            return null;
            }
        }

    public TurnClient getTurnClient(final IoSession readerWriter)
        {
        return this.m_clientMappings.get(readerWriter);
        }

    public TurnClient removeBinding(final IoSession session)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Removing binding for: "+session);
            }
        final TurnClient client = this.m_clientMappings.remove(session);
        if (client != null)
            {
            client.close();
            final int port = client.getAllocatedSocketAddress().getPort();
            this.m_portGenerator.removePort(port);
            }
        return client;
        }
    }
