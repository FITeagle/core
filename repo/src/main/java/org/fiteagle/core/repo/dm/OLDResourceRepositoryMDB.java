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
import org.fiteagle.core.repo.OLDResourceRepository;

@MessageDriven(name = "ResourceRepositoryMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.MESSAGE_FILTER),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OLDResourceRepositoryMDB implements MessageListener {

	private OLDResourceRepository repo;
	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;

	public OLDResourceRepositoryMDB() throws JMSException {
		this.repo = new OLDResourceRepository("dummy-answer.xml");
	}

	private final static Logger LOGGER = Logger
			.getLogger(OLDResourceRepositoryMDB.class.toString());

	public void onMessage(final Message rcvMessage) {
		try {
			OLDResourceRepositoryMDB.LOGGER.info("Received a message");
			final String serialization = rcvMessage.getStringProperty(IMessageBus.SERIALIZATION);
			final String query = rcvMessage.getStringProperty(IMessageBus.QUERY);
			final String result = this.repo.queryDatabse(query, serialization);
			final String id = rcvMessage.getJMSCorrelationID();					
			final Message message = this.context.createMessage();
			
			message.setStringProperty(IMessageBus.TYPE,
					IMessageBus.RESPONSE);
			message.setStringProperty(IMessageBus.RESULT, result);
			if (null != id)
				message.setJMSCorrelationID(id);

			this.context.createProducer().send(topic, message);
		} catch (final JMSException e) {
			OLDResourceRepositoryMDB.LOGGER.log(Level.SEVERE, "Issue with JMS", e);
		}
	}
}
