package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.attribute.DataAttribute;
import org.lastbamboo.common.turn.message.attribute.DataAttributeImpl;
import org.lastbamboo.common.turn.message.attribute.InetSocketAddressTurnAttribute;
import org.lastbamboo.common.turn.message.attribute.TurnAttributeTypes;
import org.lastbamboo.common.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.common.turn.message.handler.AbstractTurnMessageHandler;

/**
 * Handles TURN send messages from clients, opening up the appropriate 
 * permissions.
 */
public final class SendRequestHandler extends AbstractTurnMessageHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(SendRequestHandler.class);
    
    /**
     * Creates a new request handler with the specified client binding for 
     * opening a new connection to an external address and for allowing 
     * incoming data from that address.
     * 
     * @param client The address and port of the requesting client.
     * @param turnMessageFactory The factory for creating TURN messages. 
     * @param attributesReader The reader for reading TURN attributes.
     * @param readerWriter The reader and writer for sending and receiving data
     * to and from the TURN client.
     */
    public SendRequestHandler(final InetSocketAddress client, 
        final TurnMessageFactory turnMessageFactory, 
        final TurnAttributesReader attributesReader)
        {
        super(client, turnMessageFactory, attributesReader);
        }

    public void handleMessage() throws IOException
        {
        final InetSocketAddressTurnAttribute destinationAddress =
            (InetSocketAddressTurnAttribute) this.m_attributes.get(
                new Integer(TurnAttributeTypes.DESTINATION_ADDRESS));
    
        if (destinationAddress == null)
            {
            throw new IOException("Could not read message -- " +
                "no DESTINATION-ADDRESS");
            }       
        
        final DataAttribute data =
            (DataAttributeImpl) this.m_attributes.get(
                new Integer(TurnAttributeTypes.DATA));
        
        LOG.trace("Retrieved attribute: "+data);
        if (data == null)
            {
            throw new IOException("Could not read data");
            }
        
        this.m_turnMessage = 
            this.m_turnMessageFactory.createSendRequest(this.m_transctionId, 
                destinationAddress, data);   
        }

    }
