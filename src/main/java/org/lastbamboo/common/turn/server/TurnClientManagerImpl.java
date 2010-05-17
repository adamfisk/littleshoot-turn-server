package org.lastbamboo.common.turn.server;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.amazon.ec2.AmazonEc2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages endpoint bindings for TURN clients.  This includes allocating
 * bindings, timing out bindings, etc.
 */
public final class TurnClientManagerImpl implements TurnClientManager,
    TurnClientManagerImplMBean
    {
    
    /**
     * Logger for this class.
     */
    private final Logger m_log = 
        LoggerFactory.getLogger(TurnClientManagerImpl.class);
    
    /**
     * Map of {@link IoSession}s to TURN clients.  Each {@link IoSession}
     * represents a connection over which a client has issued an Allocate
     * Request method. 
     */
    private final Map<IoSession, TurnClient> m_clientMappings = 
        new ConcurrentHashMap<IoSession, TurnClient>();

    private final InetAddress m_publicAddress;

    /**
     * The maximum number of TURN clients we've seen.
     */
    private int m_maxSize = 0;

    private int m_maxRemotePerClient = 0;

    private int m_maxRemoteClients;

    /**
     * Creates a new TURN client manager.
     */
    public TurnClientManagerImpl()
        {
        // We need to determine the public address of the EC2 server -- we need
        // to give this to clients when allocating relays.
        m_publicAddress = AmazonEc2Utils.getPublicAddress();
        }

    public TurnClient allocateBinding(final IoSession ioSession) 
        {
        // If we already have a client, then the allocation acts as a 
        // keep-alive to keep the binding active.  Just the fact that there's
        // traffic suffices to keep it alive -- we don't need to notify the
        // client.
        if (this.m_clientMappings.containsKey(ioSession))
            {
            m_log.debug("Keep alive -- we already have the binding");
            return this.m_clientMappings.get(ioSession);
            }
        
        // Otherwise, we need to allocate a new server for the new client.
        else
            {
            final TurnClient turnClient = 
                new TurnClientImpl(m_publicAddress, ioSession);
            turnClient.startServer();
            this.m_clientMappings.put(ioSession, turnClient);
            
            if (this.m_clientMappings.size() > m_maxSize)
                {
                m_maxSize = this.m_clientMappings.size();
                }
            return turnClient;
            }
        }

    public TurnClient getTurnClient(final IoSession readerWriter)
        {
        return this.m_clientMappings.get(readerWriter);
        }

    public TurnClient removeBinding(final IoSession session)
        {
        if (m_log.isDebugEnabled())
            {
            m_log.debug("Removing binding for: "+session);
            }
        final TurnClient client = this.m_clientMappings.remove(session);
        if (client != null)
            {
            client.close();
            }
        return client;
        }

    public int getNumTurnClients()
        {
        return this.m_clientMappings.size();
        }

    public int getMaxNumTurnClients()
        {
        return this.m_maxSize;
        }
    
    public int getNumRemoteTurnClients()
        {
        int numRemoteClients = 0;
        
        // We unfortunately have to synchronize on the iteration here, but
        // it should be really quick.
        synchronized (this.m_clientMappings)
            {
            final Collection<TurnClient> clients = 
                this.m_clientMappings.values();
            for (final TurnClient client : clients)
                {
                final int numRemote = client.getNumConnections();
                
                // Record the maximum number of remote host connections per 
                // client.
                if (numRemote > m_maxRemotePerClient)
                    {
                    m_maxRemotePerClient = numRemote;
                    }
                numRemoteClients += numRemote;
                }
            }
        
        // Record the maximum total remote connections.
        if (numRemoteClients > m_maxRemoteClients)
            {
            m_maxRemoteClients = numRemoteClients;
            }
        return numRemoteClients;
        }

    public int getMaxNumRemoteTurnClients()
        {
        return this.m_maxRemoteClients;
        }

    public int getMaxNumRemoteSingleTurnClient()
        {
        return this.m_maxRemotePerClient;
        }
    }
