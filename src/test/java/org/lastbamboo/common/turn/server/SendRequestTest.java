package org.lastbamboo.common.turn.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.turn.TurnProtocolHandler;
import org.lastbamboo.common.turn.message.AllocateRequest;
import org.lastbamboo.common.turn.message.AllocateResponse;
import org.lastbamboo.common.turn.message.DataIndication;
import org.lastbamboo.common.turn.message.SendErrorResponse;
import org.lastbamboo.common.turn.message.SendRequest;
import org.lastbamboo.common.turn.message.SendResponse;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.TurnMessageVisitor;
import org.lastbamboo.common.turn.message.handler.TurnMessageHandlerFactory;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Test for the TURN "Send Request" message.
 */
public final class SendRequestTest
    extends AbstractDependencyInjectionSpringContextTests
    implements TurnMessageVisitor
    {

    /**
     * Logger for the test.
     */
    private static final Log LOG = LogFactory.getLog(SendRequestTest.class);

    private TurnMessageHandlerFactory m_handlerFactory;

    private String m_testString = "MELLOW";

    private ByteBuffer m_expectedDataBuffer =
        ByteBuffer.allocate(m_testString.length());

    private TurnMessageFactory m_messageFactory;

    private SendRequest m_sendRequest;

    private boolean m_visitSendRequestCalled;

    protected String[] getConfigLocations()
        {
        return new String[] {"turnStackBeans.xml",
            "turnServerBeans.xml"};
        }

    protected void onSetUp() throws Exception
        {
        this.m_testString = "MELLOW";
        this.m_visitSendRequestCalled = false;
        this.m_expectedDataBuffer.put(m_testString.getBytes());
        this.m_expectedDataBuffer.rewind();

        this.m_handlerFactory =
            (TurnMessageHandlerFactory) applicationContext.getBean(
                "turnServerMessageHandlerFactory");

        this.m_messageFactory =
            (TurnMessageFactory) applicationContext.getBean(
                "turnMessageFactory");

        this.m_sendRequest = null;
        }

    /**
     * Test to make sure we can still read Send Requests correctly when only
     * some of the data resides in the original buffer.
     *
     * @throws Exception If any unexpected error occurs.
     */
    public void testPartialSendRequest() throws Exception
        {
        final int dataSize = 4000;
        final int headersSize = 36;
        final ByteBuffer data = ByteBuffer.allocate(dataSize);
        assertEquals(4000, data.remaining());
        for (int i=0; i < 1000; i++)
            {
            data.putInt(i);
            }
        assertFalse(data.hasRemaining());
        data.rewind();

        final InetSocketAddress destinationAddress =
            new InetSocketAddress(InetAddress.getLocalHost(), 43234);

        final SendRequest request =
            this.m_messageFactory.createSendRequest(destinationAddress, data);

        // The length should be the total size minus the 20 byte message header
        // (the attribute headers are included in the length).
        assertEquals(4016, request.getLength());

        assertEquals(dataSize, request.getData().remaining());

        final ByteBuffer toRead = ByteBuffer.allocate(dataSize+headersSize);

        final Collection buffers = request.toByteBuffers();
        for (final Iterator iter = buffers.iterator(); iter.hasNext();)
            {
            final ByteBuffer buf = (ByteBuffer) iter.next();
            toRead.put(buf);
            }
        assertFalse(toRead.hasRemaining());
        toRead.rewind();

        final ByteBuffer firstBuffer = ByteBuffer.allocate(3000+headersSize);
        final ByteBuffer secondBuffer = ByteBuffer.allocate(1000);

        while (firstBuffer.hasRemaining())
            {
            firstBuffer.put(toRead.get());
            }
        while (secondBuffer.hasRemaining())
            {
            secondBuffer.put(toRead.get());
            }

        assertEquals(0, toRead.remaining());


        this.m_testString = new String(data.duplicate().array());
        this.m_expectedDataBuffer = data.duplicate();
        this.m_expectedDataBuffer.rewind();
        this.m_visitSendRequestCalled = false;

        final ProtocolHandler handler =
            new TurnProtocolHandler(this.m_handlerFactory, this);

        final ByteBuffer readBuffer = ByteBuffer.allocateDirect(10000);
        firstBuffer.rewind();
        readBuffer.put(firstBuffer);

        // Note the address doesn't matter here.
        handler.handleMessages(readBuffer, destinationAddress);
        assertFalse(this.m_visitSendRequestCalled);

        readBuffer.clear();
        secondBuffer.rewind();
        LOG.trace("Remainging in main buffer: "+readBuffer.remaining());
        readBuffer.put(secondBuffer);
        handler.handleMessages(readBuffer, destinationAddress);
        assertTrue("Expected send request", this.m_visitSendRequestCalled);
        }

    /**
     * This tests Send Requests where the data in the request is a wrapped
     * byte array byte buffer with an offset.  This makes sure the send request
     * preserves the offset in the data.
     * @throws Exception If any unexpected error occurs.
     */
    public void testWrappedPartialDataArray() throws Exception
        {
        final String numbers = "0123456789";

        ByteBuffer buffer = ByteBuffer.wrap(numbers.getBytes(), 8, 2);

        assertEquals(2, buffer.remaining());
        final InetSocketAddress destinationAddress =
            new InetSocketAddress(InetAddress.getLocalHost(), 39485);

        SendRequest request =
            this.m_messageFactory.createSendRequest(destinationAddress, buffer);

        final ByteBuffer data = request.getData();

        assertEquals(2, data.remaining());
        final byte[] dataBytes = new byte[data.remaining()];
        data.get(dataBytes);

        assertEquals("Should have received: "+numbers.substring(8)+
            " but was: "+new String(dataBytes), numbers.substring(8),
            new String(dataBytes));


        buffer = ByteBuffer.wrap(numbers.getBytes(), 8, 2);
        assertEquals(2, buffer.remaining());
        this.m_sendRequest =
            this.m_messageFactory.createSendRequest(destinationAddress, buffer);
        assertEquals(2, this.m_sendRequest.getData().remaining());

        int totalRemaining = 0;
        final Collection sendRequestBuffers =
            this.m_sendRequest.toByteBuffers();

        for (Iterator iter = sendRequestBuffers.iterator(); iter.hasNext();)
            {
            final ByteBuffer curBuf = (ByteBuffer) iter.next();
            LOG.trace("Remaining: "+curBuf.remaining());
            totalRemaining += curBuf.remaining();
            }

        // The length of the message, including:
        // 1) 20 byte mesage header.
        // 2) 12 bytes for the DESTINATION-ADDRESS attribute, including 2 bytes
        //    for the attribute type, 2 bytes for the attribute length,
        //    2 bytes for the tranport type, 2 bytes for the port, and 4 bytes
        //    for the IP address.
        // 3) 6 bytes for the DATA attribute, inlcuding 2 bytes for the
        //    attribute type, 2 bytes for the attribute length, and 2 bytes
        //    for the raw data.
        assertEquals("Unexpected total message length", 38, totalRemaining);

        final ByteBuffer outBuf = combineBuffers(sendRequestBuffers);
        this.m_expectedDataBuffer = ByteBuffer.allocate(2);
        this.m_expectedDataBuffer.put("89".getBytes());
        this.m_expectedDataBuffer.rewind();

        final ProtocolHandler handler =
            new TurnProtocolHandler(this.m_handlerFactory, this);

        this.m_testString = numbers.substring(8);
        this.m_visitSendRequestCalled = false;
        handler.handleMessages(outBuf, destinationAddress);

        assertTrue(this.m_visitSendRequestCalled);
        }

    public void testSendRequest() throws Exception
        {
        final InetSocketAddress clientAddress =
            new InetSocketAddress(InetAddress.getLocalHost(), 39485);
        final byte[] testStringBytes = m_testString.getBytes();
        final ByteBuffer data = ByteBuffer.allocate(testStringBytes.length);
        data.put(testStringBytes);
        data.rewind();
        this.m_sendRequest =
            this.m_messageFactory.createSendRequest(clientAddress, data);

        LOG.debug("*********Send Request data: "+this.m_sendRequest.getData());
        final Collection buffers = this.m_sendRequest.toByteBuffers();

        final ByteBuffer toProcess = combineBuffers(buffers);

        final ProtocolHandler handler =
            new TurnProtocolHandler(this.m_handlerFactory, this);

        this.m_visitSendRequestCalled = false;
        handler.handleMessages(toProcess, clientAddress);
        assertTrue(this.m_visitSendRequestCalled);
        }

    private ByteBuffer combineBuffers(final Collection buffers)
        {

        final ByteBuffer combined = ByteBuffer.allocate(10000);

        for (final Iterator iter = buffers.iterator(); iter.hasNext();)
            {
            final ByteBuffer buffer = (ByteBuffer) iter.next();
            LOG.trace("Adding: "+buffer.remaining()+" bytes...");
            combined.put(buffer);
            }

        return combined;
        }

    public void visitAllocateResponse(AllocateResponse response)
        {
        // TODO Auto-generated method stub

        }

    public void visitSendRequest(final SendRequest request)
        {
        LOG.trace("Received send request: "+request);
        this.m_visitSendRequestCalled = true;
        final ByteBuffer data = request.getData();
        final ByteBuffer duplicate = data.duplicate();
        assertEquals(m_expectedDataBuffer, duplicate);

        final byte[] dataBytes = new byte[duplicate.capacity()];
        duplicate.get(dataBytes);
        assertEquals(m_testString, new String(dataBytes));
        }

    public void visitAllocateRequest(AllocateRequest request)
        {
        // TODO Auto-generated method stub

        }

    public void visitSendResponse(SendResponse response)
        {
        // TODO Auto-generated method stub

        }

    public void visitDataIndication(DataIndication dataIndication)
        {
        // TODO Auto-generated method stub

        }

    public void visitSendErrorResponse(SendErrorResponse response)
        {
        }
    }
