package org.fiteagle.core.bus.dm;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fiteagle.api.core.IMessageBus;

@ServerEndpoint("/api/logger")
@MessageDriven(name = "LoggerMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class MessageBusLoggerWS implements MessageListener {

	private static final Logger LOGGER = Logger
			.getLogger(MessageBusLoggerWS.class.getName());

	private Session wsSession;

	@OnOpen
	public void onOpen(final Session wsSession, final EndpointConfig config)
			throws IOException {
		LOGGER.log(
				Level.INFO,
				"Opening WebSocket connection with "
						+ wsSession.getId() + "...");
		this.wsSession = wsSession;
	}

	public void onMessage(final Message message) {
		try {
			LOGGER.log(Level.INFO, "Logging JMS message...");
			if (null != this.wsSession && this.wsSession.isOpen()) {
				String result = messageToString(message);
				Set<Session> sessions = this.wsSession.getOpenSessions();
				for (Session client : sessions) {
					client.getAsyncRemote().sendText(result);
				}
			} else {
				LOGGER.log(Level.INFO, "No client to talk to");
			}
		} catch (JMSException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}

	private String messageToString(Message message) throws JMSException {
		String result = "";

		result += "Message ID: " + message.getJMSMessageID() + "\n";
		result += "  * JMSCorrelationID: " + message.getJMSCorrelationID()
				+ "\n";
		result += "  * Request: "
				+ message.getStringProperty(IMessageBus.TYPE_REQUEST) + "\n";
		result += "  * Result: "
				+ message.getStringProperty(IMessageBus.TYPE_RESULT) + "\n";

		return result;
	}
}
