package org.fiteagle.core.bus.dm;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fiteagle.api.core.IMessageBus;

@Named
@ServerEndpoint("/api/commander")
public class MessageBusCommanderWS {

	private static final Logger LOGGER = Logger
			.getLogger(MessageBusCommanderWS.class.getName());

	private MessageBusBean senderBean;

	@Inject
	public MessageBusCommanderWS(MessageBusBean senderBean) {
		this.senderBean = senderBean;
	}

	@OnMessage
	public String onMessage(final String command) throws JMSException {
		LOGGER.log(Level.INFO, "Received WebSocket message: " + command);
		
		final Message message = senderBean.createMessage();
		message.setStringProperty(IMessageBus.TYPE_REQUEST, command);
		message.setJMSCorrelationID(UUID.randomUUID().toString());

		senderBean.sendMessage(message);
		return "";
	}

	@OnOpen
	public void onOpen(final Session wsSession, final EndpointConfig config)
			throws IOException {
		LOGGER.log(Level.INFO, "Opening WebSocket connection...");
	}
}
