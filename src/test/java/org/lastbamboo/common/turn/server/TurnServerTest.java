package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

import junit.framework.TestCase;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.littleshoot.mina.common.ByteBuffer;
import org.lastbamboo.common.amazon.ec2.AmazonEc2Utils;
import org.lastbamboo.common.stun.stack.encoder.StunMessageEncoder;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageType;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttribute;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributeType;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributesFactory;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributesFactoryImpl;
import org.lastbamboo.common.stun.stack.message.attributes.turn.DataAttribute;
import org.lastbamboo.common.stun.stack.message.attributes.turn.RelayAddressAttribute;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.littleshoot.util.NetworkUtils;
import org.littleshoot.util.mina.MinaUtils;

/**
 * Tests the TURN servers response to all TURN requests.
 */
public final class TurnServerTest extends TestCase
    {

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TurnServerTest.class);

    private Socket m_turnClientSocket;

    private TurnServer m_server;

    protected void setUp() throws Exception
        {
        if (m_turnClientSocket != null && m_server != null)
            {
            return;
            }
        
        m_server = new TcpTurnServer();
        m_server.start();
        Thread.sleep(2000);
        m_turnClientSocket = new Socket(NetworkUtils.getLocalHost(), 3478);
        m_turnClientSocket.setSoTimeout(3000);
        }
    
    protected void tearDown() throws Exception
        {
        m_turnClientSocket.close();
        m_server.stop();
        }

    /**
     * Test the message cycle of allocate requests, send requests, their
     * associated responses, etc.
     * @throws Exception If any unexpected error occurs.
     */
    public void testGeneralMessages() throws Exception
        {
        final AllocateRequest allocateRequest = new AllocateRequest();
        
        write(m_turnClientSocket, allocateRequest);
        
        final Map<StunAttributeType, StunAttribute> allocateResponseAttributes = 
            readMessage(m_turnClientSocket, 
                StunMessageType.ALLOCATE_SUCCESS_RESPONSE,
                StunAttributeType.MAPPED_ADDRESS, 8);
        final RelayAddressAttribute ma = (RelayAddressAttribute) allocateResponseAttributes.get(
            StunAttributeType.RELAY_ADDRESS);
        // Vista seems to have an issue with connecting to local network 
        // addresses, so we use straight localhost instead and just use the 
        // allocated port.
        
        // We also need to do the workaround below because EC2 doesn't support
        // hairpinning.
        final InetSocketAddress relaySocketAddress;
        if (AmazonEc2Utils.onEc2())
            {
            relaySocketAddress =
                new InetSocketAddress(NetworkUtils.getLocalHost(), 
                    ma.getInetSocketAddress().getPort());
            }
        else
            {
            relaySocketAddress = ma.getInetSocketAddress();
            }
        // Done reading the allocate response.  We'll use this to connect to
        // the TURN client.
        

        // Now make sure we're rejected from connecting to that address since we
        // have not sent a Connect Request to it yet...
        LOG.trace("About to start socket for remote client");
        final Socket remoteHostSocket = new Socket();
        
        LOG.trace("Started socket....");
        
        // We create a random port here because the operating system keeps 
        // ports bound for a bit even after the socket is closed, leading to
        // bind failures if the test is run in rapid succession.  This makes
        // that extremely unlikely.
        final int port = 1024 + (RandomUtils.nextInt() % 5000);
        remoteHostSocket.bind(
            new InetSocketAddress(NetworkUtils.getLocalHost(), port));

        final InetSocketAddress remoteHostAddress = 
            (InetSocketAddress) remoteHostSocket.getLocalSocketAddress();
        LOG.debug("Bound to: "+remoteHostAddress);

        LOG.debug("Connecting to: "+relaySocketAddress.getAddress());
        assertTrue("Could not reach address: "+relaySocketAddress.getAddress(), 
                relaySocketAddress.getAddress().isReachable(4000));
        
        try
            {
            remoteHostSocket.connect(relaySocketAddress, 6000);
            }
        catch (final IOException e) 
            {
            LOG.debug("Could not connect to: "+relaySocketAddress, e);
            fail("could not connect to: "+relaySocketAddress+" "+ 
                e.getMessage());
            }
        assertTrue("Socket not connected", remoteHostSocket.isConnected());
        assertTrue("Socket not bound", remoteHostSocket.isBound());
        LOG.debug("Remote host connected successfully!");
        
        // The server again sends a connect status because the client is now
        // connected!!
        Map<StunAttributeType, StunAttribute> messageAttributes = 
            readMessage(m_turnClientSocket, 
            StunMessageType.CONNECTION_STATUS_INDICATION,
            StunAttributeType.CONNECT_STAT, 4);
        
        // Send the TURN client arbitrary data from the remote host.  Then
        // we'll make sure it receives it in a data indication.
        final String remoteHostMessage = "HELLO FROM YOUR REMOTE HOST";
        write(remoteHostSocket, remoteHostMessage);
        
        LOG.debug("Reading data indication...");
        messageAttributes = 
            readMessage(m_turnClientSocket, StunMessageType.DATA_INDICATION,
            StunAttributeType.DATA, remoteHostMessage.length());
        
        final DataAttribute dataAttribute = 
            (DataAttribute) messageAttributes.get(StunAttributeType.DATA);
       
        assertEquals(remoteHostMessage, 
            new String(dataAttribute.getData(), "US-ASCII"));
        // Done with remote host data check.
        
        
        // Now send data from the TURN client socket.  We'll wrap this in a
        // Send Indication, but the remote host should receive it as raw data.
        final String turnClientMessage = 
            "HELLOW FROM YOUR FRIENDLY TURN CLIENT\r\n";
        final byte[] turnClientMessageBytes = 
            turnClientMessage.getBytes("US-ASCII");
        final SendIndication sendIndication = 
            new SendIndication(remoteHostAddress, turnClientMessageBytes);
        
        LOG.debug("About to write...");
        write(m_turnClientSocket, sendIndication);
        // Read the raw data from the remote host socket.
        final Scanner scanner = 
            new Scanner(remoteHostSocket.getInputStream());
        scanner.useDelimiter("\r\n");
        assertTrue(scanner.hasNext());
        final String dataOnRemoteHost = scanner.next();
        assertEquals(turnClientMessage.trim(), dataOnRemoteHost);
        }

    private Map<StunAttributeType, StunAttribute> readMessage(
        final Socket socket, 
        final StunMessageType expectedMessageType, 
        final StunAttributeType expectedAttributeType, 
        final int expectedAttributeLength) throws Exception
        {
        final InputStream is = socket.getInputStream();
        final ByteBuffer responseHeaderBuffer = ByteBuffer.allocate(20);
        LOG.debug("Reading response...");
        // Read the whole header.
        for (int i = 0; i < 20; i++)
            {
            responseHeaderBuffer.put((byte) is.read());
            }
        
        responseHeaderBuffer.flip();
        
        final int typeInt = responseHeaderBuffer.getUnsignedShort();
        final StunMessageType type = StunMessageType.toType(typeInt);
        LOG.debug("Got message type: "+type);
        assertEquals(expectedMessageType, type);

        final int messageLength = responseHeaderBuffer.getUnsignedShort();
        LOG.debug("Got message length: "+messageLength);
        

        final byte[] transactionIdBytes = new byte[16];
        responseHeaderBuffer.get(transactionIdBytes);
        
        // We ignore the transaction ID, since it's tricky for indications.
        
        final byte[] bodyBytes = new byte[messageLength];
        is.read(bodyBytes);
        
        final ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyBytes);
        final StunAttributesFactory attributesFactory = 
            new StunAttributesFactoryImpl();
        
        final Map<StunAttributeType, StunAttribute> attributes = 
            attributesFactory.createAttributes(bodyBuffer); 
        
        final StunAttribute attribute = attributes.get(expectedAttributeType);
        assertNotNull("Attribute did not exist", attribute);
        assertEquals(expectedAttributeLength, attribute.getBodyLength());
        return attributes;
        }

    private void write(final Socket socket, final String msg) throws Exception
        {
        final OutputStream os = socket.getOutputStream();
        os.write(msg.getBytes("US-ASCII"));
        os.flush();
        }

    private void write(final Socket socket, 
        final StunMessage request) throws Exception
        {
        final ByteBuffer buffer = toByteBuffer(request);
        final OutputStream os = socket.getOutputStream();
        os.write(MinaUtils.toByteArray(buffer));
        os.flush();
        }

    private ByteBuffer toByteBuffer(final StunMessage message)
        {
        final StunMessageEncoder encoder = new StunMessageEncoder();
        return encoder.encode(message);
        }
    }
