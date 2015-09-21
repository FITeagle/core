package org.fiteagle.core.resourceAdapterManager.dm;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageFilters;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.core.resourceAdapterManager.ResourceAdapterManager;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

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


    @Inject
    ResourceAdapterManager resourceAdapterManager;

   //TODO add message queue for for incoming messages which raise exception
    
  public void onMessage(final Message message) {
      if(!resourceAdapterManager.initialized()){
          resourceAdapterManager.storeMessage(message);
      }else {

          resourceAdapterManager.handleMessage(message);

      }
  }


  
}