package org.fiteagle.core.reservation;

import java.util.UUID;

import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class ReservationHandlerTest {


    private ReservationHandler reservationHandler;

    @Before
    public void setup(){
        this.reservationHandler = new ReservationHandler();
    }
    
    @org.junit.Test
    public void test_checkReservationRequest_EmptyModel() throws Exception {

        Model requestModel = ModelFactory.createDefaultModel();
        String errorMessage = reservationHandler.checkReservationRequest(requestModel);

        Assert.assertNotNull(errorMessage);
    }

    @Test
    public void test_createReservationModel_EmptyTopology() throws TripletStoreAccessor.ResourceRepositoryException {
        Model requestModel = ModelFactory.createDefaultModel();
        Resource topo = requestModel.createResource("http://localhost/topology/topo1");
        topo.addProperty(RDF.type, Omn.Topology);
        
        Model responseModel = ModelFactory.createDefaultModel();

        //topo should be in responseModel ... even empty
        reservationHandler.createReservationModel(requestModel, responseModel);

        ResIterator resIterator = responseModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        Resource topo1 = resIterator.nextResource();

        Assert.assertEquals(topo,topo1);

    }

    @Test
    public void test_createReservationModel_SingleResourceTopology()throws  Exception {
        Model requestModel = ModelFactory.createDefaultModel();
        Resource topo = requestModel.createResource("http://localhost/topology/topo1");
        topo.addProperty(RDF.type, Omn.Topology);
        Resource resource = requestModel.createResource("http://localhost/resource/resource1");
        resource.addProperty(RDF.type, Omn.Resource);
        resource.addProperty(Omn_lifecycle.implementedBy, "http://localhost/resource/adapterInstance");
        topo.addProperty(Omn.hasResource, resource);
        resource.addProperty(Omn.isResourceOf,topo);
        
        Model responseModel = ModelFactory.createDefaultModel();

        reservationHandler.createReservationModel(requestModel, responseModel);
        reservationHandler.reserve(responseModel);

        ResIterator resIterator = responseModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        Resource topo1 = resIterator.nextResource();
        Resource resource1 = topo1.getProperty(Omn.hasResource).getObject().asResource();
        Assert.assertEquals(topo,topo1);
        Assert.assertNotSame(resource.getURI(),resource1.getURI());

        
        Assert.assertNotNull(resource1.getProperty(Omn.hasReservation));

    }



}