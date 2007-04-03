package org.lastbamboo.shoot.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.AcceptorListener;
import org.lastbamboo.common.nio.NioReaderWriter;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.shoot.protocol.ProtocolHandler;
import org.lastbamboo.shoot.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;

/**
 * Listener for incoming connections attempting to send data to one of this
 * server's TURN clients.
 */
public final class RemoteHostAcceptorListener implements AcceptorListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(RemoteHostAcceptorListener.class);
    
    private final TurnClient m_turnClient;

    /**
     * The selector registering for event types for the new client as necessary.
     */
    private final SelectorManager m_selectorManager;

    /**
     * Factory for creating TURN messages.
     */
    private final TurnMessageFactory m_turnMessageFactory;

    /**
     * Creates a new listener for incoming connections from remote hosts to 
     * one of our TURN clients.
     * @param turnClient The TURN client the remote host is trying to send 
     * data to.
     * @param manager The class that manages the NIO selector.
     * @param messageFactory The factory for creating TURN messages.
     */
    public RemoteHostAcceptorListener(final TurnClient turnClient,
        final SelectorManager manager, final TurnMessageFactory messageFactory)
        {
        this.m_turnClient = turnClient;
        this.m_selectorManager = manager;
        this.m_turnMessageFactory = messageFactory;
        }

    public void onAccept(final SocketChannel sc)
        {
        LOG.trace("Checking to accept socket from remote host: "+sc.socket());
        final InetSocketAddress socketAddress = 
            new InetSocketAddress(sc.socket().getInetAddress(), 
                sc.socket().getPort());
        /*
        if (!this.m_turnClient.hasIncomingPermission(socketAddress.getAddress()))
            {
            try
                {
                LOG.trace("Rejecting connection from: "+socketAddress);
                sc.close();
                }
            catch (final IOException e)
                {
                // Nothing to do.
                LOG.debug("Exception closing socket", e);
                }
            }
            */
        
        //else
          //  {
            LOG.trace("Accepting socket from remote host!!! "+socketAddress);
            createClientHandler(socketAddress, sc);
            //}
        }

    /**
     * Creates a new handler for the specified socket.  This occurs after we've
     * accepted the connection from the remote host.
     * @param socketAddress The address of the remote host.
     * @param sc The channel to handle.
     */
    private void createClientHandler(final InetSocketAddress socketAddress, 
        final SocketChannel sc)
        {
        final ProtocolHandler protocolHandler = 
            new RemoteHostProtocolHandler(this.m_turnClient, 
                this.m_turnMessageFactory);
        
        try
            {
            // This also registers the handler.
            final ReaderWriter readerWriter =
                new NioReaderWriter(sc, this.m_selectorManager, 
                    protocolHandler);
            
            this.m_turnClient.addConnection(socketAddress, readerWriter);
            }
        catch (final SocketException e)
            {
            handleException(sc, e);
            }
        catch (final IOException e)
            {
            handleException(sc, e);
            }
        }

    private void handleException(final SocketChannel sc, final IOException e)
        {
        LOG.warn("Unexpected socket exception", e);
        try
            {
            sc.close();
            }
        catch (final IOException ioe)
            {
            LOG.warn("Could not close socket", ioe);
            }
        }
    }
