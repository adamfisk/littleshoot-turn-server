package org.lastbamboo.shoot.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;

import junit.framework.TestCase;

import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.nio.SelectorManagerImpl;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageFactoryImpl;
import org.lastbamboo.shoot.turn.stub.ReaderWriterStub;
import org.lastbamboo.shoot.turn.util.RandomNonCollidingPortGeneratorImpl;

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
        final SelectorManager selector = new SelectorManagerImpl();
        selector.start();
        final TurnMessageFactory turnMessageFactory =
            new TurnMessageFactoryImpl();
        final RandomNonCollidingPortGeneratorImpl portGenerator =
            new RandomNonCollidingPortGeneratorImpl();
        final TurnClientManagerImpl clientManager =
            new TurnClientManagerImpl(selector, turnMessageFactory,
                portGenerator);

        final ReaderWriter readerWriter = new ReaderWriterStub();
        final TurnClient client = clientManager.allocateBinding(readerWriter);

        final InetSocketAddress host0 =
            new InetSocketAddress("43.43.2.1", 5332);
        final InetSocketAddress host1 =
            new InetSocketAddress("4.40.12.41", 3332);
        final InetSocketAddress host2 =
            new InetSocketAddress("78.3.27.71", 6332);
        final ReaderWriter readerWriter0 = new ReaderWriterStub();
        final ReaderWriter readerWriter1 = new ReaderWriterStub();
        final ReaderWriter readerWriter2 = new ReaderWriterStub();

        client.write(host0, ByteBuffer.allocate(0));
        client.write(host1, ByteBuffer.allocate(0));
        client.write(host2, ByteBuffer.allocate(0));
        client.addConnection(host0, readerWriter0);
        client.addConnection(host1, readerWriter1);
        client.addConnection(host2, readerWriter2);

        assertNotNull(clientManager.getTurnClient(readerWriter));

        final TurnClient removedClient =
            clientManager.removeBinding(readerWriter);

        Thread.sleep(2000);
        assertNull(clientManager.getTurnClient(readerWriter));

        // Make sure we can't connect to the allocated address for the TURN
        // client, since it should be closed.
        connectToServer(client.getAllocatedSocketAddress());

        assertEquals(0, removedClient.getNumConnections());
        }

    private void connectToServer(final InetSocketAddress allocatedSocketAddress)
        throws SocketException
        {
        final Socket client = new Socket();
        client.setSoTimeout(4000);
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
