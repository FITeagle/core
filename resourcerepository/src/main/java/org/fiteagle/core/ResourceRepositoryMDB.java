package org.fiteagle.core;

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

import org.fiteagle.api.core.IResourceRepository.Serialization;

@MessageDriven(name = "ResourceRepositoryMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/core"),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "request = 'listResources'"),
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
			final Destination sender = rcvMessage.getJMSReplyTo();
			ResourceRepositoryMDB.LOGGER.info("Received a message from: "
					+ sender);
			final String result = this.repo.listResources(Serialization.XML);
			this.sendMessage(result);
		} catch (final JMSException e) {
			ResourceRepositoryMDB.LOGGER.log(Level.SEVERE, "Issue with JMS", e);
		}
	}
}
