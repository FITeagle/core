/*
package org.fiteagle.core.orchestrator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import org.easymock.*;
import org.fiteagle.core.orchestrator.dm.OrchestratorStateKeeper;
import org.fiteagle.core.orchestrator.dm.Request;
import org.fiteagle.core.orchestrator.dm.RequestContext;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest( { TripletStoreAccessor.class })
public class ConfigurationHandlerTest  extends EasyMockSupport{

    RequestContext context = new RequestContext("someID");

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);
    @Mock
    OrchestratorStateKeeper stateKeeper;

    @TestSubject
    private final ConfigurationHandler configurationHandler = new ConfigurationHandler();

    @Test
    public void testParseEmptyModel() throws Exception {
        Model m = ModelFactory.createDefaultModel();
        createStatekeeperMock();
        configurationHandler.parseModel(context, m);
        Assert.assertNotNull(context.getRequestMap());
    }

    @Test
    public void testParseSingleResourceModel() throws Exception {

        Model m = ModelFactory.createDefaultModel();
        String resourceName = "http://testnamespace.de/test#Test1";
        String adapterName = "http://testnamespace.de/test#Adapter";
        Resource adapter = m.createResource(adapterName);
        adapter.addProperty(RDF.type, Omn.Component);

        Resource resource = m.createResource(resourceName);
        resource.addProperty(RDF.type, Omn.Resource);
        resource.addProperty(Omn_lifecycle.implementedBy, adapter);
        Assert.assertNotNull(m.getResource(resourceName));
        createStatekeeperMock();
        configurationHandler.parseModel(context, m);
        Assert.assertEquals(1, context.getRequestMap().size());

        Request request = context.getRequest(resource);
        Assert.assertEquals(resource, request.getResourceList().get(0));

    }

    private void createStatekeeperMock() {
        PowerMock.mockStatic(TripletStoreAccessor.class);
        EasyMock.expect(TripletStoreAccessor.getResource(EasyMock.anyString())).andReturn(null);
        PowerMock.replay(TripletStoreAccessor.class);
        stateKeeper.addRequest(EasyMock.anyObject(Request.class));
        EasyMock.expectLastCall().anyTimes();
        replayAll();
    }

    @Test
    public void testParseTwoResourcesModel() {
        Model m = ModelFactory.createDefaultModel();
        String resourceName = "http://testnamespace.de/test#Test1";
        String resourceName2 = "http://testnamespace.de/test#Test2";
        String adapterName = "http://testnamespace.de/test#Adapter";
        String adapterName2 = "http://testnamespace.de/test#Adapter2";

        Resource adapter = m.createResource(adapterName);
        adapter.addProperty(RDF.type, Omn.Component);
        Resource adapter2 = m.createResource(adapterName2);
        adapter.addProperty(RDF.type, Omn.Component);

        Resource resource = m.createResource(resourceName);
        resource.addProperty(RDF.type, Omn.Resource);
        resource.addProperty(Omn_lifecycle.implementedBy, adapter);
        Resource resource2 = m.createResource(resourceName2);
        resource2.addProperty(RDF.type, Omn.Resource);
        resource2.addProperty(Omn_lifecycle.implementedBy, adapter2);
        createStatekeeperMock();
        configurationHandler.parseModel(context, m);
        Assert.assertEquals(2, context.getRequestMap().size());
    }

    @Test
    public void testParseTwoResourcesImplementedBySameAdapter() {
        Model m = ModelFactory.createDefaultModel();
        String resourceName = "http://testnamespace.de/test#Test1";
        String resourceName2 = "http://testnamespace.de/test#Test2";
        String adapterName = "http://testnamespace.de/test#Adapter";
        Resource adapter = m.createResource(adapterName);
        adapter.addProperty(RDF.type, Omn.Component);
        Resource resource = m.createResource(resourceName);
        resource.addProperty(RDF.type, Omn.Resource);
        resource.addProperty(Omn_lifecycle.implementedBy, adapter);
        Resource resource2 = m.createResource(resourceName2);
        resource2.addProperty(RDF.type, Omn.Resource);
        resource2.addProperty(Omn_lifecycle.implementedBy, adapter);
        createStatekeeperMock();
        configurationHandler.parseModel(context, m);
        Assert.assertEquals(1, context.getRequestMap().size());
    }
}*/
