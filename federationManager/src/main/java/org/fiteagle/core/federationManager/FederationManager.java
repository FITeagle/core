package org.fiteagle.core.federationManager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.vocabulary.Omn_federation;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class FederationManager {


    private static final Logger LOGGER = Logger.getLogger(FederationManager.class.getName());

    @javax.annotation.PostConstruct
    public void setup() {

        LOGGER.log(Level.INFO, "I am starting");

     Model federationModel = OntologyModelUtil.loadModel("ontologies/defaultFederation.ttl", IMessageBus.SERIALIZATION_TURTLE);
      try {
        ResIterator iter = federationModel.listSubjectsWithProperty(RDF.type, Omn_federation.Infrastructure);
        while (iter.hasNext()) {
          Resource resource = iter.nextResource();
          TripletStoreAccessor.addResource(resource);
        }
      } catch (TripletStoreAccessor.ResourceRepositoryException e) {
        LOGGER.log(Level.SEVERE, e.getMessage());
      }
    }
}
