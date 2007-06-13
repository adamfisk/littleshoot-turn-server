package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.lastbamboo.common.stun.stack.decoder.StunMessageDecodingState;
import org.lastbamboo.common.stun.stack.encoder.StunProtocolEncoder;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.turn.RandomNonCollidingPortGenerator;
import org.lastbamboo.common.stun.stack.turn.RandomNonCollidingPortGeneratorImpl;
import org.lastbamboo.common.util.mina.StateMachineProtocolDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for processing TURN TCP messages.
 */
public class TcpTurnServer implements TurnServer, IoServiceListener
    {

    private final Logger LOG = LoggerFactory.getLogger(TcpTurnServer.class);
    
    private final TurnClientManagerImpl m_turnClientManager;

    private SocketAcceptor m_acceptor;
    
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
        
        final RandomNonCollidingPortGenerator portGenerator = 
            new RandomNonCollidingPortGeneratorImpl();
        this.m_turnClientManager = new TurnClientManagerImpl(portGenerator);
        }
    
    public void start()
        {
        final Executor threadPool = Executors.newCachedThreadPool();
        this.m_acceptor = new SocketAcceptor(
            Runtime.getRuntime().availableProcessors() + 1, threadPool);
        
        m_acceptor.addListener(this);
        
        final ProtocolEncoder encoder = new StunProtocolEncoder();
        final ProtocolDecoder decoder = 
            new StateMachineProtocolDecoder(new StunMessageDecodingState());
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(encoder, decoder);
        m_acceptor.getFilterChain().addLast("to-stun", stunFilter);
        
        
        final StunMessageVisitorFactory visitorFactory =
            new TurnServerMessageVisitorFactory(this.m_turnClientManager);
        final IoHandler handler = new TurnServerIoHandler(visitorFactory);
        final InetSocketAddress address = new InetSocketAddress(STUN_PORT);
        
        try
            {
            m_acceptor.bind(address, handler);
            }
        catch (final IOException e)
            {
            LOG.error("Could not bind!!", e);
            }
        }
    
    public void stop()
        {
        this.m_acceptor.unbindAll();
        }

    public void serviceActivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config)
        {
        // TODO Auto-generated method stub
        
        }

    public void serviceDeactivated(IoService service, SocketAddress serviceAddress, IoHandler handler, IoServiceConfig config)
        {
        // TODO Auto-generated method stub
        
        }

    public void sessionCreated(final IoSession session)
        {
        // TODO Auto-generated method stub
        
        }

    public void sessionDestroyed(final IoSession session)
        {
        // Key -- we need to tell the client manager the session with the
        // TURN client has closed.
        LOG.debug("TURN client disconnected: "+session);
        this.m_turnClientManager.removeBinding(session);
        }

    }
