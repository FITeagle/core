package org.fiteagle.core.repo.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.OntologyModels;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This Class sends the ontology Model over message bus on startup
 */
public class ResourceRepoMDBSender implements ServletContextListener{
 
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private static Logger LOGGER = Logger.getLogger(ResourceRepoMDBSender.class.toString());
  
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      Model model = OntologyModels.getModel();
      Model messageModel = MessageBusMsgFactory.createMsgInform(model);
      String serializedRDF = MessageBusMsgFactory.serializeModel(messageModel);
      
      final Message eventMessage = this.context.createMessage();
      
      eventMessage.setStringProperty(IMessageBus.METHOD_TYPE, IMessageBus.TYPE_INFORM);
      eventMessage.setStringProperty(IMessageBus.RDF, serializedRDF);
      eventMessage.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);
      LOGGER.log(Level.INFO, "Sending Ontology Model as Inform Message");
      this.context.createProducer().send(topic, eventMessage);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
