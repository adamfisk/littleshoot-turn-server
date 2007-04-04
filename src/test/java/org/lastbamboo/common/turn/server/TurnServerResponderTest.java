package org.lastbamboo.common.turn.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.easymock.MockControl;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.server.TurnClientImpl;
import org.lastbamboo.common.turn.server.TurnClientManagerImpl;
import org.lastbamboo.common.turn.server.TurnServerResponder;
import org.lastbamboo.shoot.turn.message.SendErrorResponse;
import org.lastbamboo.shoot.turn.message.SendRequest;
import org.lastbamboo.shoot.turn.message.SendResponse;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.attribute.SendErrorResponseCodeImpl;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Test for the class that responds to TURN messages to the server.
 */
public final class TurnServerResponderTest
    extends AbstractDependencyInjectionSpringContextTests
    {

    private TurnMessageFactory m_messageFactory;
    private TurnClientManagerImpl m_turnClientManager;

    protected String[] getConfigLocations()
        {
        return new String[] {"turnStackBeans.xml",
            "turnServerBeans.xml"};
        }

    protected void onSetUp() throws Exception
        {
        this.m_messageFactory =
            (TurnMessageFactory) applicationContext.getBean(
                "turnMessageFactory");

        this.m_turnClientManager =
            (TurnClientManagerImpl) applicationContext.getBean(
                "turnClientManager");
        }

    public void testVisitSendRequestToDeadRemoteHost() throws Exception
        {
        final InetAddress localHost = InetAddress.getLocalHost();
        final InetSocketAddress destinationAddress =
            new InetSocketAddress(localHost, 6879);

        final ByteBuffer data = ByteBuffer.allocate(26);
        final String alphabet = "abcdefghijklmnopqrstuvwxyz";
        data.put(alphabet.getBytes());
        data.rewind();
        final SendRequest request =
            this.m_messageFactory.createSendRequest(destinationAddress, data);

        final SendErrorResponse response =
            this.m_messageFactory.createSendErrorResponse(
                request.getTransactionId(),
                    SendErrorResponseCodeImpl.SEND_FAILED);

        final MockControl turnClientReaderWriterControl =
            MockControl.createControl(ReaderWriter.class);
        final ReaderWriter turnClientReaderWriter =
            (ReaderWriter) turnClientReaderWriterControl.getMock();
        turnClientReaderWriter.addCloseListener(this.m_turnClientManager);
        turnClientReaderWriterControl.setVoidCallable(1);
        turnClientReaderWriter.write(response.toByteBuffers());
        turnClientReaderWriterControl.setVoidCallable(1);
        turnClientReaderWriterControl.replay();

        final TurnClientImpl client =
            (TurnClientImpl) this.m_turnClientManager.allocateBinding(
                turnClientReaderWriter);

        final MockControl remoteHostReaderWriterControl =
            MockControl.createControl(ReaderWriter.class);
        final ReaderWriter remoteHostReaderWriter =
            (ReaderWriter) remoteHostReaderWriterControl.getMock();
        remoteHostReaderWriter.write(data);
        remoteHostReaderWriterControl.setThrowable(new IOException(), 1);
        remoteHostReaderWriterControl.replay();

        client.write(destinationAddress, ByteBuffer.allocate(10));
        client.addConnection(destinationAddress, remoteHostReaderWriter);

        final TurnServerResponder responder =
            new TurnServerResponder(turnClientReaderWriter,
                this.m_messageFactory, this.m_turnClientManager);

        responder.visitSendRequest(request);
        turnClientReaderWriterControl.verify();
        remoteHostReaderWriterControl.verify();
        }

    /**
     * Tests the method for visiting a TURN Send Request to make sure it's
     * forwarded correctly.
     */
    public void testVisitSendRequest() throws Exception
        {
        final InetAddress localHost = InetAddress.getLocalHost();
        final InetSocketAddress socketAddress =
            new InetSocketAddress(localHost, 6879);
        final ByteBuffer data = ByteBuffer.allocate(26);
        final String alphabet = "abcdefghijklmnopqrstuvwxyz";
        data.put(alphabet.getBytes());
        data.rewind();
        final SendRequest request =
            this.m_messageFactory.createSendRequest(socketAddress, data);

        assertEquals(alphabet.length(), request.getData().remaining());


        final SendResponse response =
            this.m_messageFactory.createSendResponse(
                request.getTransactionId());

        final MockControl turnClientReaderWriterControl =
            MockControl.createControl(ReaderWriter.class);
        final ReaderWriter turnClientReaderWriter =
            (ReaderWriter) turnClientReaderWriterControl.getMock();

        turnClientReaderWriter.addCloseListener(this.m_turnClientManager);
        turnClientReaderWriterControl.setVoidCallable(1);
        turnClientReaderWriter.write(response.toByteBuffers());

        turnClientReaderWriterControl.setVoidCallable(1);

        turnClientReaderWriterControl.replay();

        final TurnClientImpl client =
            (TurnClientImpl) this.m_turnClientManager.allocateBinding(
                turnClientReaderWriter);

        final MockControl remoteHostReaderWriterControl =
            MockControl.createControl(ReaderWriter.class);
        final ReaderWriter remoteHostReaderWriter =
            (ReaderWriter) remoteHostReaderWriterControl.getMock();
        remoteHostReaderWriter.write(data);
        remoteHostReaderWriterControl.setVoidCallable(1);
        remoteHostReaderWriterControl.replay();

        client.write(socketAddress, data);
        client.addConnection(socketAddress, remoteHostReaderWriter);


        final TurnServerResponder responder =
            new TurnServerResponder(turnClientReaderWriter, 
                this.m_messageFactory, this.m_turnClientManager);

        responder.visitSendRequest(request);

        remoteHostReaderWriterControl.verify();
        turnClientReaderWriterControl.verify();
        }

    }
