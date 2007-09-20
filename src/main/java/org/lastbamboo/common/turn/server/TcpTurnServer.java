package org.lastbamboo.common.turn.server;

import java.net.SocketAddress;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.StunProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.util.mina.MinaTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for processing TURN TCP messages.
 */
public class TcpTurnServer implements TurnServer, IoServiceListener
    {

    private final Logger m_log = LoggerFactory.getLogger(TcpTurnServer.class);
    
    private final TurnClientManagerImpl m_turnClientManager;

    private final MinaTcpServer m_minaServer;
    
    /**
     * Use the default STUN port.
     */
    private static final int STUN_PORT = 3478;

    /**
     * Creates a new TCP TURN server.
     */
    public TcpTurnServer()
        {
        // Configure the MINA buffers for optimal performance.
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        
        this.m_turnClientManager = new TurnClientManagerImpl();
        final ProtocolCodecFactory codecFactory = 
            new StunProtocolCodecFactory();
        
        final StunMessageVisitorFactory visitorFactory =
            new TurnServerMessageVisitorFactory(this.m_turnClientManager);
        final IoHandler handler = new TurnServerIoHandler(visitorFactory);
        
        this.m_minaServer = 
            new MinaTcpServer(codecFactory, this, handler, STUN_PORT, 
                "TCP-TURN-Server");
        }
    
    public void start()
        {
        this.m_minaServer.start();
        }
    
    public void stop()
        {
        this.m_minaServer.stop();
        }

    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.debug("Started server on: "+serviceAddress);
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.warn("TURN Server deactivated!!");
        }

    public void sessionCreated(final IoSession session)
        {
        }

    public void sessionDestroyed(final IoSession session)
        {
        // Key -- we need to tell the client manager the session with the
        // TURN client has closed.
        m_log.debug("TURN client disconnected: "+session);
        this.m_turnClientManager.removeBinding(session);
        }

    }
