package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.lastbamboo.common.stun.stack.message.turn.AllocateSuccessResponse;
import org.lastbamboo.common.stun.stack.message.turn.ConnectRequest;
import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that responds to TURN requests from a single TURN client.  Each TURN 
 * client is allocated a unique responder for handling all requests.
 */
public final class TurnServerMessageVisitor 
    extends StunMessageVisitorAdapter<StunMessage>
    {
    
    /**
     * Logger for this class.
     */
    private static final Logger LOG = 
        LoggerFactory.getLogger(TurnServerMessageVisitor.class);
    private final TurnClientManager m_turnClientManager;
    private final IoSession m_ioSession;

    /**
     * Creates a new visitor that responds to incoming TURN client requests.
     * 
     * @param ioSession The reader/writer for sending and receiving TURN data
     * to and from a single client.
     * @param clientManager The client manager for allocating new bindings for 
     * the client.
     */
    public TurnServerMessageVisitor(final IoSession ioSession,
        final TurnClientManager clientManager)
        {
        this.m_ioSession = ioSession;
        this.m_turnClientManager = clientManager;
        }

    public StunMessage visitAllocateRequest(final AllocateRequest request)
        {
        LOG.debug("Processing allocate request...");
        
        // Note that the client here will frequently have already existed,
        // with the new allocate request simply serving to keep the binding
        // alive.
        final TurnClient client = this.m_turnClientManager.allocateBinding(
            this.m_ioSession);
        
        final InetSocketAddress relayAddress = client.getRelayAddress();
        final InetSocketAddress mappedAddress = client.getMappedAddress();
        
        final AllocateSuccessResponse response =
            new AllocateSuccessResponse(request.getTransactionId(), 
                relayAddress, mappedAddress); 

        this.m_ioSession.write(response);
        return null;
        }    

    public StunMessage visitSendIndication(final SendIndication indication)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Processing Send Indication with body length: "+
                indication.getBodyLength());
            }
        final InetSocketAddress remoteAddress = indication.getRemoteAddress();
        
        final byte[] data = indication.getData();
        
        final TurnClient client = 
            this.m_turnClientManager.getTurnClient(this.m_ioSession);
        
        // This is a non-blocking write to the remote host.
        client.write(remoteAddress, ByteBuffer.wrap(data));
        LOG.trace("Finished handling Send Indication...");
        return null;
        }

    public StunMessage visitConnectRequest(final ConnectRequest request)
        {
        LOG.debug("Processing connect request for: {}", 
            request.getRemoteAddress());
        final InetSocketAddress remoteAddress = request.getRemoteAddress();
        final TurnClient client = 
            this.m_turnClientManager.getTurnClient(this.m_ioSession);
        client.handleConnect(remoteAddress);
        return null;
        }
    }
