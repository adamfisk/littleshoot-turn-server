package org.lastbamboo.common.turn.server;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.turn.stub.IoSessionStub;
import org.littleshoot.util.NetworkUtils;

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
    public void testCloseHandling() throws Exception
        {
        final IoSession readerWriter = new IoSessionStub();
        final TurnClientImpl turnClient = 
            new TurnClientImpl(NetworkUtils.getLocalHost(), readerWriter);
        
        final InetSocketAddress remoteHostAddress = 
            new InetSocketAddress(NetworkUtils.getLocalHost(), 5342);
        final IoSession remoteHostIoSession = 
            new IoSessionStub(remoteHostAddress);
        
        // Just do this to add permissions for the host.
        turnClient.handleConnect(remoteHostAddress);
        
        turnClient.addConnection(remoteHostIoSession);
        
        final ByteBuffer data = ByteBuffer.allocate(10);
        assertTrue(turnClient.write(remoteHostAddress, data));
        
        //remoteHostIoSession.close();
        
        turnClient.removeConnection(remoteHostIoSession);
        
        assertFalse(turnClient.write(remoteHostAddress, data));
        }
    
    }
