package org.lastbamboo.common.turn.server;

import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * Factory fore creating message visitors for TURN servers.
 */
public class TurnServerMessageVisitorFactory implements
    StunMessageVisitorFactory
    {
    
    private final TurnClientManager m_turnClientManager;

    /**
     * Creates a new visitor that responds to incoming TURN client requests.
     * 
     * @param clientManager The client manager for allocating new bindings for 
     * the client.
     */
    public TurnServerMessageVisitorFactory(
        final TurnClientManager clientManager)
        {
        this.m_turnClientManager = clientManager;
        }

    public StunMessageVisitor createVisitor(final IoSession session)
        {
        return new TurnServerMessageVisitor(session, this.m_turnClientManager);
        }

    public StunMessageVisitor createVisitor(IoSession session, Object attachment)
        {
        return createVisitor(session);
        }

    }
