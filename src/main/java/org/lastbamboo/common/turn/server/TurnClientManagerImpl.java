package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoSession;
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
    
    public TurnClient allocateBinding(final IoSession ioSession) 
        {
        // If we already have a client, then the allocation acts as a 
        // keep-alive to keep the binding active.  Just the fact that there's
        // traffic suffices to keep it alive -- we don't need to notify the
        // client.
        if (this.m_clientMappings.containsKey(ioSession))
            {
            return this.m_clientMappings.get(ioSession);
            }
        
        // Otherwise, we need to allocate a new server for the new client.
        else
            {
            try
                {
                // Allocate an ephemeral port.
                final InetSocketAddress relayAddress = 
                    new InetSocketAddress(NetworkUtils.getLocalHost(), 0);
                
                final TurnClient turnClient = 
                    new TurnClientImpl(relayAddress, ioSession);
                turnClient.startServer();
                this.m_clientMappings.put(ioSession, turnClient);
                
                return turnClient;
                }
            catch (final UnknownHostException e)
                {
                // Should never happen.
                LOG.error("Could not resolve host", e);
                return null;
                }
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
            }
        return client;
        }
    }
