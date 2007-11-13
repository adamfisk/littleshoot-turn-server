package org.lastbamboo.common.turn.server;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.amazon.ec2.AmazonEc2CandidateProvider;
import org.lastbamboo.common.amazon.ec2.AmazonEc2Utils;

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
    private static final Log LOG = 
        LogFactory.getLog(TurnClientManagerImpl.class);
    
    /**
     * Map of {@link IoSession}s to TURN clients.  Each {@link IoSession}
     * represents a connection over which a client has issued an Allocate
     * Request method. 
     */
    private final Map<IoSession, TurnClient> m_clientMappings = 
        new ConcurrentHashMap<IoSession, TurnClient>();

    private final InetAddress m_publicAddress;
    
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
            return this.m_clientMappings.get(ioSession);
            }
        
        // Otherwise, we need to allocate a new server for the new client.
        else
            {
            // Allocate an ephemeral port.
            //final InetSocketAddress relayAddress = 
              //  new InetSocketAddress(m_publicAddress, 0);
            final TurnClient turnClient = 
                new TurnClientImpl(m_publicAddress, ioSession);
            turnClient.startServer();
            this.m_clientMappings.put(ioSession, turnClient);
            return turnClient;
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


    public int getNumClients()
        {
        return this.m_clientMappings.size();
        }
    }
