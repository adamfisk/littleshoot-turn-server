package org.lastbamboo.shoot.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.shoot.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.message.AllocateRequest;
import org.lastbamboo.shoot.turn.message.AllocateResponse;
import org.lastbamboo.shoot.turn.message.DataIndication;
import org.lastbamboo.shoot.turn.message.SendErrorResponse;
import org.lastbamboo.shoot.turn.message.SendRequest;
import org.lastbamboo.shoot.turn.message.SendResponse;
import org.lastbamboo.shoot.turn.message.TurnMessage;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageVisitor;
import org.lastbamboo.shoot.turn.message.attribute.SendErrorResponseCodeImpl;

/**
 * Class that responds to TURN requests from a single TURN client.  Each TURN 
 * client is allocated a unique responder for handling all requests.
 */
public final class TurnServerResponder implements TurnMessageVisitor
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(TurnServerResponder.class);
    private final TurnMessageFactory m_turnMessageFactory;
    private final TurnClientManager m_turnClientManager;
    private final ReaderWriter m_readerWriter;

    /**
     * Creates a new responder that is simply responsible for responding to 
     * incoming TURN client requests.
     * @param readerWriter The reader/writer for sending and receiving TURN data
     * to and from a single client.
     * @param messageFactory The factory for creating TURN responses.
     * @param clientManager The client manager for allocating new bindings for 
     * the client.
     */
    public TurnServerResponder(final ReaderWriter readerWriter,
        final TurnMessageFactory messageFactory,
        final TurnClientManager clientManager)
        {
        this.m_readerWriter = readerWriter;
        this.m_turnMessageFactory = messageFactory;
        this.m_turnClientManager = clientManager;
        }

    public void visitAllocateRequest(final AllocateRequest request)
        {
        LOG.trace("Processing allocate request...");
        
        // TODO: Use the decorator pattern here to first validate the request...
        final TurnClient client = this.m_turnClientManager.allocateBinding(
            this.m_readerWriter);
        
        final InetSocketAddress address = client.getAllocatedSocketAddress();
        
        LOG.trace("Sending MAPPED-ADDRESS with port: "+address.getPort());
        final TurnMessage response = 
            this.m_turnMessageFactory.createAllocateResponse(
                request.getTransactionId(), address);

        writeResponse(response);
        }

    public void visitSendRequest(final SendRequest request)
        {
        LOG.trace("Processing send request: "+request);
        final InetSocketAddress destinationAddress = 
            request.getDestinationAddress();
        
        final ByteBuffer data = request.getData();
        
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Forwarding "+data.remaining()+" bytes to client: "+
                destinationAddress);
            LOG.debug("Accessing TurnClient for: "+this.m_readerWriter);
            LOG.debug("Request ID: "+request.getTransactionId());
            }
        
        final TurnClient client = 
            this.m_turnClientManager.getTurnClient(this.m_readerWriter);
        
        // This is a non-blocking write to the remote host.
        try
            {
            client.write(destinationAddress, data);
            
            // We currently write a successful send response even if we did
            // not have a previous connection to the destination address and
            // do not know here whether or not we successfully connected and
            // transmitted the data.  The alternative would be to wait for the
            // connection and to then return a Send Error Response, but that
            // would hang up all message processing.
            writeSuccessfulSendResponse(request);
            }
        catch (final IOException e)
            {
            // We could not write the data to the client.  We should remove
            // the client from the list of valid clients for this host.
            LOG.warn("Could not write to remote host: "+destinationAddress, e);
            writeSendErrorResponse(request);
            }
        LOG.trace("Finished handling Send Request...");
        }
   
    
    private void writeSendErrorResponse(final SendRequest request)
        {
        LOG.trace("Writing send error response to: "+this.m_readerWriter);
        final TurnMessage response = 
            this.m_turnMessageFactory.createSendErrorResponse(
                request.getTransactionId(), 
                    SendErrorResponseCodeImpl.SEND_FAILED);        
        writeResponse(response);
        }

    private void writeSuccessfulSendResponse(final TurnMessage request)
        {
        LOG.trace("Writing successful send response to: "+this.m_readerWriter);
        final TurnMessage response = 
            this.m_turnMessageFactory.createSendResponse(
                request.getTransactionId());        
        writeResponse(response);
        }

    public void visitAllocateResponse(final AllocateResponse response)
        {
        LOG.error("Server should not receive allocate responses...");
        }

    public void visitSendResponse(final SendResponse response)
        {
        LOG.error("Server should not receive send responses...");
        }
    
    public void visitSendErrorResponse(SendErrorResponse response)
        {
        LOG.error("Server should not receive send error responses...");
        }

    public void visitDataIndication(final DataIndication dataIndication)
        {
        LOG.error("Server should not receive data indication messages...");
        }
    
    /**
     * Writes the specified response back to the client, cleaning up 
     * appropriately in case the client has disappeared.
     * @param response The response to write to the client, such as a 
     * Send Response or an Allocate Response.
     */
    private void writeResponse(final TurnMessage response)
        {
        LOG.trace("Writing response...");
        try
            {
            this.m_readerWriter.write(response.toByteBuffers());
            }
        catch (final IOException e)
            {
            LOG.debug("Could not write", e);
            removeClient();
            }
        }
    
    /**
     * Removes this client from the group of active clients.  This is called
     * when we're unable to write to the client, for example.
     */
    private void removeClient()
        {
        LOG.trace("Removing client: "+this.m_readerWriter);
        this.m_turnClientManager.removeBinding(this.m_readerWriter);
        }


    }
