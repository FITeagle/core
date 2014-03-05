package org.fiteagle.core.repository.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.IResourceRepository.Serialization;
import org.fiteagle.core.AbstractModuleMDB;
import org.fiteagle.core.repository.ResourceRepository;

@MessageDriven(name = "ResourceRepositoryMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.MESSAGE_FILTER),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceRepositoryMDB extends AbstractModuleMDB implements
		MessageListener {

	private ResourceRepository repo;

	public ResourceRepositoryMDB() throws JMSException {
		this.repo = new ResourceRepository();
	}

	private final static Logger LOGGER = Logger
			.getLogger(ResourceRepositoryMDB.class.toString());

	public ResourceRepositoryMDB(final ConnectionFactory connectionFactory,
			final Topic topic) throws JMSException {
		super(connectionFactory, topic);
	}

	public void onMessage(final Message rcvMessage) {
		try {
			ResourceRepositoryMDB.LOGGER.info("Received a message");
			final Serialization serialization = getSerialization(rcvMessage);
			final String result = this.repo.listResources(serialization);
			this.sendMessage(result);
		} catch (final JMSException e) {
			ResourceRepositoryMDB.LOGGER.log(Level.SEVERE, "Issue with JMS", e);
		}
	}

	private Serialization getSerialization(final Message rcvMessage)
			throws JMSException {
		final Serialization serialization;
		if (IResourceRepository.SERIALIZATION_XML.equals(rcvMessage.getStringProperty(IResourceRepository.PROP_SERIALIZATION))) {
			serialization = Serialization.XML;
		} else {
			serialization = Serialization.TTL;
		}
		return serialization;
	}
}
