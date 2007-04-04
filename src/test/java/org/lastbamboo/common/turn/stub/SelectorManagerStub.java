package org.lastbamboo.common.turn.stub;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import org.lastbamboo.common.nio.SelectorHandler;
import org.lastbamboo.common.nio.SelectorManager;

/**
 * Stub selector for testing.
 */
public final class SelectorManagerStub implements SelectorManager
    {

    public void start() throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void registerChannelLater(SelectableChannel channel,
        int selectionKeys, SelectorHandler handler)
        {
        // TODO Auto-generated method stub

        }

    public void addChannelInterestNow(SelectableChannel channel, int interest)
        throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void addChannelInterestLater(SelectableChannel channel, int interest)
        {
        // TODO Auto-generated method stub

        }

    public void registerChannelNow(SelectableChannel channel,
        int selectionKeys, SelectorHandler handler) throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void removeChannelInterestNow(SelectableChannel channel, int interest)
        throws IOException
        {
        // TODO Auto-generated method stub

        }

    public void removeChannelInterestLater(SelectableChannel channel,
        int interest)
        {
        // TODO Auto-generated method stub

        }

    public void invokeAndWait(Runnable task) throws InterruptedException
        {
        // TODO Auto-generated method stub

        }

    public void close()
        {
        // TODO Auto-generated method stub
        
        }

    public void invokeLater(Runnable runnable)
        {
        // TODO Auto-generated method stub
        
        }

    }
