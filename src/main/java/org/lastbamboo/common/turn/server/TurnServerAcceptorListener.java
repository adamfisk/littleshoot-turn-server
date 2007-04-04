package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.AcceptorListener;
import org.lastbamboo.common.nio.NioReaderWriter;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.TurnProtocolHandler;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.TurnMessageVisitor;
import org.lastbamboo.common.turn.message.handler.TurnMessageHandlerFactory;

/**
 * Creates a new listener for accepted sockets to the TURN server.  These will
 * typically come from TURN clients.
 */
public final class TurnServerAcceptorListener implements AcceptorListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(TurnServerAcceptorListener.class);
    
    private final SelectorManager m_selectorManager;

    private final TurnMessageHandlerFactory m_turnMessageHandlerFactory;

    private final TurnMessageFactory m_turnMessageFactory;

    private final TurnClientManager m_turnClientManager;

    /**
     * Creates a new class that listens for incoming connections from TURN
     * clients attempting to allocate new TURN bindings.
     * 
     * @param manager The class that manages the selector for incoming
     * TURN client messages.
     * @param messageHandlerFactory The class that creates handlers for
     * different types of TURN messages.
     * @param messageFactory The class that creates new TURN messages.
     * @param clientManager The class that manages allocated sockets for
     * TURN clients.
     */
    public TurnServerAcceptorListener(final SelectorManager manager,
        final TurnMessageHandlerFactory messageHandlerFactory,
        final TurnMessageFactory messageFactory,
        final TurnClientManager clientManager)
        {
        this.m_selectorManager = manager;
        this.m_turnMessageHandlerFactory = messageHandlerFactory;
        this.m_turnMessageFactory = messageFactory;
        this.m_turnClientManager = clientManager;
        }
    
    public void onAccept(final SocketChannel sc)
        {
        final ReaderWriter readerWriter;
        try
            {
            readerWriter = new NioReaderWriter(sc, this.m_selectorManager);
            }
        catch (final SocketException e)
            {
            handleException(sc, e);
            return;
            }
        catch (final IOException e)
            {
            handleException(sc, e);
            return;
            }
        
        final TurnMessageVisitor visitor = 
            new TurnServerResponder(readerWriter, this.m_turnMessageFactory,
                this.m_turnClientManager);
        final ProtocolHandler protocolHandler = 
            new TurnProtocolHandler(this.m_turnMessageHandlerFactory, visitor);
        readerWriter.setProtocolHandler(protocolHandler);
        }

    private void handleException(final SocketChannel sc, final IOException e)
        {
        LOG.warn("Unexpected exception on the socket", e);
        try
            {
            sc.close();
            }
        catch (final IOException ioe)
            {
            LOG.warn("Unexpected exception closing socket", ioe);
            }
        }

    }
