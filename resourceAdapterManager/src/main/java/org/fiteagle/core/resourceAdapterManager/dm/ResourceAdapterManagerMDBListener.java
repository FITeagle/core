package org.fiteagle.core.resourceAdapterManager.dm;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.TimerHelper;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@MessageDriven(name = "ResourceAdapterManagerMDBListener", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = MessageFilters.FILTER_RESOURCE_ADAPTER_MANAGER),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceAdapterManagerMDBListener implements MessageListener {
  
  private static Logger LOGGER = Logger.getLogger(ResourceAdapterManagerMDBListener.class.toString());
  
  @Inject
  private TimerHelper helper;
  @Inject
  private JMSContext context;
  @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  public void onMessage(final Message message) {
    String messageType = MessageUtil.getMessageType(message);
    String serialization = MessageUtil.getMessageSerialization(message);
    String rdfString = MessageUtil.getStringBody(message);
    LOGGER.log(Level.INFO, "Received a " + messageType + " message");
    try {
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
    }catch (ResourceRepositoryException e) {
        e.printStackTrace();
    } catch (InvalidModelException e) {
        e.printStackTrace();
    }

  }

  private void handleGet(Message message, String serialization, String requestID) {
    Message responseMessage = null;

      String serializedResponse = null;

      try {
          serializedResponse = TripletStoreAccessor.getResources();
      } catch (ResourceRepositoryException e) {
          LOGGER.log(Level.SEVERE, "Could not get resource", e );
      }

      responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);

      context.createProducer().send(topic, responseMessage);

  }
  
  private void handleCreate(Model model, String serialization, String requestID) {
    ResIterator resIterator = model.listSubjectsWithProperty(Omn_lifecycle.canImplement);
    while (resIterator.hasNext()) {
      Resource resource = resIterator.next();
/*      try {*/
    helper.setNewTimer(new ResourceClass(resource));
//        TripletStoreAccessor.addResource(resource);
      /*} catch (ResourceRepositoryException e) {
        LOGGER.log(Level.SEVERE, "Could not add " + resource, e);
      }*/
    }
    Message message = MessageUtil.createRDFMessage(model, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
    context.createProducer().send(topic, message);
  }
  
  private void handleDelete(Model model) throws InvalidModelException, ResourceRepositoryException {
        TripletStoreAccessor.deleteModel(model);
  }
  
  
	class ResourceClass implements Callable<Void> {
		Resource resource;
		
		public ResourceClass(Resource resource){
		this.resource = resource;
		}
		
		@Override
		public Void call() throws ResourceRepositoryException, HttpException {	
			TripletStoreAccessor.addResource(resource);
			try {
				this.finalize();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
}
  
}