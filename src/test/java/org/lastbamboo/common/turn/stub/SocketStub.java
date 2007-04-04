package org.lastbamboo.common.turn.stub;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class SocketStub extends Socket
    {

    public SocketAddress getRemoteSocketAddress()
        {
        try
            {
            return new InetSocketAddress(InetAddress.getLocalHost(), 7777);
            }
        catch (UnknownHostException e)
            {
            return null;
            }
        }
    }
