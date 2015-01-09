package org.fiteagle.core.orchestrator.dm;

import java.util.Map;
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
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.orchestrator.provision.HandleProvision;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

@MessageDriven(name = "ReservationMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_ORCHESTRATOR),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OrchestratorMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(OrchestratorMDBListener.class.toString());
  private static String OMN = "http://open-multinet.info/ontology/omn#";
  
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  public void onMessage(final Message message) {
    try {
      String messageType = message.getStringProperty(IMessageBus.METHOD_TYPE);
      String serialization = message.getStringProperty(IMessageBus.SERIALIZATION);
      String rdfString = MessageUtil.getStringBody(message);
      
      if (messageType != null && rdfString != null) {
        Model messageModel = MessageUtil.parseSerializedModel(rdfString, serialization);
        if (messageType.equals(IMessageBus.TYPE_CONFIGURE)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
          handleConfigureRequest(messageModel, serialization, message.getJMSCorrelationID());
        }
        if (messageType.equals(IMessageBus.TYPE_DELETE)) {
          LOGGER.log(Level.INFO, "Received a " + messageType + " message");
          handleDeleteRequest(messageModel, serialization, message.getJMSCorrelationID());
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private void handleConfigureRequest(Model requestModel, String serialization, String requestID)
      throws JMSException {
    LOGGER.log(Level.INFO, "handling provision request ...");
    Message responseMessage = null;
    Model resultModel = ModelFactory.createDefaultModel();

    StmtIterator iterator = requestModel.listStatements(null, RDF.type, MessageBusOntologyModel.classGroup);
    while(iterator.hasNext()){
      Model createModel = ModelFactory.createDefaultModel();
      Resource slice = iterator.next().getSubject();
      LOGGER.log(Level.INFO, "trying to provision this URN " + slice.getURI());
      Map<String, Object> reservations = null;
      try {
        reservations = HandleProvision.getReservations(slice.getURI());
      } catch (ResourceRepositoryException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      try {
        createModel = HandleProvision.createRequest(reservations);
      } catch (ResourceRepositoryException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      LOGGER.log(Level.INFO, createModel.getGraph().toString());
      String serializedModel = MessageUtil.serializeModel(createModel, IMessageBus.SERIALIZATION_TURTLE);
      LOGGER.log(Level.INFO, "message contains " + serializedModel);
      sendMessage(serializedModel, IMessageBus.TYPE_CREATE, IMessageBus.TARGET_ADAPTER);
      LOGGER.log(Level.INFO, "create message is sent to resource adapter");
    }
    
    String serializedResponse = MessageUtil.serializeModel(resultModel, serialization);
    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization,
        requestID, context);
    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
    context.createProducer().send(topic, responseMessage);
  }
  
  private void handleDeleteRequest(Model requestModel, String serialization, String requestID)
      throws JMSException {
    LOGGER.log(Level.INFO, "handling provision request ...");
//    Message responseMessage = null;
//    Model resultModel = ModelFactory.createDefaultModel();

   
    
//    String serializedResponse = MessageUtil.serializeModel(resultModel);
//    responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, serialization,
//        requestID, context);
//    LOGGER.log(Level.INFO, " a reply is sent to SFA ...");
//    context.createProducer().send(topic, responseMessage);
  }
  
 
  private void sendMessage(String model, String methodType, String methodTarget) {
    final Message request = MessageUtil.createRDFMessage(model, methodType, methodTarget, IMessageBus.SERIALIZATION_TURTLE, null, context);
    context.createProducer().send(topic, request);
/*    Message rcvMessage = MessageUtil.waitForResult(request, context, topic);
    String resultString = MessageUtil.getStringBody(rcvMessage);
    
    if(MessageUtil.getMessageType(rcvMessage).equals(IMessageBus.TYPE_ERROR)){
      if(resultString.equals(Response.Status.REQUEST_TIMEOUT.name())){
        throw new TimeoutException("Sent message ("+ methodType + ") (Target: "+methodTarget+"): "+MessageUtil.getStringBody(request));
      }
      throw new RuntimeException(resultString);
    }
    else{
      LOGGER.log(Level.INFO, "Received reply");
      return MessageUtil.parseSerializedModel(resultString, IMessageBus.SERIALIZATION_TURTLE);
    }*/
  }
  
}
