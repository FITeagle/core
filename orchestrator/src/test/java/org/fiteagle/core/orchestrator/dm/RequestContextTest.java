package org.fiteagle.core.orchestrator.dm;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequestContextTest {
    RequestContext context = null;
    @Before
    public void setup(){
        this.context = new RequestContext("id");
    }
    @Test
    public void testGetRequestMap() throws Exception {

        Assert.assertNotNull(context.getRequestMap());
    }

    @Test
    public void testGetRequestContextId() throws Exception {
        Assert.assertNotNull(context.getRequestContextId());
    }
    @Test
    public void testAddRequest() throws Exception {
        Request request = new Request("requestid",null,context);
        context.addRequest(request);
    }
    @Test
    public void testGetRequest() throws Exception {
        Request request = new Request("requestid",null,context);
        context.addRequest(request);
        Assert.assertNotNull(context.getRequest("requestid"));

    }



    @Test
    public void testAllAnswersReceived() throws Exception {
        Request request = new Request("requestid",null,context);
        context.addRequest(request);
        Assert.assertFalse(context.allAnswersReceived());
        Request request1 = context.getRequest("requestid");
        request1.setHandled();
        Assert.assertTrue(context.allAnswersReceived());

    }

    @Test
    public void testAddResource() throws Exception {

        Model model = ModelFactory.createDefaultModel();
        String resourceURI = "http://test.de/test#Test1";
        String adapterURI = "http://test.de/adapter#Adapter1";

        Resource adapter = model.createResource(adapterURI);
        adapter.addProperty(RDF.type, Omn.Component);
        Resource resource = model.getResource(resourceURI);
        resource.addProperty(RDF.type, Omn.Resource);
        resource.addProperty(Omn_lifecycle.implementedBy, adapter);
        context.getRequestByTarget(adapterURI);
        Assert.assertEquals(1, context.getRequestMap().size());

    }
}