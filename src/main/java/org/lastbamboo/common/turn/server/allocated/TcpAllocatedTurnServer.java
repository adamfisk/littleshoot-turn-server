package org.lastbamboo.common.turn.server.allocated;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.DefaultIoFilterChainBuilder;
import org.littleshoot.mina.common.IoHandler;
import org.littleshoot.mina.common.IoService;
import org.littleshoot.mina.common.IoServiceConfig;
import org.littleshoot.mina.common.IoServiceListener;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.common.SimpleByteBufferAllocator;
import org.littleshoot.mina.common.ThreadModel;
import org.littleshoot.mina.filter.executor.ExecutorFilter;
import org.littleshoot.mina.transport.socket.nio.SocketAcceptor;
import org.littleshoot.mina.transport.socket.nio.SocketAcceptorConfig;
import org.lastbamboo.common.turn.server.TurnClient;
import org.littleshoot.util.DaemonThreadFactory;
import org.littleshoot.util.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a TCP allocated TURN server.
 */
public class TcpAllocatedTurnServer implements AllocatedTurnServer, 
    IoServiceListener
    {

    private final Logger LOG = 
        LoggerFactory.getLogger(TcpAllocatedTurnServer.class);

    private final TurnClient m_turnClient;

    private final SocketAcceptor m_acceptor;

    private final InetAddress m_publicAddress;

    private InetSocketAddress m_serviceAddress;
    
    private final Executor m_threadPool = Executors.newCachedThreadPool(
        new DaemonThreadFactory("TCP-Allocated-TURN-Server-Thread-Pool"));
    
    /**
     * Creates a new TURN server allocated on behalf of a TURN client.  This
     * server will accept connections with permission to connect to the TURN
     * client and will relay data on the TURN client's behalf.
     * 
     * @param turnClient The TURN client.
     * @param publicAddress The address to bind to.
     */
    public TcpAllocatedTurnServer(final TurnClient turnClient, 
        final InetAddress publicAddress)
        {
        m_turnClient = turnClient;
        this.m_publicAddress = publicAddress;
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        
        this.m_acceptor = new SocketAcceptor(4, m_threadPool);
        m_acceptor.addListener(this);
        }

    public void start()
        {
        // Note there's no encoder here because we just write raw bytes to
        // remote hosts.  The TURN server is responsible for unwrapping the
        // raw bytes from the TURN Send Indication messages.
        
        // We're receiving raw data on these sockets and packaging it for 
        // our TURN client.  This filter just reads the raw data and
        // encapsulates it in TURN Data Indication messages.
        
        
        final SocketAcceptorConfig config = m_acceptor.getDefaultConfig();
        config.setThreadModel(ThreadModel.MANUAL);
        
        final DefaultIoFilterChainBuilder filterChainBuilder = 
            m_acceptor.getDefaultConfig().getFilterChain();
        
        filterChainBuilder.addLast("to-stun", new TurnRawDataFilter());
        filterChainBuilder.addLast("threadPool", 
            new ExecutorFilter(this.m_threadPool));
        
        //final ThreadModel threadModel = 
         //   ExecutorThreadModel.getInstance("TCP-TURN-Allocated-Server");
        //config.setThreadModel(threadModel);
        //m_acceptor.setDefaultConfig(config);
    
        // The IO handler just processes the Data Indication messages.
        final IoHandler handler = 
            new AllocatedTurnServerIoHandler(this.m_turnClient);
        
        try
            {
            final InetSocketAddress bindAddress = 
                new InetSocketAddress(NetworkUtils.getLocalHost(), 0);
            m_acceptor.bind(bindAddress, handler);
            LOG.debug("Started TCP allocated TURN server, bound to: "+
                bindAddress);
            }
        catch (final IOException e)
            {
            LOG.error("Could not bind server", e);
            }
        }

    public void stop()
        {
        this.m_acceptor.unbindAll();
        }
    
    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        final InetSocketAddress isa = (InetSocketAddress) serviceAddress;
        this.m_serviceAddress = 
            new InetSocketAddress(this.m_publicAddress, isa.getPort());
        LOG.debug("Allocated server started on: {}", serviceAddress);
        LOG.debug("Using public address: {}", this.m_serviceAddress);
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        LOG.debug("Deactivating allocated TURN server...");
        }

    public void sessionCreated(final IoSession session)
        {
        this.m_turnClient.addConnection(session);
        /*
        if (this.m_turnClient.hasIncomingPermission(session))
            {
            this.m_turnClient.addConnection(session);
            }
        else
            {
            session.close();
            }
            */
        }

    public void sessionDestroyed(final IoSession session)
        {
        LOG.debug("Lost connection to: {}", session);
        this.m_turnClient.removeConnection(session);
        }

    public InetSocketAddress getSocketAddress()
        {
        return this.m_serviceAddress;
        }
    }
