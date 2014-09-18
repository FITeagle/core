package org.fiteagle.core.repo.dm;

/**
 * Created by vju on 9/9/14.
 */

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;
import java.io.InputStream;
import java.lang.Exception;import java.lang.IllegalArgumentException;import java.lang.String;import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Bean sends the ontology Model over message bus on startup, currently only the motor-ontology
 */
@Singleton
@Startup
public class OntologyModelInformerBean {
    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    private static Logger LOGGER = Logger.getLogger(OntologyModelInformerBean.class.toString());
    @PostConstruct
    public void onStartup() {
    try{
        Model motorModel = org.fiteagle.api.core.OntologyModels.getMotorModel();
        Model messageModel = MessageBusMsgFactory.createMsgInform(motorModel);
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
}
