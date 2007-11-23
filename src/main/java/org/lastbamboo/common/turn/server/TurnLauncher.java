package org.lastbamboo.common.turn.server;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

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

    private TurnClientManager m_turnClientManager;

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
        this.m_turnClientManager = 
            (TurnClientManager)context.getBean("turnClientManager");
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
        
        // Start this last because otherwise we might be seen as "online"
        // prematurely.
        startJmxServer();
        }
    

    private void startJmxServer()
        {
        LOG.debug("Starting JMX server...");
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ObjectName mbeanName;
        try
            {
            final String jmxUrl = 
                "org.lastbamboo.common.turn.server:type=TurnClientManagerImpl";
            mbeanName = new ObjectName(jmxUrl);
            }
        catch (final MalformedObjectNameException e)
            {
            LOG.error("Could not start JMX", e);
            return;
            }
        try
            {
            mbs.registerMBean(this.m_turnClientManager, mbeanName);
            }
        catch (final InstanceAlreadyExistsException e)
            {
            LOG.error("Could not start JMX", e);
            }
        catch (final MBeanRegistrationException e)
            {
            LOG.error("Could not start JMX", e);
            }
        catch (final NotCompliantMBeanException e)
            {
            LOG.error("Could not start JMX", e);
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
