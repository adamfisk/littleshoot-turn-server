package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.turn.RandomNonCollidingPortGeneratorImpl;
import org.lastbamboo.common.turn.stub.IoSessionStub;

/**
 * Tests the class for managing TURN clients.
 */
public final class TurnClientManagerImplTest extends TestCase
    {

    /**
     * Tests the class for removing bindings to TURN cleints.
     *
     * @throws Exception If any unexpected error occurs.
     */
    public void testRemoveBinding() throws Exception
        {
        final RandomNonCollidingPortGeneratorImpl portGenerator =
            new RandomNonCollidingPortGeneratorImpl();
        final TurnClientManagerImpl clientManager =
            new TurnClientManagerImpl(portGenerator);

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
        client.handleConnect((InetSocketAddress)socket.getLocalSocketAddress());
        
        // Make sure we can connect to the allocated address for the TURN
        // client.
        connectToServerSuccess(client.getRelayAddress(), socket);

        final TurnClient removedClient = clientManager.removeBinding(session);

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
        client.setSoTimeout(1000);
        try
            {
            client.connect(allocatedSocketAddress);
            assertTrue(client.isBound());
            assertTrue(client.isConnected());
            }
        catch (final IOException e)
            {
            fail("Should have been able to connect to server");
            }
        }

    private void connectToServer(final InetSocketAddress allocatedSocketAddress)
        throws SocketException
        {
        final Socket client = new Socket();
        client.setSoTimeout(1000);
        try
            {
            client.connect(allocatedSocketAddress);
            assertFalse(client.isBound());
            assertFalse(client.isConnected());
            fail("Should not have been able to connect to server");
            }
        catch (final IOException e)
            {
            // Expected since the server should be closed.
            }
        }
    }
