package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import junit.framework.TestCase;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.amazon.ec2.AmazonEc2Utils;
import org.lastbamboo.common.turn.stub.IoSessionStub;
import org.littleshoot.util.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the class for managing TURN clients.
 */
public final class TurnClientManagerImplTest extends TestCase
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    /**
     * Tests the class for removing bindings to TURN clients.
     *
     * @throws Exception If any unexpected error occurs.
     */
    public void testRemoveBinding() throws Exception
        {
        final TurnClientManagerImpl clientManager = 
            new TurnClientManagerImpl();
        
        final InetSocketAddress host =
            new InetSocketAddress("1.1.1.1", 5332);
        final IoSession session = new IoSessionStub(host); 
        final TurnClient client = clientManager.allocateBinding(session);

        final InetSocketAddress host0 =
            new InetSocketAddress("43.43.2.1", 5332);
        final InetSocketAddress host1 =
            new InetSocketAddress("4.40.12.41", 3332);
        final InetSocketAddress host2 =
            new InetSocketAddress("78.3.27.71", 6332);
        final IoSession readerWriter0 = new IoSessionStub(host0);
        final IoSession readerWriter1 = new IoSessionStub(host1);
        final IoSession readerWriter2 = new IoSessionStub(host2);

        client.write(host0, ByteBuffer.allocate(0));
        client.write(host1, ByteBuffer.allocate(0));
        client.write(host2, ByteBuffer.allocate(0));
        client.addConnection(readerWriter0);
        client.addConnection(readerWriter1);
        client.addConnection(readerWriter2);
        

        assertNotNull(clientManager.getTurnClient(session));
        
        final Socket socket = new Socket();
        
        // For it to bind to an ephemeral local port.  Make sure we're able
        // to connect.
        socket.bind(null);
        
        assertTrue(socket.isBound());
        client.handleConnect((InetSocketAddress)socket.getLocalSocketAddress());
        
        final InetSocketAddress relayAddress = client.getRelayAddress();
        m_log.debug("Connecting to relay address: {}", relayAddress);
        
        
        // Make sure we can connect to the allocated address for the TURN
        // client.
        connectToServerSuccess(relayAddress, socket);

        final TurnClient removedClient = clientManager.removeBinding(session);

        m_log.debug("About to sleep...");
        Thread.sleep(800);
        assertNull(clientManager.getTurnClient(session));

        // Make sure we can't connect to the allocated address for the TURN
        // client, since it should be closed.
        // This is disabled for now because we allow all incoming connections.
        //connectToServer(client.getAllocatedSocketAddress());

        //assertEquals(0, removedClient.getNumConnections());
        }
    
    private void connectToServerSuccess(
        final InetSocketAddress allocatedSocketAddress, final Socket client)
        throws SocketException
        {
        client.setSoTimeout(3000);

        try
            {
            final InetSocketAddress toUse;
            
            // We unfortunately have to do this because EC2 doesn't support hairpinning, 
            // so we can't connect to the real public address.
            if (AmazonEc2Utils.onEc2())
                {
                toUse = new InetSocketAddress(NetworkUtils.getLocalHost(), 
                        allocatedSocketAddress.getPort());
                }
            else
                {
                toUse = allocatedSocketAddress;
                }
            client.connect(toUse, 3000);
            assertTrue(client.isBound());
            assertTrue(client.isConnected());
            }
        catch (final IOException e)
            {
            fail("Should have been able to connect to server");
            }
        }
    }
