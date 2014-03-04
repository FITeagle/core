package org.fiteagle.core;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;

public abstract class AbstractModuleMDB {

	@Resource
	private ConnectionFactory connectionFactory;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;
	protected JMSProducer messageProducer;
	private final static Logger LOGGER = Logger
			.getLogger(AbstractModuleMDB.class.toString());

	public AbstractModuleMDB(final ConnectionFactory connectionFactory,
			final Topic topic) throws JMSException {
		//to be done
	}

	public AbstractModuleMDB() throws JMSException {
	}

	public void sendMessage(final Destination receiver, final String text)
			throws JMSException {
		final Connection connection = this.connectionFactory.createConnection();
		connection.start();
		final Session session = connection.createSession(false,
				Session.AUTO_ACKNOWLEDGE);
		final MessageProducer producer = session.createProducer(receiver);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

		final Message message = session.createMessage();
		message.setStringProperty(IMessageBus.TYPE_RESPONSE,
				IResourceRepository.LIST_RESOURCES);
		message.setStringProperty(IMessageBus.TYPE_RESULT, text);

		producer.send(message);
	}

	protected void sendMessage(final String result) throws JMSException {
		this.sendMessage(this.topic, result);
	}

}
