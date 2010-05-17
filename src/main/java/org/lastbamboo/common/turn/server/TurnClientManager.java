package org.lastbamboo.common.turn.server;

import org.littleshoot.mina.common.IoSession;

/**
 * Interface for classes that manage allocated binding for TURN clients.  These
 * bindings allow external agents to access TURN clients as if the bindings
 * were there own.
 */
public interface TurnClientManager
    {

    /**
     * Allocates a binding for the specified TURN client that other nodes can
     * use to access it.
     *  
     * @param readerWriter The class for writing data back to the TURN client.
     * @return The client proxy.
     */
    TurnClient allocateBinding(final IoSession readerWriter);

    /**
     * Accessor for the TURN client handler for the specified TURN client 
     * address.
     * @param readerWriter The reader/writer for the client. 
     * @return The TURN client class that handles writing data to the client
     * and accepting incoming connection on the client's behalf.
     */
    TurnClient getTurnClient(final IoSession readerWriter);

    /**
     * Removes the TURN client associated with the specified reader/writer
     * instance.
     * @param readerWriter The reader/writer to remove.
     * @return The removed client instance.
     */
    TurnClient removeBinding(final IoSession readerWriter);

    }
