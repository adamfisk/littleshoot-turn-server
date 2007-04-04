package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.TurnMessageTypes;
import org.lastbamboo.common.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.common.turn.message.handler.TurnMessageHandler;
import org.lastbamboo.common.turn.message.handler.TurnMessageHandlerFactory;
import org.lastbamboo.common.turn.message.handler.UnknownMessageTypeHandler;

/**
 * Implementation of a TURN requests handler factory for creating TURN
 * request handlers for specific request types.
 */
public final class ServerTurnMessageHandlerFactory 
    implements TurnMessageHandlerFactory
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(ServerTurnMessageHandlerFactory.class);
    
    private TurnMessageFactory m_turnMessageFactory;

    private TurnAttributesReader m_attributesReader;
    
    /**
     * Sets the factory to use for creating TURN messages.
     * @param factory The factory to use for creating TURN messages.
     */
    public void setTurnMessageFactory(final TurnMessageFactory factory)
        {
        this.m_turnMessageFactory = factory;
        }
    
    public void setAttributesReader(final TurnAttributesReader reader)
        {
        this.m_attributesReader = reader;
        }
    
    public TurnMessageHandler createTurnMessageHandler(final int type, 
        final InetSocketAddress turnClient)
        {
        switch (type)
            {
            case TurnMessageTypes.ALLOCATE_REQUEST:
                LOG.trace("Recevied ALLOCATE REQUEST!!!");
                return new AllocateRequestHandler(turnClient, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            case TurnMessageTypes.SEND_REQUEST:
                LOG.trace("Received SEND REQUEST!!!");
                return new SendRequestHandler(turnClient, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            default:
                LOG.warn("Received UNKNOWN MESSAGE TYPE: "+getTurnType(type)+
                    " from TURN client: "+turnClient);
                return new UnknownMessageTypeHandler(type, turnClient, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            }
        }

    private static String getTurnType(final int type)
        {
        switch (type)
            {
            case TurnMessageTypes.ALLOCATE_ERROR_RESPONSE:
                return "ALLOCATE ERROR RESPONSE";
            case TurnMessageTypes.ALLOCATE_REQUEST:
                return "ALLOCATE REQUEST";
            case TurnMessageTypes.ALLOCATE_RESPONSE:
                return "ALLOCATE RESPONSE";
            case TurnMessageTypes.DATA_INDICATION:
                return "DATA INDICATION";
            case TurnMessageTypes.SEND_ERROR_RESPONSE:
                return "SEND ERROR RESPONSE";
            case TurnMessageTypes.SEND_REQUEST:
                return "SEND REQUEST";
            case TurnMessageTypes.SEND_RESPONSE:
                return "SEND RESPONSE";
            default:
                return "Still could not determine type of: "+type;
            }
        }
    }
