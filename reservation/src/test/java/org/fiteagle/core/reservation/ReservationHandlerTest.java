package org.fiteagle.core.reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;

import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;



//@RunWith(PowerMockRunner.class)
//@PrepareForTest( { TripletStoreAccessor.class })
public class ReservationHandlerTest{
  
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


//    @Test
    public void test_createReservationModel_EmptyTopology() throws Exception {
      
      Model responseModel = ModelFactory.createDefaultModel();  
      Model requestModel = ModelFactory.createDefaultModel();
        Resource topo = requestModel.createResource("http://localhost/topology/topo1");
        topo.addProperty(RDF.type, Omn.Topology);
        
        

        //topo should be in responseModel ... even empty
        
        // TODO: find out why the test is not working with rdf jena models
        PowerMock.mockStatic(TripletStoreAccessor.class);
        EasyMock.expect(TripletStoreAccessor.exists(EasyMock.anyString())).andReturn(false);
        PowerMock.replay(TripletStoreAccessor.class);
        
        reservationHandler.createReservationModel(requestModel, responseModel);

        ResIterator resIterator = responseModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        Resource topo1 = resIterator.nextResource();

        Assert.assertEquals(topo,topo1);
        PowerMock.verifyAll();
    }


 //   @Test
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

    @Test
    public void test_getReservedResources(){
      Resource adapterInstance = ModelFactory.createDefaultModel().createResource("http://localhost/resource/adapterInstance");
      Model reservationModel = ModelFactory.createDefaultModel();
      Resource resource = reservationModel.createResource("http://localhost/resource/resource1/xxxx");
      resource.addProperty(RDF.type, adapterInstance);
      resource.addProperty(Omn_lifecycle.implementedBy, adapterInstance);
      
      reservationHandler.getReservedResources(reservationModel, adapterInstance, adapterInstance);
      
      Assert.assertEquals(1, reservationHandler.getReservedResources(reservationModel, adapterInstance, adapterInstance));
    }
    
    
    @Test
    public void test_getNumberOfSameResourceType(){
      Model requestModel = ModelFactory.createDefaultModel();
      Resource requestedResource = requestModel.createResource("http://localhost/resource/resource1/xxxx");
      Resource resourceType = requestModel.createResource("http://localhost/resource/resource1");
      requestedResource.addProperty(RDF.type, resourceType);
      
      Assert.assertEquals(1, reservationHandler.getNumberOfSameResourceType(requestedResource, requestModel));
      
    }
    
    @Test
    public void test_getNumOfSameResFromSameAdapter(){
      Model requestModel = ModelFactory.createDefaultModel();
      Resource resourceType = requestModel.createResource("http://localhost/resource/resource1");
      Resource adapterInstance = requestModel.createResource("http://localhost/resource/adapterInstance1");
      Resource requestedResource = requestModel.createResource("http://localhost/resource/resource1/xxxx");
      requestedResource.addProperty(RDF.type, resourceType);
      requestedResource.addProperty(Omn_lifecycle.implementedBy, adapterInstance);
      
      Assert.assertEquals(1, reservationHandler.getNumOfSameResFromSameAdapter(requestedResource, requestModel, adapterInstance));
      
    }
    
//    @Test
    public void test_Mock() throws Exception{

      Model model = ModelFactory.createDefaultModel();

      PowerMock.mockStatic(TripletStoreAccessor.class);
      EasyMock.expect(TripletStoreAccessor.exists(EasyMock.anyString())).andReturn(false);
      PowerMock.replay(TripletStoreAccessor.class);
      
      Assert.assertEquals(false, testTriple("http://localhost"));
    }

    private boolean testTriple(String uri){
      return reservationHandler.ttt(uri);
    }
    
    

}