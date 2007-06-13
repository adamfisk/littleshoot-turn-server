package org.lastbamboo.common.turn.server;

import org.apache.mina.common.IoHandler;
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

    }
