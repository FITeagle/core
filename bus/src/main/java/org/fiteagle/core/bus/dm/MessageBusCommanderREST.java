package org.fiteagle.core.bus.dm;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.fiteagle.api.core.IMessageBus;

@Path("/")
public class MessageBusCommanderREST {

	private static Logger LOGGER = Logger
			.getLogger(MessageBusCommanderREST.class.toString());
	private MessageBusBean senderBean;

	@Inject
	public MessageBusCommanderREST(MessageBusBean senderBean)
			throws NamingException {
		LOGGER.log(Level.INFO, "Started REST");
		this.senderBean = senderBean;
	}
	
	public MessageBusCommanderREST() {
		LOGGER.log(Level.INFO, "Started REST");
		this.senderBean = new MessageBusBean();
	}

	@PUT
	@Path("/commander")
	@Consumes(MediaType.TEXT_PLAIN)
	public String commander(String command) throws JMSException {
		LOGGER.log(Level.INFO, "Received REST message: " + command);

		final Message message = senderBean.createMessage();
		message.setStringProperty(IMessageBus.TYPE_REQUEST, command);
		message.setJMSCorrelationID(UUID.randomUUID().toString());

		senderBean.sendMessage(message);
		return "done sending '" + command + "'";
	}
}
