package org.fiteagle.core.reservation.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ReservationMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ReservationMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ReservationMDBListener.class.toString());
  
//  private ResourceRepoHandler repoHandler;
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  @PostConstruct
  private void startUp() {
//    repoHandler = ResourceRepoHandler.getInstance();
  }
  
  public void onMessage(final Message message) {
    try {
      String messageType = message.getStringProperty(IMessageBus.METHOD_TYPE);
      String serialization = message.getStringProperty(IMessageBus.SERIALIZATION);
      String rdfString = MessageUtil.getRDFResult(message);
      
      if (messageType != null && rdfString != null) {
        Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);
        
        if (messageType.equals(IMessageBus.TYPE_INFORM)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
//          handleInform(messageModel);
          
        } else if (messageType.equals(IMessageBus.TYPE_REQUEST)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
//          handleRequest(messageModel, message.getStringProperty(IMessageBus.SERIALIZATION), message.getJMSCorrelationID());
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  
  
}