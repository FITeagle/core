package org.fiteagle.core.tripletStoreAccessor;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageUtil.ParsingException;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.junit.Test;

import static org.junit.Assert.*;

import com.hp.hpl.jena.rdf.model.Model;

public class TripletStoreAccessorTest {
  
  @Test
  public void testHandleSPARQLRequestTurtle() throws ParsingException {
    String serializedModel = MessageUtil.createSerializedSPARQLQueryModel("SELECT * WHERE {?r ?s ?p}", IMessageBus.SERIALIZATION_TURTLE);
    Model requestModel = MessageUtil.parseSerializedModel(serializedModel, IMessageBus.SERIALIZATION_TURTLE);
    String result = null;
    try {
      result = TripletStoreAccessor.handleSPARQLRequest(requestModel, IMessageBus.SERIALIZATION_TURTLE);
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
    String serializedModel = MessageUtil.createSerializedSPARQLQueryModel("SELECT * WHERE {?r ?s ?p}", IMessageBus.SERIALIZATION_JSONLD);
    Model requestModel = MessageUtil.parseSerializedModel(serializedModel, IMessageBus.SERIALIZATION_JSONLD);
    String result = null;
    try {
      result = TripletStoreAccessor.handleSPARQLRequest(requestModel, IMessageBus.SERIALIZATION_JSONLD);
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
