package org.lastbamboo.common.turn.server;


import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for processing TURN Data Indication messages that have
 * been created to wrap raw data received from remote hosts.
 */
public class AllocatedTurnServerIoHandler implements IoHandler
    {

    private final Logger LOG = 
        LoggerFactory.getLogger(AllocatedTurnServerIoHandler.class);
    
    private final TurnClient m_turnClient;

    /**
     * Creates a new IO handler for the specified TURN client.
     * 
     * @param client The client IO handler.
     */
    public AllocatedTurnServerIoHandler(final TurnClient client)
        {
        m_turnClient = client;
        }

    public void exceptionCaught(IoSession session, Throwable cause)
        throws Exception
        {
        // TODO Auto-generated method stub

        }

    public void messageReceived(final IoSession session, final Object message)
        throws Exception
        {
        LOG.debug("Received message on allocated server: {}", message);
        final DataIndication indication = (DataIndication) message;
        
        // If the client has set the active destination, just forward the data
        // along.
        if (this.m_turnClient.hasActiveDestination())
            {
            this.m_turnClient.getIoSession().write(indication);
            }
        
        // Otherwise, encapsulate the data in a "Data Indication" message.
        else
            {
            this.m_turnClient.getIoSession().write(indication);
            }
        }

    public void messageSent(IoSession session, Object message) throws Exception
        {
        // TODO Auto-generated method stub

        }

    public void sessionClosed(IoSession session) throws Exception
        {
        // TODO Auto-generated method stub

        }

    public void sessionCreated(IoSession session) throws Exception
        {
        // TODO Auto-generated method stub

        }

    public void sessionIdle(IoSession session, IdleStatus status)
        throws Exception
        {
        // TODO Auto-generated method stub

        }

    public void sessionOpened(IoSession session) throws Exception
        {
        // TODO Auto-generated method stub

        }

    }
