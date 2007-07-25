package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;

import org.apache.commons.lang.ObjectUtils;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.NullStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.SuccessfulBindingResponse;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.lastbamboo.common.stun.stack.message.turn.ConnectRequest;
import org.lastbamboo.common.stun.stack.message.turn.ConnectionStatusIndication;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.lastbamboo.common.stun.stack.message.turn.SuccessfulAllocateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that responds to TURN requests from a single TURN client.  Each TURN 
 * client is allocated a unique responder for handling all requests.
 */
public final class TurnServerMessageVisitor implements StunMessageVisitor
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

    public Object visitAllocateRequest(final AllocateRequest request)
        {
        LOG.debug("Processing allocate request...");
        
        // Note that the client here will frequently have already existed,
        // with the new allocate request simply serving to keep the binding
        // alive.
        final TurnClient client = this.m_turnClientManager.allocateBinding(
            this.m_ioSession);
        
        final InetSocketAddress relayAddress = client.getRelayAddress();
        final InetSocketAddress mappedAddress = client.getMappedAddress();
        
        final SuccessfulAllocateResponse response =
            new SuccessfulAllocateResponse(request.getTransactionId(), 
                relayAddress, mappedAddress); 

        this.m_ioSession.write(response);
        return ObjectUtils.NULL;
        }    

    public Object visitBindingRequest(final BindingRequest request)
        {
        return ObjectUtils.NULL;
        }

    public Object visitSendIndication(final SendIndication indication)
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
        return ObjectUtils.NULL;
        }

    public Object visitConnectRequest(final ConnectRequest request)
        {
        LOG.debug("Processing connect request for: {}", 
            request.getRemoteAddress());
        final InetSocketAddress remoteAddress = request.getRemoteAddress();
        final TurnClient client = 
            this.m_turnClientManager.getTurnClient(this.m_ioSession);
        client.handleConnect(remoteAddress);
        return ObjectUtils.NULL;
        }
    
    public Object visitDataIndication(final DataIndication dataIndication)
        {
        LOG.error("Server should not receive data indication messages...");
        return ObjectUtils.NULL;
        }

    public Object visitSuccessfulAllocateResponse(
        final SuccessfulAllocateResponse response)
        {
        LOG.error("The server should not get allocate response messages: " + 
           response);
        return ObjectUtils.NULL;
        }

    public Object visitSuccessfulBindingResponse(
        final SuccessfulBindingResponse response)
        {
        LOG.error("The server should not get binding response messages: " + 
            response);
        return ObjectUtils.NULL;
        }

    public Object visitConnectionStatusIndication(
        final ConnectionStatusIndication indication)
        {
        LOG.error("The server should not get connect status indications: " + 
            indication);
        return ObjectUtils.NULL;
        }

    public Object visitNullMessage(final NullStunMessage message)
        {
        LOG.error("Null message on server!!");
        return ObjectUtils.NULL;
        }
    }
