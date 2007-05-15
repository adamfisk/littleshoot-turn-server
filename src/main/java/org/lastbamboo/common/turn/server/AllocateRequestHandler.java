package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.common.turn.message.handler.AbstractTurnMessageHandler;

/**
 * Request handler for processing TURN allocate requests -- requests for new
 * IP/port mappings for TURN clients.
 */
public final class AllocateRequestHandler extends AbstractTurnMessageHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(AllocateRequestHandler.class);
    
    /**
     * Creates a new request handler with the specified client binding for 
     * creating a new mapping to an external address.
     * @param inetSocketAddress The address of the requesting client.
     * @param factory The factory for creating TURN messages.  
     * @param attributesReader Reader for TURN attributes.
     */
    public AllocateRequestHandler(final InetSocketAddress inetSocketAddress,
        final TurnMessageFactory factory,  
        final TurnAttributesReader attributesReader)
        {
        super(inetSocketAddress, factory, attributesReader);
        }

    public void handleMessage()
        {
        if (LOG.isTraceEnabled())
            {
            LOG.trace("Handling request...");
            }
        this.m_turnMessage = 
            this.m_turnMessageFactory.createAllocateRequest(
                this.m_transctionId);
        }
    }
