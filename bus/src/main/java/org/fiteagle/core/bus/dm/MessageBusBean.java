package org.fiteagle.core.bus.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;

@Stateless
public class MessageBusBean {

	@Inject
	private JMSContext jmsContext;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;
	private static final Logger LOGGER = Logger.getLogger(MessageBusBean.class
			.getName());

	public void sendMessage(Message message) throws JMSException {
		LOGGER.log(Level.INFO, "Submitting request to JMS...");
		jmsContext.createProducer().send(topic, message);
	}

	public Message createMessage() {
		if (null == jmsContext)
			LOGGER.log(Level.SEVERE, "jms context was not injected!");
		return jmsContext.createMessage();
	}

	public void sendMessage(String string) throws JMSException {
		sendMessage(jmsContext.createTextMessage(string));
	}
}
