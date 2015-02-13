package org.fiteagle.core.orchestrator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.org.apache.xpath.internal.operations.Mod;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import org.fiteagle.core.orchestrator.dm.OrchestratorStateKeeper;
import org.fiteagle.core.orchestrator.dm.Request;
import org.fiteagle.core.orchestrator.dm.RequestContext;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dne on 12.02.15.
 */
@Stateless
public class ConfigurationHandler {
    @Inject
    OrchestratorStateKeeper stateKeeper;

    public void parseModel(RequestContext context, Model requestModel) {
        List<Request> returnList = new LinkedList<>();

        Model requestedResources = getRequestedResources(requestModel);

        ResIterator resIterator = requestedResources.listSubjectsWithProperty(Omn_lifecycle.implementedBy);

        while (resIterator.hasNext()) {
            Resource requestedResource = resIterator.nextResource();
            String target = requestedResource.getProperty(Omn_lifecycle.implementedBy).getObject().asResource().getURI();
            Request request = context.getRequestByTarget(target);
            request.addResource(requestedResource);
            stateKeeper.addRequest(request);

        }

    }

    private Model getRequestedResources(Model requestModel) {
        Model returnModel = ModelFactory.createDefaultModel();
        ResIterator resIterator = requestModel.listSubjectsWithProperty(RDF.type, Omn.Resource);

        while (resIterator.hasNext()) {
            Resource requestedResource = resIterator.nextResource();
            Model resourceModel = TripletStoreAccessor.getResource(requestedResource.getURI());

            returnModel.add(resourceModel);


        }
        return returnModel;

    }


    protected void setStateKeeper(OrchestratorStateKeeper stateKeeper) {
        this.stateKeeper = stateKeeper;
    }

}
