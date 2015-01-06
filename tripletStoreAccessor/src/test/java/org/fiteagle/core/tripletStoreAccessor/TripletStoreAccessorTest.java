package org.fiteagle.core.tripletStoreAccessor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;

public class TripletStoreAccessorTest {
  
  @Test
  public void testHandleSPARQLRequestTurtle() throws ParsingException {
    String result = null;
    try {
      result = TripletStoreAccessor.handleSPARQLRequest("SELECT * WHERE {?r ?s ?p}", IMessageBus.SERIALIZATION_TURTLE);
    } catch (ResourceRepositoryException e) {
      if(e.getMessage().contains("org.apache.http.conn.HttpHostConnectException")){
        return;
      }
      else{
        fail(e.getMessage());
      }
    }
    assertNotNull(result);
    Model resultModel = MessageUtil.parseSerializedModel(result);
    assertNotNull(resultModel);
  }
  
  @Test
  public void testHandleSPARQLRequestJSON() throws ParsingException {
    String result = null;
    try {
      result = TripletStoreAccessor.handleSPARQLRequest("SELECT * WHERE {?r ?s ?p}", IMessageBus.SERIALIZATION_JSONLD);
    } catch (ResourceRepositoryException e) {
      if(e.getMessage().contains("org.apache.http.conn.HttpHostConnectException")){
        return;
      }
      else{
        fail(e.getMessage());
      }
    }
    assertNotNull(result);
  }
  
}
