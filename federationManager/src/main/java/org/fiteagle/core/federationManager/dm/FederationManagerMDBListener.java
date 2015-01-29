package org.fiteagle.core.federationManager.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.vocabulary.Omn_federation;
import org.apache.jena.riot.thrift.wire.RDF_VarTuple;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;

@MessageDriven(name = "FederationManagerMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_FEDERATION_MANAGER),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class FederationManagerMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(FederationManagerMDBListener.class.toString());
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;


  public void onMessage(final Message message) {
    String messageType = MessageUtil.getMessageType(message);
    String serialization = MessageUtil.getMessageSerialization(message);
    LOGGER.log(Level.INFO, "Received a " + messageType + " message");
    
    if (messageType != null) {
      if (messageType.equals(IMessageBus.TYPE_GET)) {
        handleGet(serialization, MessageUtil.getJMSCorrelationID(message));
      }
    }
  }
  
  private void handleGet(String serialization, String requestID){
    Model federationModel = null;
    try {
    federationModel=  TripletStoreAccessor.get(Omn_federation.Infrastructure);
    } catch (ResourceRepositoryException e) {
      Message message = MessageUtil.createErrorMessage(e.getMessage(), requestID, context);
      context.createProducer().send(topic, message);
      return;
    }

    Message message = MessageUtil.createRDFMessage(federationModel, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
    context.createProducer().send(topic, message);
  }
  
}