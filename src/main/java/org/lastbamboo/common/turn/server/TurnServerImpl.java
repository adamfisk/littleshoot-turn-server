package org.lastbamboo.common.turn.server;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.AcceptorListener;
import org.lastbamboo.common.nio.NioServer;
import org.lastbamboo.common.nio.NioServerImpl;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.shoot.turn.TurnConstants;

/**
 * Creates a new TURN server.
 */
public final class TurnServerImpl implements TurnServer
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(TurnServerImpl.class);

    private final SelectorManager m_selectorManager;

    private final AcceptorListener m_acceptorListener;

    /**
     * Creates a new TURN server.
     *
     * @param manager The NIO selector class.
     * @param acceptorListener The listener for incoming sockets.
     */
    public TurnServerImpl(final SelectorManager manager,
        final AcceptorListener acceptorListener)
        {
        this.m_selectorManager = manager;
        this.m_acceptorListener = acceptorListener;
        }

    public void start()
        {
        final int port = TurnConstants.DEFAULT_SERVER_PORT;
        LOG.trace("Starting TURN server on port: "+port);
        final NioServer server =
            new NioServerImpl(port,
                this.m_selectorManager, this.m_acceptorListener);
        try
            {
            server.startServer();
            }
        catch (final IOException e)
            {
            LOG.fatal("Could not start server", e);
            }
        }
    }
