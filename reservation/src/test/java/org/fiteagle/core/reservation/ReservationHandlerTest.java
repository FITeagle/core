package org.fiteagle.core.reservation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.vocabulary.Omn;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class ReservationHandlerTest {

/*

    private ReservationHandler reservationHandler;

    @Before
    public void setup(){
        this.reservationHandler = new ReservationHandler();
    }
    @org.junit.Test
    public void testHandleReservationEmptyModel() throws Exception {

        Model requestModel = ModelFactory.createDefaultModel();
        Model reservationModel = reservationHandler.handleReservation(requestModel);

        Assert.assertNotNull(reservationModel);
    }

    @Test
    public void testHandleReservationEmptyTopology() throws TripletStoreAccessor.ResourceRepositoryException {
        Model requestModel = ModelFactory.createDefaultModel();
        Resource topo = requestModel.createResource("http://localhost/topology/topo1");
        topo.addProperty(RDF.type, Omn.Topology);

        //topo should be in responseModel ... even empty
        Model responseModel =  reservationHandler.handleReservation(requestModel);

        ResIterator resIterator = responseModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        Resource topo1 = resIterator.nextResource();

        Assert.assertEquals(topo,topo1);

    }

    @Test
    public void testHandleReservationSingleResourceTopology()throws  Exception {
        Model requestModel = ModelFactory.createDefaultModel();
        Resource topo = requestModel.createResource("http://localhost/topology/topo1");
        topo.addProperty(RDF.type, Omn.Topology);
        Resource resource = requestModel.createResource("http://localhost/resource/resource1");
        resource.addProperty(RDF.type, Omn.Resource);
        topo.addProperty(Omn.hasResource, resource);
        resource.addProperty(Omn.isResourceOf,topo);

        Model responseModel =  reservationHandler.handleReservation(requestModel);

        ResIterator resIterator = responseModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        Resource topo1 = resIterator.nextResource();
        Resource resource1 = topo1.getProperty(Omn.hasResource).getObject().asResource();
        Assert.assertEquals(topo,topo1);
        Assert.assertNotEquals(resource.getURI(),resource1.getURI());

        Assert.assertNotNull(resource1.getProperty(Omn.hasReservation));

    }
*/


}