package org.fiteagle.core.repo.dm;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.ResourceRepository;
import org.fiteagle.core.repo.ResourceRequestListener;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

@MessageDriven(name = "ResRepReqListener", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.REQUEST_FILTER),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResRepReqListener implements MessageListener{


	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;
	
	public ResRepReqListener() throws JMSException {
		
	}
	
	private final static Logger LOGGER = Logger
			.getLogger(ResRepReqListener.class.toString());
	
	public void onMessage(final Message rcvMessage) {
		try {
			ResRepReqListener.LOGGER.info("Received a request from type listResources");
			final String serialization = rcvMessage.getStringProperty(IMessageBus.SERIALIZATION);
			final String query = rcvMessage.getStringProperty(IMessageBus.QUERY);
			
			final String result = this.queryDB(query, serialization);
			
			final String id = rcvMessage.getJMSCorrelationID();					
			final Message message = this.context.createMessage();
			
			message.setStringProperty(IMessageBus.TYPE,
					IMessageBus.RESPONSE);
			message.setStringProperty(IMessageBus.RESULT, result);
			if (null != id)
				message.setJMSCorrelationID(id);
			this.context.createProducer().send(topic, message);
		} catch (final JMSException excep) {
			ResRepReqListener.LOGGER.log(Level.SEVERE, "error with JMS", excep);
		}

	}
	
	private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/data"; //query
	
	private String queryDB(String query, String serialization) {
		 
		try { 
    		
            DatasetAccessor dataAccessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
            Model model = dataAccessor.getModel();
            
			QueryExecution queryExec = QueryExecutionFactory.create(QueryFactory.create(query), model);
			ResultSet result = queryExec.execSelect();	

			Model resultModel = result.getResourceModel();
			
			StringWriter writer = new StringWriter();
	        resultModel.write(writer, serialization);
	        
	        return writer.toString();        
			
 		} catch (Exception e){
 			e.printStackTrace();
 		}
		return "found no data in the repository" ;
		
	}
		 
}
