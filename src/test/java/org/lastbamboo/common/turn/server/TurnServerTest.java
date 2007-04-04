package org.lastbamboo.common.turn.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.id.uuid.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.turn.server.TurnServer;
import org.lastbamboo.shoot.turn.TurnConstants;
import org.lastbamboo.shoot.turn.message.AllocateRequest;
import org.lastbamboo.shoot.turn.message.SendRequest;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageTypes;
import org.lastbamboo.shoot.turn.message.attribute.StunAttributeTypes;
import org.lastbamboo.shoot.turn.message.attribute.TurnAttributeTypes;
import org.lastbamboo.util.Unsigned;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests the TURN servers response to all TURN requests.
 */
public final class TurnServerTest
    extends AbstractDependencyInjectionSpringContextTests
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(TurnServerTest.class);

    private static Socket s_turnClientSocket;

    private static TurnMessageFactory s_turnMessageFactory;

    private static final int TEST_SERVER_PORT = 57686;

    private static final String[] s_configLocations =
        new String[]
            {
            "turnStackBeans.xml",
            "turnServerBeans.xml"
            };

    private volatile boolean m_notified;

    private volatile boolean m_retrievedTestString;

    protected String[] getConfigLocations()
        {
        return s_configLocations;
        }

    public void onSetUp() throws Exception
        {
        LOG.trace("Setting up test...");
        this.m_notified = false;
        this.m_retrievedTestString = false;
        if (s_turnClientSocket != null)
            {
            return;
            }

        final TurnServer server =
            (TurnServer) applicationContext.getBean("turnServer");
        server.start();
        Thread.sleep(2000);
        s_turnClientSocket =
            new Socket(InetAddress.getLocalHost(),
                       TurnConstants.DEFAULT_SERVER_PORT);

        s_turnClientSocket.setSoTimeout(4000);

        s_turnMessageFactory =
            (TurnMessageFactory) applicationContext.getBean(
                "turnMessageFactory");
        }

    /**
     * Test the message cycle of allocate requests, send requests, their
     * associated responses, etc.
     * @throws Exception If any unexpected error occurs.
     */
    public void testGeneralMessages() throws Exception
        {
        final AllocateRequest request =
            s_turnMessageFactory.createAllocateRequest();
        Collection buffers = request.toByteBuffers();
        final OutputStream turnClientOutputStream =
            s_turnClientSocket.getOutputStream();
        for (final Iterator iter = buffers.iterator(); iter.hasNext();)
            {
            final ByteBuffer curBuffer = (ByteBuffer) iter.next();
            turnClientOutputStream.write(curBuffer.array());
            }
        turnClientOutputStream.flush();

        final InputStream is = s_turnClientSocket.getInputStream();

        final ByteBuffer responseBuffer = ByteBuffer.allocate(32);

        LOG.trace("*************** reading response...");
        while (responseBuffer.hasRemaining())
            {
            responseBuffer.put((byte) is.read());
            }
        responseBuffer.flip();
        short type = responseBuffer.getShort();
        assertEquals(type, TurnMessageTypes.ALLOCATE_RESPONSE);
        final short length = responseBuffer.getShort();
        assertEquals(12, length);

        final byte[] transactionIdBytes = new byte[16];
        responseBuffer.get(transactionIdBytes);
        final UUID transactionId = new UUID(transactionIdBytes);
        assertEquals(request.getTransactionId(), transactionId);
        final short attributeType = responseBuffer.getShort();
        assertEquals(StunAttributeTypes.MAPPED_ADDRESS, attributeType);

        final short attributeLength = responseBuffer.getShort();
        assertEquals(8, attributeLength);
        responseBuffer.getShort();
        final int port = responseBuffer.getShort() & 0xffff;

        final byte[] ip = new byte[4];
        responseBuffer.get(ip);
        final InetAddress address = InetAddress.getByAddress(ip);

        // Now make sure we're rejected from connecting to that address since we
        // have not sent a Send Request to it yet...
        LOG.trace("About to start socket for remote client***************");
        //final Socket remoteClientSocket = new Socket(address, port);
        //remoteClientSocket.setSoTimeout(6000);
        LOG.trace("Started socket....");
        //final InputStream readStream = remoteClientSocket.getInputStream();

        // Make sure the stream is dead.
        //assertEquals(-1, readStream.read());

        final InetSocketAddress localHost =
            new InetSocketAddress(InetAddress.getLocalHost(), TEST_SERVER_PORT);

        LOG.trace("**************************Running server...");
        runThreadedTestServer();
        Thread.sleep(1000);

        synchronized(this)
            {
            int count = 0;
            while (!this.m_notified && count < 3)
                {
                wait(3000);
                count++;
                }
            }

        assertTrue("Did not connect to remote host", this.m_notified);

        LOG.trace("**************************About to wait...");
        final ByteBuffer sendDataBuffer =
            ByteBuffer.allocate(TEST_STRING.length());
        sendDataBuffer.put(TEST_STRING.getBytes());
        sendDataBuffer.rewind();
        final SendRequest sendRequest =
            s_turnMessageFactory.createSendRequest(localHost, sendDataBuffer);
        LOG.trace("Sending send request with id: "+
            sendRequest.getTransactionId());

        buffers = sendRequest.toByteBuffers();

        for (final Iterator iter = buffers.iterator(); iter.hasNext();)
            {
            final ByteBuffer curBuffer = (ByteBuffer) iter.next();
            turnClientOutputStream.write(curBuffer.array());
            }
        turnClientOutputStream.flush();

        synchronized(this)
            {
            int count = 0;
            while (!this.m_retrievedTestString && count < 3)
                {
                wait(6000);
                count++;
                }
            }

        assertTrue(this.m_retrievedTestString);

        // Read the send response.
        verifySendResponse(is, sendRequest);

        // Make sure that data indication messages are sent appropriately.
        verifyDataIndication(is, address, port);
        }

    private void verifyDataIndication(final InputStream is,
        final InetAddress address, final int port) throws Exception
        {
        LOG.trace("Verifying Data Indication...\n\n\n");
        final Socket remoteClient = new Socket(address, port);
        remoteClient.setSoTimeout(4000);
        final OutputStream remoteClientStream = remoteClient.getOutputStream();
        final String helloClient = "hello client.  i love asia";

        LOG.trace("About to write data...");
        remoteClientStream.write(helloClient.getBytes());

        final ByteBuffer buffer =
            ByteBuffer.allocate(36 + helloClient.length());

        LOG.trace("About to read data....");
        while (buffer.hasRemaining())
            {
            buffer.put((byte) is.read());
            }

        buffer.rewind();
        final short type = buffer.getShort();
        assertEquals(TurnMessageTypes.DATA_INDICATION, type);
        final short length = buffer.getShort();
        final short expectedLength = (short)(4 + helloClient.length() + 12);
        assertEquals(expectedLength, length);

        // Just read the transaction ID even though it doesn't matter for
        // data indication messages.
        final byte[] transactionIdBytes = new byte[16];
        buffer.get(transactionIdBytes);

        LOG.trace("Reading "+length+" bytes...");
        LOG.trace("Bytes remaining: "+buffer.remaining());

        assertEquals(TurnAttributeTypes.REMOTE_ADDRESS, buffer.getShort());
        assertEquals(8, buffer.getShort());
        assertEquals(0x00, buffer.get());
        assertEquals(TurnConstants.ADDRESS_FAMILY, buffer.get());
        assertEquals(remoteClient.getLocalPort(),
            Unsigned.getUnsignedShort(buffer));

        final byte[] readIpBytes = new byte[4];
        buffer.get(readIpBytes);
        final InetAddress readAddress = InetAddress.getByAddress(readIpBytes);
        assertEquals(address, readAddress);

        assertEquals(TurnAttributeTypes.DATA, buffer.getShort());
        assertEquals(helloClient.length(), Unsigned.getUnsignedShort(buffer));
        final byte[] readBytes = new byte[helloClient.length()];
        buffer.get(readBytes);

        final String readString = new String(readBytes);
        LOG.trace("read: "+readString);
        assertEquals(helloClient, readString);

        }

    private void verifySendResponse(final InputStream is,
        final SendRequest request) throws Exception
        {
        LOG.trace("Verifying Send Response...\n\n\n");
        final ByteBuffer buffer = ByteBuffer.allocate(20);
        while (buffer.hasRemaining())
            {
            buffer.put((byte) is.read());
            }
        buffer.rewind();
        final short type = buffer.getShort();
        assertEquals(TurnMessageTypes.SEND_RESPONSE, type);
        final short length = buffer.getShort();
        assertEquals(0, length);

        final byte[] idBytes = new byte[16];
        buffer.get(idBytes);
        final UUID id = new UUID(idBytes);
        assertEquals(request.getTransactionId(), id);
        }

    private static final String TEST_STRING = "HTTP TEST";

    /**
     * This runs a local server that acts as the remote host for receiving
     * TURN Send Requests.
     *
     * @throws Exception If any unexpected error occurs.
     */
    private void runThreadedTestServer() throws Exception
        {
        final Thread serverThread = new Thread(new Runnable()
            {
            public void run()
                {
                try
                    {
                    runTestServer();
                    }
                catch (final Exception e)
                    {
                    TurnServerTest.fail("Unexpected exception: "+e);
                    }
                }
            });
        serverThread.setDaemon(true);
        serverThread.start();
        }

    private void runTestServer() throws Exception
        {
        LOG.trace("Running test server on port: "+TEST_SERVER_PORT);
        final ServerSocket server = new ServerSocket(TEST_SERVER_PORT);
        LOG.trace("About to notify....");
        synchronized (this)
            {
            LOG.trace("Notifying...");
            this.m_notified = true;
            notifyAll();
            }
        final Socket client = server.accept();
        LOG.trace("Received test socket...");
        client.setSoTimeout(6000);
        final InputStream is = client.getInputStream();
        final ByteBuffer buffer = ByteBuffer.allocate(TEST_STRING.length());
        while (buffer.hasRemaining())
            {
            LOG.trace("About to read...");
            buffer.put((byte) is.read());
            }

        buffer.rewind();
        final byte[] httpTestBytes = new byte[9];
        buffer.get(httpTestBytes);

        final String receivedString = new String(httpTestBytes);
        LOG.trace("Received string: "+receivedString);
        assertEquals(TEST_STRING, receivedString);

        this.m_retrievedTestString = true;
        synchronized (this)
            {
            notifyAll();
            }
        }
    }
