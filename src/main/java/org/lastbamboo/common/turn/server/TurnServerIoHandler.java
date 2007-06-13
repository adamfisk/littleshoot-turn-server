package org.lastbamboo.common.turn.server;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.AbstractStunIoHandler;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * {@link IoHandler} for STUN servers.
 */
public class TurnServerIoHandler extends AbstractStunIoHandler
    {

    /**
     * Creates a new server IO handler.
     * 
     * @param factory The STUN server message visitor for processing read
     * messages. 
     */
    public TurnServerIoHandler(final StunMessageVisitorFactory factory)
        {
        super(factory);
        }
    
    public void sessionCreated(final IoSession session) throws Exception
        {
        SessionUtil.initialize(session);
        
        // If we haven't heard from the TURN client in awhile, we close the
        // connection.  Note this is in seconds.
        session.setIdleTime(IdleStatus.READER_IDLE, 60 * 10);
        }

    public void sessionIdle(final IoSession session, final IdleStatus status) 
        throws Exception
        {
        // Note closing the session here will create the appropriate event
        // handlers to clean up all mappings and references.
        session.close();
        }

    }
