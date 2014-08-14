package org.fiteagle.core.repo.dm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.apache.jena.riot.RiotException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.ResourceInformListener;
import org.fiteagle.core.repo.ResourceRepository;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@MessageDriven(name = "ResourceInformListenerMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
        // @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = IResourceRepository.MESSAGE_FILTER),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ResourceInformListenerMDB implements MessageListener {

    private ResourceRepository repo;
    @Inject
    private JMSContext context;
    @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
    private Topic topic;

    Property propertyInform;

    @PostConstruct
    private void setup() {
        Model fiteagle = loadModel("fiteagle.owl");
        propertyInform = fiteagle.getProperty("http://fiteagle.org/ontology#Inform");
    }

    public void onMessage(final Message message) {

        try {

            if (message.getStringProperty(IMessageBus.METHOD_TYPE) != null && message.getStringProperty(IMessageBus.RDF) != null) {
                String result = "";

                if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_INFORM)) {

                    String inputRDF = message.getStringProperty(IMessageBus.RDF);

                    //System.err.println("Input RDF: " + inputRDF);
                    // create an empty model
                    Model modelInform = ModelFactory.createDefaultModel();

                    InputStream is = new ByteArrayInputStream(inputRDF.getBytes());

                    try {

                        // read the RDF/XML file
                        modelInform.read(is, null, message.getStringProperty(IMessageBus.SERIALIZATION));

                        StmtIterator iter = modelInform.listStatements(new SimpleSelector(null, RDF.type, propertyInform));
                        Statement currentStatement = null;
                        while (iter.hasNext()) {
                            currentStatement = iter.nextStatement();
                            // Resource currentResource = iter2.nextStatement().getSubject();
                            //System.err.println("+" + currentStatement.toString());

                            // System.out.println(currentResource.getProperty(RDFS.label).getObject().toString());
                            // StmtIterator iter2 = currentResource.listProperties(RDFS.domain);
                            // while (iter2.hasNext()) {
                            // System.out.println("  " + iter2.nextStatement().getObject().asLiteral().getLong());
                            //
                            // }
                        }

                        // This is an inform message, so do something with it
                        if (currentStatement != null) {
                            modelInform.remove(currentStatement);
                            //modelInform.write(System.err, "TURTLE");
                            ResourceInformListener.addInformToRepository(modelInform);
                        }
                    } catch (RiotException e) {
                        System.err.println("Invalid RDF");
                    }
                }
            }

        } catch (JMSException e) {
            System.err.println("JMSException");
        }

    }

    public static Model loadModel(String filename2) {
        Model fiteagle = ModelFactory.createDefaultModel();

        // String filename2 = "/home/leo/ontology.txt";

        // use the FileManager to find the input file
        InputStream in2 = FileManager.get().open(filename2);
        if (in2 == null) {
            throw new IllegalArgumentException("File: " + filename2 + " not found");
        }

        // read the RDF/XML file
        fiteagle.read(in2, null, "TURTLE");

        return fiteagle;
    }

}
