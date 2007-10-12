package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
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

    private final InetAddress m_publicAddress;
    
    /**
     * Creates a new TURN client manager.
     */
    public TurnClientManagerImpl()
        {
        // We need to determine the public address of the EC2 server -- we need
        // to give this to clients when allocating relays.
        m_publicAddress = getPublicAddress();
        }
    
    private static InetAddress getPublicAddress()
        {
        // First just check if we're even on Amazon -- we could be testing
        // locally, for example.
        LOG.debug("Getting public address");
        
        // Check to see if we're running on EC2.  If we're not, we're probably 
        // testing.  This technique could be a problem if the EC2 internal 
        // addressing is ever different from 10.253.
        try
            {
            if (!NetworkUtils.getLocalHost().getHostAddress().startsWith("10.253"))
                {
                // Not running on EC2.
                LOG.debug("Not running on EC2.  Testing??");
                return NetworkUtils.getLocalHost();
                }
            }
        catch (final UnknownHostException e)
            {
            LOG.error("Could not get host.", e);
            return null;
            }
        final String url = "http://169.254.169.254/latest/meta-data/public-ipv4";
        final HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(
            10 * 1000);
        final GetMethod method = new GetMethod(url);
        try
            {
            LOG.debug("Executing method...");
            final int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK)
                {
                LOG.warn("ERROR ISSUING REQUEST:\n" + method.getStatusLine() + 
                    "\n" + method.getResponseBodyAsString());
                return null;
                }
            else
                {
                LOG.debug("Successfully wrote request...");
                }
            final String host = method.getResponseBodyAsString();
            LOG.debug("Got address: "+host);
            return InetAddress.getByName(host);
            }
        catch (final HttpException e)
            {
            LOG.error("Could not access EC2 service", e);
            return null;
            }
        catch (final IOException e)
            {
            LOG.error("Could not access EC2 service", e);
            return null;
            }
        finally 
            {
            method.releaseConnection();
            }
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
    }
