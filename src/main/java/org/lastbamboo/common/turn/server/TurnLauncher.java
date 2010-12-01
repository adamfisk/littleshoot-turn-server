package org.lastbamboo.common.turn.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher class for launching a TURN servers.
 */
public class TurnLauncher
    {

    private static final Logger LOG = 
        LoggerFactory.getLogger(TurnLauncher.class);
    
    /**
     * Launches the SIP and TURN servers.
     * 
     * @param args The command line arguments.
     */
    public static void main(final String[] args)
        {
        LOG.debug("Launching SIP and TURN servers...");
        final TurnLauncher launcher = new TurnLauncher();
        LOG.debug("Created launcher");
        try
            {
            launcher.start();
            LOG.debug("Started launcher");
            }
        catch (final Throwable t)
            {
            LOG.error("Could not start!!!", t);
            }
        }

    private TurnServer m_turnServer;

    /**
     * Creates a new TURN launcher.
     */
    public TurnLauncher()
        {
        this.m_turnServer = new TcpTurnServer(new TurnClientManagerImpl());
        }

    /**
     * Launches any services that should be launched only if this peer is on
     * the open Internet, such as running a TURN server or a SIP proxy.
     * @throws IOException If we could not bind to the server socket.
     */
    public void start() throws IOException
        {
        // Launch the TURN server
        m_turnServer.start ();
        
        // Just keep the thread open.
        try
            {
            synchronized (this)
                {
                wait();
                }
            }
        catch (final InterruptedException e)
            {
            LOG.debug("Got interrupt -- CTR-Ced?", e);
            }
        }
    

    /**
     * Stops the server.
     */
    public void stop()
        {
        this.m_turnServer.stop();
        }
    }
