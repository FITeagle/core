package org.fiteagle.core.repo.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.ResourceRepository;

@MessageDriven(name = "ResourceRepositoryMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.MESSAGE_FILTER),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRepositoryMDB implements MessageListener {

	private ResourceRepository repo;
	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;

	public ResourceRepositoryMDB() throws JMSException {
		this.repo = new ResourceRepository();
	}

	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepositoryMDB.class.toString());

	public void onMessage(final Message rcvMessage) {
		try {
			ResourceRepositoryMDB.LOGGER.info("Received a message");
			final String serialization = rcvMessage.getStringProperty(IResourceRepository.PROP_SERIALIZATION);
			final String result = this.repo.listResources(serialization);
			final String id = rcvMessage.getJMSCorrelationID();					
			final Message message = this.context.createMessage();
			
			message.setStringProperty(IMessageBus.TYPE_RESPONSE,
					IResourceRepository.LIST_RESOURCES);
			message.setStringProperty(IMessageBus.TYPE_RESULT, result);
			if (null != id)
				message.setJMSCorrelationID(id);

			this.context.createProducer().send(topic, message);
		} catch (final JMSException e) {
			ResourceRepositoryMDB.LOGGER.log(Level.SEVERE, "Issue with JMS", e);
		}
	}
}
