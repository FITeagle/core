package org.fiteagle.core;

import java.util.logging.Level;
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

public abstract class AbstractModuleMDB {

    @Resource
    private ConnectionFactory connectionFactory;
	@Resource(mappedName = "java:/topic/core")
	private Topic topic;
	protected JMSProducer messageProducer;
	private final static Logger LOGGER = Logger
			.getLogger(AbstractModuleMDB.class.toString());


	public AbstractModuleMDB(ConnectionFactory connectionFactory, Topic topic)
			throws JMSException {
	}

	public AbstractModuleMDB() throws JMSException {
	}

	public void sendMessage(Destination receiver, String text) throws JMSException {
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(receiver);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        Message message = session.createMessage();
        message.setStringProperty("foo", "bar");
        message.setStringProperty("result", text);
        LOGGER.log(Level.INFO, "Sending: " + text);
        producer.send(message);
	}
	
	protected void sendMessage(String result) throws JMSException {
		sendMessage(topic, result);		
	}

}
