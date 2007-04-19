package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.lastbamboo.common.nio.NioReaderWriter;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.TurnMessageFactoryImpl;
import org.lastbamboo.common.turn.message.attribute.TurnAttributeFactoryImpl;
import org.lastbamboo.common.turn.server.TurnClient;
import org.lastbamboo.common.turn.server.TurnClientImpl;
import org.lastbamboo.common.turn.stub.ReaderWriterStub;
import org.lastbamboo.common.turn.stub.SelectorManagerStub;
import org.lastbamboo.common.turn.stub.SocketChannelStub;
import org.lastbamboo.common.util.NetworkUtils;

/**
 * Tests the class that handles services for a single TURN client.
 */
public final class TurnClientImplTest extends TestCase
    {
    
    /**
     * Test to make sure the TURN client responds appropriately to socket 
     * closings.
     * 
     * @throws Exception If any unexpected error occurs.
     */
    public void testCloseListening() throws Exception
        {
        final InetSocketAddress allocatedAddress =
            new InetSocketAddress(NetworkUtils.getLocalHost(), 4859);
       
        final ReaderWriter readerWriter = new ReaderWriterStub();
        final SelectorManager selector = new SelectorManagerStub();
        final TurnMessageFactory messageFactory = new TurnMessageFactoryImpl();
        final TurnClientImpl turnClient = 
            new TurnClientImpl(allocatedAddress, readerWriter, selector, 
                messageFactory);
        
        final InetSocketAddress remoteHostAddress = 
            new InetSocketAddress(NetworkUtils.getLocalHost(), 5342);
        final ReaderWriter remoteHostReaderWriter = 
            new NioReaderWriter(new SocketChannelStub(), selector);
        
        // Just do this to add permissions for the host.
        turnClient.write(remoteHostAddress, ByteBuffer.allocate(0));
        turnClient.addConnection(remoteHostAddress, remoteHostReaderWriter);
        
        final ByteBuffer data = ByteBuffer.allocate(10);
        turnClient.write(remoteHostAddress, data);
        
        remoteHostReaderWriter.close();
        
        try
            {
            turnClient.write(remoteHostAddress, data);
            fail("Should have thrown IOE");
            }
        catch (final IOException e)
            {
            // Expected.
            }
        }

    /**
     * Test to make sure this class successfully differentiates between
     * connections from the same client running on different ports.
     * 
     * @throws Exception If any unexpected error occurs.
     */
    public void testMultipleConnectionsForSingleIp() throws Exception
        {
        final TurnClientImpl client = createTurnClient();
        
        final ByteBuffer testData1 = ByteBuffer.allocate(10); 
        final ByteBuffer testData2 = ByteBuffer.allocate(20); 
        final MockControl readerWriter1Control = 
            MockControl.createControl(ReaderWriter.class);
        final MockControl readerWriter2Control = 
            MockControl.createControl(ReaderWriter.class);
        final ReaderWriter readerWriter1 = 
            (ReaderWriter) readerWriter1Control.getMock();
        final ReaderWriter readerWriter2 = 
            (ReaderWriter) readerWriter2Control.getMock();
        readerWriter1.write(testData1);
        readerWriter2.write(testData2);
        
        readerWriter1Control.replay();
        readerWriter2Control.replay();
        
        final InetSocketAddress destinationAddress1 =
            new InetSocketAddress(NetworkUtils.getLocalHost(), 7680);       
        final InetSocketAddress destinationAddress2 =
            new InetSocketAddress(NetworkUtils.getLocalHost(), 7681);
        
        // This just opens up permission for the clients.
        client.write(destinationAddress1, ByteBuffer.allocate(0));
        client.write(destinationAddress2, ByteBuffer.allocate(0));
        client.addConnection(destinationAddress1, readerWriter1); 
        client.addConnection(destinationAddress2, readerWriter2); 
        
        client.write(destinationAddress1, testData1);
        client.write(destinationAddress2, testData2);
        readerWriter1Control.verify();
        readerWriter2Control.verify();
        }
    
    private TurnClientImpl createTurnClient() throws UnknownHostException
        {
        final TurnAttributeFactoryImpl attributeFactory =
            new TurnAttributeFactoryImpl();
        final TurnMessageFactoryImpl messageFactory = 
            new TurnMessageFactoryImpl();
        messageFactory.setAttributeFactory(attributeFactory);
        
        final InetSocketAddress allocatedAddress = 
            new InetSocketAddress(NetworkUtils.getLocalHost(), 11111); 
        
        final SelectorManager selector = new SelectorManagerStub();
        final TurnClientImpl client = new TurnClientImpl(allocatedAddress, 
            null, selector, messageFactory);
        return client;
        }

    /**
     * This used to test to make sure we couldn't add connections to TURN 
     * clients when those clients don't have permission to connect.  We now
     * allow this since the permission system is prohibitive in practice,
     * necessitating unacceptable waits for TURN Send Requests as UASes, 
     * causing socket resolution to just take too long.
     * 
     * @throws Exception If any unexpected error occurs.
     */
    public void testAddPermission() throws Exception
        {
        final TurnClient client = createTurnClient();
        final InetSocketAddress destinationAddress =
            new InetSocketAddress(NetworkUtils.getLocalHost(), 7680); 
        
        final InetAddress address = destinationAddress.getAddress();
        assertFalse("Should not have incoming permission", 
            client.hasIncomingPermission(address));
        
        try
            {
            client.addConnection(destinationAddress, new ReaderWriterStub());
            
            }
        catch (final IllegalArgumentException e)
            {
            // We don't care about permissions anymore since they cause 
            // unreasonable difficulties in the implementation.
            fail("Should have thrown an exception for not having permissions");
            }
        
        assertTrue("Should have incoming permission", 
            client.hasIncomingPermission(address));
        
        client.write(destinationAddress, ByteBuffer.allocate(10));
        assertTrue("Should have incoming permission", 
            client.hasIncomingPermission(address));
        }
    }
