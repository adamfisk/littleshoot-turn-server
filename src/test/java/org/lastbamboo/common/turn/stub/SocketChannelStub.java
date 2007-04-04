package org.lastbamboo.common.turn.stub;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public class SocketChannelStub extends SocketChannel
    {

    private Socket m_socket;


    public SocketChannelStub()
        {
        super(new SelectorProviderStub());
        this.m_socket = new SocketStub();
        }

    public boolean finishConnect() throws IOException
        {
        // TODO Auto-generated method stub
        return false;
        }

    public boolean isConnected()
        {
        // TODO Auto-generated method stub
        return false;
        }

    public boolean isConnectionPending()
        {
        // TODO Auto-generated method stub
        return false;
        }

    public Socket socket()
        {
        return this.m_socket;
        }

    public boolean connect(SocketAddress arg0) throws IOException
        {
        // TODO Auto-generated method stub
        return false;
        }

    public int read(ByteBuffer arg0) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    public int write(ByteBuffer arg0) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    public long read(ByteBuffer[] arg0, int arg1, int arg2) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    public long write(ByteBuffer[] arg0, int arg1, int arg2) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    protected void implCloseSelectableChannel() throws IOException
        {
        // TODO Auto-generated method stub

        }

    protected void implConfigureBlocking(boolean arg0) throws IOException
        {
        // TODO Auto-generated method stub

        }

    
    private static final class SelectorProviderStub extends SelectorProvider
        {

        public DatagramChannel openDatagramChannel() throws IOException
            {
            // TODO Auto-generated method stub
            return null;
            }

        public Pipe openPipe() throws IOException
            {
            // TODO Auto-generated method stub
            return null;
            }

        public ServerSocketChannel openServerSocketChannel() throws IOException
            {
            // TODO Auto-generated method stub
            return null;
            }

        public SocketChannel openSocketChannel() throws IOException
            {
            // TODO Auto-generated method stub
            return null;
            }

        public AbstractSelector openSelector() throws IOException
            {
            // TODO Auto-generated method stub
            return null;
            }
    
        }
    }
