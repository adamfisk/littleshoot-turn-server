package org.lastbamboo.common.turn.server;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.StunIoHandler;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for TURN STUN servers.
 */
public class TurnServerIoHandler extends StunIoHandler
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
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
        
        // We consider a connection to be idle if there's been no traffic
        // in either direction for awhile.  Note that the client should be
        // periodically updating the connection with allocate requests, but
        // those might not come for awhile if there's a large file transfer,
        // for example, so we respect traffic from either direction.
        session.setIdleTime(IdleStatus.BOTH_IDLE, 60 * 10);
        }

    public void sessionIdle(final IoSession session, final IdleStatus status) 
        throws Exception
        {
        // Note closing the session here will create the appropriate event
        // handlers to clean up all mappings and references. We close idle
        // sessions because properly implemented clients should be sending
        // keep alive messages.
        session.close();
        }
    }
