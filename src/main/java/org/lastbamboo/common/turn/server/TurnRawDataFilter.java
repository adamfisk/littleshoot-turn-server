package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder that reads in raw data from remote hosts and forwards it to the 
 * TURN client.  The data will get wrapped in a Send Indication message
 * unless there's an active destination.
 */
public class TurnRawDataFilter extends IoFilterAdapter
    {
    
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    
    /**
     * This is the limit on the length of the data to encapsulate in a Send
     * Request.  TURN messages cannot be larger than 0xffff, so this leaves 
     * room for other attributes in the message as well as for headers.
     */
    private static final int LENGTH_LIMIT = 0xffff - 1000;
    
    public void messageReceived(
        final NextFilter nextFilter, final IoSession session, 
        final Object message) throws Exception
        {
        LOG.debug("Received raw data...");
        final InetSocketAddress remoteHost = 
            (InetSocketAddress) session.getRemoteAddress();
        
        final ByteBuffer in = (ByteBuffer) message;

        // Send the data broken up into chunks if necessary.  This is because
        // TURN messages cannot be larger than 0xffff.
        sendSplitBuffers(remoteHost, in, session, nextFilter);
        }
    
    /**
     * Splits the main read buffer into smaller buffers that will fit in
     * TURN messages.
     * 
     * @param remoteHost The host the data came from.
     * @param buffer The main read buffer to split.
     * @param session 
     * @param nextFilter The output of the decoder.
     */
    private void sendSplitBuffers(
        final InetSocketAddress remoteHost, final ByteBuffer buffer, 
        final IoSession session, final NextFilter nextFilter)
        {
        // Break up the data into smaller chunks.
        final Collection<byte[]> buffers = 
            MinaUtils.splitToByteArrays(buffer, LENGTH_LIMIT);
        for (final Iterator iter = buffers.iterator(); iter.hasNext();)
            {
            final byte[] data = (byte[]) iter.next();
            final DataIndication indication =
                new DataIndication(remoteHost, data);
            nextFilter.messageReceived(session, indication);
            }
        }
    }
