package org.fiteagle.core.resourceAdapterManager;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import info.openmultinet.ontology.vocabulary.Wgs84;
import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_federation;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;


/**
 * Created by dne on 18.09.15.
 */
@Startup
@Singleton
public class ResourceAdapterManager {
    @javax.annotation.Resource
    public TimerService timerService;

    @Inject
    private JMSContext context;
    @javax.annotation.Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;
    private boolean initialized;
    private int failureCounter;

    private static Logger LOGGER = Logger.getLogger(ResourceAdapterManager.class.getName());
    private Resource infrastructure;
    private Stack<Message> messageStore;

    @PostConstruct
    public void initialize(){
        initialized = false;
        messageStore = new Stack<>();
        runSetup();

    }


    public void runSetup() {

        if (!initialized) {
            timerService.createIntervalTimer(0, 5000, new TimerConfig());
        }
    }

    @Timeout
    public void timerMethod(Timer timer) {
        if (failureCounter < 10) {
            try {

                 Model infModel = TripletStoreAccessor.getInfrastructure();
                ResIterator resIter = infModel.listResourcesWithProperty(RDF.type, Omn_federation.Infrastructure);
                while (resIter.hasNext()){
                    infrastructure = resIter.nextResource();
                }

                if(infrastructure != null){
                    initialized = true;
                    handleStoredMessages();
                    timer.cancel();
                }

            } catch (HttpException e) {
                LOGGER.info(
                        "Couldn't find RDF Database - will try again");
                failureCounter++;
            } catch (TripletStoreAccessor.ResourceRepositoryException e) {

                LOGGER.info(
                        "Errored while reading Database - will try again");
                failureCounter++;

            }
        } else {
            LOGGER.severe(
                    "Tried read Database several times, but failed. Please check the OpenRDF-Database");
        }

    }

    private void handleStoredMessages() {
       while(!messageStore.empty()){
           handleMessage(messageStore.pop());
       }
    }

    public void create(Model model) {
        ResIterator resIterator = model.listSubjectsWithProperty(Omn_lifecycle.canImplement);
        while (resIterator.hasNext()) {
            Resource resource = resIterator.next();
            infrastructure.addProperty(Omn.hasResource, resource);
            try {
        	LOGGER.info("START: Adding adapter");
        	LOGGER.fine("CONTENT: \n" + OntologyModelUtil.toString(resource.getModel()));
        	
        	addDefaultGeoInformation(model);
        	        	
                TripletStoreAccessor.addResource(resource);
                TripletStoreAccessor.updateModel(infrastructure.getModel());
                LOGGER.info("END: Adding adapter");
            } catch (TripletStoreAccessor.ResourceRepositoryException e) {
                LOGGER.log(Level.INFO, "Could not add " + resource, e);
            } catch (InvalidModelException e) {
        	LOGGER.log(Level.INFO, "Could not add " + resource, e);
            }
        }
    }


    public void addDefaultGeoInformation(Model model) {
	LOGGER.info("Looking for GEO information");
	ResIterator adapters = model.listSubjectsWithProperty(Omn_lifecycle.implements_);
	while (adapters.hasNext()) {
	    Resource adapter = adapters.next();
	    LOGGER.info("Checking: " + adapter.getURI());
	    if (null == adapter.getProperty(Wgs84.lat)) {
		RDFNode globalLat = infrastructure.getProperty(Wgs84.lat).getObject();
	    	RDFNode globalLong = infrastructure.getProperty(Wgs84.long_).getObject();
	    	LOGGER.info("Adding: " + globalLat + ", " + globalLong);
		adapter.addProperty(Wgs84.lat, globalLat);
		adapter.addProperty(Wgs84.long_, globalLong);
	    }        	    
	}
    }

    public void delete(Model model) throws InvalidModelException, TripletStoreAccessor.ResourceRepositoryException {
	LOGGER.info("START: Removing adapter: " + OntologyModelUtil.toString(model));
        TripletStoreAccessor.deleteModel(model);
    }

    public String get() {
        String response = "";
        try {
           response =  TripletStoreAccessor.getResources();
        } catch (TripletStoreAccessor.ResourceRepositoryException e) {
            LOGGER.log(Level.INFO, "Could not get resource", e);
        }

        return response;
    }

    public boolean initialized() {
        return initialized;
    }

    public void storeMessage(Message message) {
        this.messageStore.push(message);
    }

    public void handleMessage(Message message) {
        String messageType = MessageUtil.getMessageType(message);
        String serialization = MessageUtil.getMessageSerialization(message);
        String rdfString = MessageUtil.getStringBody(message);
        LOGGER.info("Received a " + messageType + " message");
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
        } catch (TripletStoreAccessor.ResourceRepositoryException e) {
            e.printStackTrace();
        } catch (InvalidModelException e) {
            e.printStackTrace();
        }
    }

    private void handleGet(Message message, String serialization, String requestID) {
        Message responseMessage = null;

        String serializedResponse = this.get();


        responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);

        context.createProducer().send(topic, responseMessage);

    }

    private void handleCreate(Model model, String serialization, String requestID) {

        this.create(model);

        Message message = MessageUtil.createRDFMessage(model, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
        context.createProducer().send(topic, message);
    }

    private void handleDelete(Model model) throws InvalidModelException, TripletStoreAccessor.ResourceRepositoryException {

        this.delete(model);

    }
}
