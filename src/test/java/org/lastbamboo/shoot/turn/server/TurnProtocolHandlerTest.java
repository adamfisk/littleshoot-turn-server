package org.lastbamboo.shoot.turn.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.MockControl;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.nio.SelectorManagerImpl;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.TurnProtocolHandler;
import org.lastbamboo.shoot.turn.message.TurnMessage;
import org.lastbamboo.shoot.turn.message.TurnMessageFactoryImpl;
import org.lastbamboo.shoot.turn.message.TurnMessageTypes;
import org.lastbamboo.shoot.turn.message.TurnMessageVisitor;
import org.lastbamboo.shoot.turn.message.attribute.TurnAttributeFactory;
import org.lastbamboo.shoot.turn.message.attribute.TurnAttributeFactoryImpl;
import org.lastbamboo.shoot.turn.message.attribute.handler.TurnAttributeHandlerFactoryImpl;
import org.lastbamboo.shoot.turn.message.attribute.reader.TurnAttributesReaderImpl;
import org.lastbamboo.shoot.turn.message.handler.TurnMessageHandlerFactory;
import org.lastbamboo.shoot.turn.util.RandomNonCollidingPortGeneratorImpl;

/**
 * Test for the class that processes incoiming TURN messages.
 */
public class TurnProtocolHandlerTest extends TestCase
    {

    private static final Log LOG = 
        LogFactory.getLog(TurnProtocolHandlerTest.class);
    
    /**
     * Tests the method for handling incoming message data.
     * @throws Exception If any unexpected error occurs.
     */
    public void testHandleMessages() throws Exception
        {        
        final ByteBuffer buffer = ByteBuffer.allocate(1024*10);
        final TurnAttributeFactory attributeFactory = 
            new TurnAttributeFactoryImpl();
        final TurnMessageFactoryImpl factory = new TurnMessageFactoryImpl();
        factory.setAttributeFactory(attributeFactory);
        
        //final Map attributes = new HashMap();
        final TurnAttributeHandlerFactoryImpl attributeHandlerFactory = 
            new TurnAttributeHandlerFactoryImpl();
        attributeHandlerFactory.setTurnAttributeFactory(attributeFactory);
        final TurnAttributesReaderImpl attributesReader = 
            new TurnAttributesReaderImpl();
        attributesReader.setTurnAttributeHandlerFactory(
            attributeHandlerFactory);
        final MockControl control = 
            MockControl.createControl(TurnMessageHandlerFactory.class);
        final TurnMessageHandlerFactory requestHandlerFactory = 
            (TurnMessageHandlerFactory) control.getMock();
        
        final InetSocketAddress address = 
            new InetSocketAddress(InetAddress.getLocalHost(), 20000);


        
        final MockControl readerWriterControl = 
            MockControl.createControl(ReaderWriter.class);
        final ReaderWriter readerWriter = 
            (ReaderWriter) readerWriterControl.getMock();
        requestHandlerFactory.createTurnMessageHandler(
            TurnMessageTypes.ALLOCATE_REQUEST, address);
        control.setReturnValue(
            new AllocateRequestHandler(address, factory, attributesReader), 2);
        
        final SelectorManager selector = new SelectorManagerImpl();
        final TurnClientManager clientManager = 
            new TurnClientManagerImpl(selector, factory, 
                new RandomNonCollidingPortGeneratorImpl());
        final TurnMessageVisitor visitor = 
            new TurnServerResponder(readerWriter, factory, clientManager);
        final TurnProtocolHandler handler = 
            new TurnProtocolHandler(requestHandlerFactory, visitor);
        
        control.replay();
        
        TurnMessage message = factory.createAllocateRequest();
        Collection buffers = message.toByteBuffers();
        for (final Iterator iter = buffers.iterator(); iter.hasNext();)
            {
            final ByteBuffer curBuffer = (ByteBuffer) iter.next();
            curBuffer.rewind();
            LOG.trace("1) Adding buffer of length: "+curBuffer.remaining());
            buffer.put(curBuffer);
            }

        handler.handleMessages(buffer, address);
        
        assertEquals(0, buffer.remaining());
        
        buffer.clear();
        
        // Now test with a partial message -- no transaction ID.
        message = factory.createAllocateRequest();
        buffers = message.toByteBuffers();
        
        final ByteBuffer[] bufferArray = 
            (ByteBuffer[]) buffers.toArray(new ByteBuffer[0]);

        for (int i = 0; i < (bufferArray.length-1); i++)
            {
            LOG.trace("2) Adding buffer of length: "+
                bufferArray[i].remaining());
            final ByteBuffer curBuffer = bufferArray[i];
            curBuffer.rewind();
            buffer.put(curBuffer);
            }
        
        handler.handleMessages(buffer, address);
        
        assertEquals(0, buffer.remaining());
        
        buffer.clear();
        
        // Add the transaction ID.
        buffer.put(bufferArray[bufferArray.length-1]);
        handler.handleMessages(buffer, address);
        
        control.verify();
        }
    }
