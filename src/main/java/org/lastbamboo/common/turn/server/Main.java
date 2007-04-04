package org.lastbamboo.common.turn.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Kicks off a TURN server.
 */
public final class Main
    {

    /**
     * Kicks off a server.
     * 
     * @param args Command line arguments.
     */
    public static void main(String[] args)
        {
        final String[] contexts =
            new String[] {"turnStackBeans.xml",
                "turnServerBeans.xml"};

        // This kicks everything off.
        new ClassPathXmlApplicationContext(contexts);
        }

    }
