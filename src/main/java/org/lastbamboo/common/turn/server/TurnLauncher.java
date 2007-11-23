package org.lastbamboo.common.turn.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
        launcher.start();
        LOG.debug("Started launcher");
        }

    private TurnServer m_turnServer;

    /**
     * Creates a new TURN launcher.
     */
    public TurnLauncher()
        {
        loadContexts();
        }
    
    private void loadContexts()
        {
        final String[] contexts = 
            {
            "turnServerBeans.xml", 
            "amazonEc2Beans.xml"
            };
        LOG.debug("Loading contexts...");
        final ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext(contexts);
        LOG.debug("Loaded contexts...");
        this.m_turnServer = (TurnServer) context.getBean("turnServer");
        LOG.debug("Loaded context...");
        }

    /**
     * Launches any services that should be launched only if this peer is on
     * the open Internet, such as running a TURN server or a SIP proxy.
     */
    public void start()
        {
        // Launch the TURN server
        m_turnServer.start ();
        }
    

    /**
     * Stops the server.
     */
    public void stop()
        {
        this.m_turnServer.stop();
        }
    }
