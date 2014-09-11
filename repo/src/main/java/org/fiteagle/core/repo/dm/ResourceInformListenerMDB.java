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
import org.fiteagle.api.core.MessageBusOntologyModel;
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

    public void onMessage(final Message message) {

        try {

            if (message.getStringProperty(IMessageBus.METHOD_TYPE) != null && message.getStringProperty(IMessageBus.RDF) != null) {
                String result = "";

                if (message.getStringProperty(IMessageBus.METHOD_TYPE).equals(IMessageBus.TYPE_INFORM)) {

                    String inputRDF = message.getStringProperty(IMessageBus.RDF);

                    // System.err.println("Input RDF: " + inputRDF);
                    // create an empty model
                    Model modelInform = ModelFactory.createDefaultModel();

                    InputStream is = new ByteArrayInputStream(inputRDF.getBytes());

                    try {

                        // read the RDF/XML file
                        modelInform.read(is, null, message.getStringProperty(IMessageBus.SERIALIZATION));

                        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
                        StmtIterator iter = modelInform.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleInform));
                        Statement currentStatement = null;
                        while (iter.hasNext()) {
                            currentStatement = iter.nextStatement();
                            // Resource currentResource = iter2.nextStatement().getSubject();
                            // System.err.println("+" + currentStatement.toString());

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

                            checkForReleases(modelInform);

                            // modelInform.write(System.err, "TURTLE");
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

    private void checkForReleases(Model modelInform) {
        List<Statement> releasesStatementsToRemove = new LinkedList<Statement>();

        // Do this manually, so the fiteagle:Inform Statement can be removed from the graph later
        StmtIterator iter = modelInform.listStatements();
        Statement currentStatement = null;
        while (iter.hasNext()) {
            currentStatement = iter.nextStatement();

            if (currentStatement.getPredicate().equals(MessageBusOntologyModel.methodReleases)) {
                ResourceInformListener.releaseResource(currentStatement.getResource());
                System.err.println("RDF Repo: Removing resource: " + currentStatement.getResource());
                releasesStatementsToRemove.add(currentStatement);
                
            }
        }
        
        for (Statement statement : releasesStatementsToRemove) {
            modelInform.remove(statement);
        }

    }

}
