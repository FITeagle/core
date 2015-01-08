package org.fiteagle.core.federationManager.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

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
  
  static{
    Model federationModel = OntologyModelUtil.loadModel("ontologies/defaultFederation.ttl", IMessageBus.SERIALIZATION_TURTLE);
    try {
      TripletStoreAccessor.updateRepositoryModel(federationModel);
    } catch (ResourceRepositoryException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private final static String TRIPLET_STORE_URL = "<http://localhost:3030/fiteagle/query> ";
  private final static String queryForFederation = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
      + "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
      + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
      + "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
      + "PREFIX av: <http://federation.av.tu-berlin.de/about#> "
      + "CONSTRUCT { ?testbed rdf:type omn:Testbed. ?testbed rdfs:label ?label. "
      + "?testbed rdfs:seeAlso ?seeAlso. ?testbed wgs:long ?long. ?testbed wgs:lat ?lat. } "
      + "FROM "
      + TRIPLET_STORE_URL
      + "WHERE {?testbed rdf:type omn:Testbed. "
      + "OPTIONAL {?testbed rdfs:label ?label. ?testbed rdfs:seeAlso ?seeAlso. ?testbed wgs:long ?long. ?testbed wgs:lat ?lat. } }";
  
  
  public void onMessage(final Message message) {
    try {
      String messageType = message.getStringProperty(IMessageBus.METHOD_TYPE);
      String serialization = message.getStringProperty(IMessageBus.SERIALIZATION);
      LOGGER.log(Level.INFO, "Received a " + messageType + " message");
      
      if (messageType != null) {
        if (messageType.equals(IMessageBus.TYPE_GET)) {
          handleGet(serialization, message.getJMSCorrelationID());
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void handleGet(String serialization, String requestID){
    String serializedFederationModel = null;
    try {
      serializedFederationModel = TripletStoreAccessor.handleSPARQLRequest(queryForFederation, serialization);
    } catch (ResourceRepositoryException | ParsingException e) {
      Message message = MessageUtil.createErrorMessage(e.getMessage(), requestID, context);
      context.createProducer().send(topic, message);
      return;
    }
    Model federationModel = MessageUtil.parseSerializedModel(serializedFederationModel, serialization);
    Message message = MessageUtil.createRDFMessage(federationModel, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
    context.createProducer().send(topic, message);
  }
  
}