package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketAddress;

import javax.management.MBeanServer;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoHandler;
import org.littleshoot.mina.common.IoService;
import org.littleshoot.mina.common.IoServiceConfig;
import org.littleshoot.mina.common.IoServiceListener;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.common.SimpleByteBufferAllocator;
import org.littleshoot.mina.filter.codec.ProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.StunConstants;
import org.lastbamboo.common.stun.stack.StunProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.littleshoot.util.JmxUtils;
import org.littleshoot.util.mina.MinaTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for processing TURN TCP messages.
 */
public class TcpTurnServer implements TurnServer, IoServiceListener,
    TcpTurnServerMBean
    {

    private final Logger m_log = LoggerFactory.getLogger(TcpTurnServer.class);
    
    private final TurnClientManager m_turnClientManager;

    private final MinaTcpServer m_minaServer;
    
    /**
     * Creates a new TCP TURN server.
     */
    public TcpTurnServer()
        {
        this (new TurnClientManagerImpl());
        }
    
    /**
     * Creates a new TCP TURN server.
     * 
     * @param turnClientManager The class that manages TURN clients.
     */
    public TcpTurnServer(final TurnClientManager turnClientManager)
        {
        // Configure the MINA buffers for optimal performance.
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        
        this.m_turnClientManager = turnClientManager;
        final ProtocolCodecFactory codecFactory = 
            new StunProtocolCodecFactory();
        
        final StunMessageVisitorFactory visitorFactory =
            new TurnServerMessageVisitorFactory(this.m_turnClientManager);
        final IoHandler handler = new TurnServerIoHandler(visitorFactory);
        
        this.m_minaServer = 
            new MinaTcpServer(codecFactory, this, handler, 
                "TCP-TURN-Server");
        }
    
    public void start() throws IOException
        {
        this.m_minaServer.start(StunConstants.STUN_PORT);
        //startJmxServer();
        }
    
    public void stop()
        {
        m_log.debug("Stopping server...");
        this.m_minaServer.stop();
        }

    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.debug("Started server on: {}", serviceAddress);
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.warn("TURN Server deactivated on: {}", serviceAddress);
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
    
    private void startJmxServer()
        {
        m_log.debug("Starting JMX server...");
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JmxUtils.register(mbs, this.m_turnClientManager);
        JmxUtils.register(mbs, this);
        }

    public int getTurnPort()
        {
        return StunConstants.STUN_PORT;
        }

    }
