package org.lastbamboo.common.turn.stub;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.lastbamboo.common.util.NetworkUtils;

public class SocketStub extends Socket
    {

    public SocketAddress getRemoteSocketAddress()
        {
        try
            {
            return new InetSocketAddress(NetworkUtils.getLocalHost(), 7777);
            }
        catch (UnknownHostException e)
            {
            return null;
            }
        }
    }
