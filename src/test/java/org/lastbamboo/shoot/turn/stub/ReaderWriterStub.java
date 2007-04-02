package org.lastbamboo.shoot.turn.stub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.lastbamboo.shoot.protocol.CloseListener;
import org.lastbamboo.shoot.protocol.ProtocolHandler;
import org.lastbamboo.shoot.protocol.ReaderWriter;
import org.lastbamboo.shoot.protocol.WriteListener;

/**
 * Stub class for testing.
 */
public final class ReaderWriterStub implements ReaderWriter
    {

    public void read() throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void setProtocolHandler(ProtocolHandler protocolHandler)
        {
        // TODO Auto-generated method stub

        }

    public boolean write() throws IOException
        {
        // TODO Auto-generated method stub
        return false;
        }

    public void write(ByteBuffer buffer) throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void write(Collection buffers) throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void close()
        {
        // TODO Auto-generated method stub

        }

    public void addCloseListener(CloseListener listener)
        {
        // TODO Auto-generated method stub

        }

    public void writeLater(Collection buffers)
        {
        // TODO Auto-generated method stub

        }

    public void writeLater(ByteBuffer data)
        {
        // TODO Auto-generated method stub
        
        }

    public void writeLaterWhenBufferFree(Collection buffers)
        {
        // TODO Auto-generated method stub
        
        }

    public InetSocketAddress getRemoteSocketAddress()
        {
        // TODO Auto-generated method stub
        return null;
        }
    
    public InetSocketAddress getLocalSocketAddress()
        {
        // TODO Auto-generated method stub
        return null;
        }

    public void write(ByteBuffer buffer, WriteListener listener) throws IOException
        {
        // TODO Auto-generated method stub
        
        }

    public void writeLater(ByteBuffer data, WriteListener listener)
        {
        // TODO Auto-generated method stub
        }

    public boolean isClosed()
        {
        // TODO Auto-generated method stub
        return false;
        }

    public SocketChannel getSocketChannel()
        {
        // TODO Auto-generated method stub
        return null;
        }
    }
