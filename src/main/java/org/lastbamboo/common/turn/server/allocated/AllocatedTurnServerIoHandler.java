package org.lastbamboo.common.turn.server.allocated;


import org.littleshoot.mina.common.IdleStatus;
import org.littleshoot.mina.common.IoHandler;
import org.littleshoot.mina.common.IoHandlerAdapter;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.turn.server.TurnClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for processing TURN Data Indication messages that have
 * been created to wrap raw data received from remote hosts.
 */
public class AllocatedTurnServerIoHandler extends IoHandlerAdapter
    {

    private final Logger m_log = 
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

    @Override
    public void messageReceived(final IoSession session, final Object message)
        {
        m_log.debug("Received message on allocated server: {}", message);
        m_log.debug("Now sent "+session.getWrittenMessages());
        final DataIndication indication = (DataIndication) message;
        this.m_turnClient.getIoSession().write(indication);
        }
    
    @Override
    public void exceptionCaught(final IoSession session, final Throwable cause)
        {
        m_log.warn("Exception on allocated TURN IoHandler", cause);
        session.close();
        }

    @Override
    public void sessionCreated(final IoSession session) throws Exception
        {
        SessionUtil.initialize(session);
        
        // The idle time is in seconds.  If there's been no traffic in either
        // direction for awhile, we free the connection to limit load on the
        // server.
        //session.setIdleTime(IdleStatus.BOTH_IDLE, 300);
        }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status)
        {
        // Kill idle sessions.
        m_log.debug("Got idle TURN session");
        //session.close();
        }
    }
