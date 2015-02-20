package org.fiteagle.core.resourceAdapterManager.dm;

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

import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import org.fiteagle.api.core.Config;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ResourceAdapterManagerMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_RESOURCE_ADAPTER_MANAGER),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceAdapterManagerMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ResourceAdapterManagerMDBListener.class.toString());
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  public void onMessage(final Message message) {
    String messageType = MessageUtil.getMessageType(message);
    String serialization = MessageUtil.getMessageSerialization(message);
    String rdfString = MessageUtil.getStringBody(message);
    LOGGER.log(Level.INFO, "Received a " + messageType + " message");
    
    if (messageType != null && rdfString != null) {
      if (messageType.equals(IMessageBus.TYPE_CREATE)) {
        Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);
        handleCreate(messageModel, serialization, MessageUtil.getJMSCorrelationID(message));
        
      } else if (messageType.equals(IMessageBus.TYPE_GET)) {
        handleGet(message, serialization, MessageUtil.getJMSCorrelationID(message));
        
      } else if (messageType.equals(IMessageBus.TYPE_DELETE)) {
        Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);
        handleDelete(messageModel);
      }
    }
  }

  private void handleGet(Message message, String serialization, String requestID) {
    Message responseMessage = null;

      //String serializedResponse = TripletStoreAccessor.handleSPARQLRequest(MessageUtil.getSPARQLQuery(message), serialization);
      String serializedResponse = TripletStoreAccessor.getResources();
      responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);

      context.createProducer().send(topic, responseMessage);

  }
  
  private void handleCreate(Model model, String serialization, String requestID) {
    try {
        ResIterator resIterator = model.listSubjectsWithProperty(Omn_lifecycle.parentTo);
        while (resIterator.hasNext()){
            TripletStoreAccessor.addResource(resIterator.nextResource());
        }
        //TripletStoreAccessor.updateRepositoryModel(model);
        
        ResIterator iterator = model.listResourcesWithProperty(Omn_lifecycle.parentTo);
        while(iterator.hasNext()){
          String adapterInstance = iterator.nextResource().getLocalName();
          Config.getInstance(adapterInstance.concat(".properties")).setDefaultProperty();
        }
    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    Message message = MessageUtil.createRDFMessage(model, IMessageBus.TYPE_INFORM, null, serialization , requestID, context);
    context.createProducer().send(topic, message);
  }
  
  private void handleDelete(Model model) {
    try {
      StmtIterator iter = model.listStatements();
      while(iter.hasNext()){
        TripletStoreAccessor.deleteResource(iter.next().getSubject());
      }      
    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
}