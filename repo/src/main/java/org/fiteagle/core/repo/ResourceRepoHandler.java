package org.fiteagle.core.repo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Container;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ResourceRepoHandler {
  
  protected static final String FUSEKI_SERVICE = "http://localhost:3030/fiteagle/";
  protected static final String FUSEKI_SERVICE_DATA = FUSEKI_SERVICE + "data";
  protected static final String FUSEKI_SERVICE_QUERY = FUSEKI_SERVICE + "query";
  
  private static Logger LOGGER = Logger.getLogger(ResourceRepoHandler.class.toString());
  
  private static ResourceRepoHandler instance;
  
  public static synchronized ResourceRepoHandler getInstance() {
    if (instance == null){
      instance = new ResourceRepoHandler();
    }
    return instance;
  }
  
  public Model handleSPARQLRequest(Model modelRequest, String serialization) {
    Model replyModel = ModelFactory.createDefaultModel();
    String sparqlQuery = getQueryFromModel(modelRequest);
    Model resultModel = null;
    String resultJSON = "";
    
    if (!sparqlQuery.isEmpty()) {
      LOGGER.log(Level.INFO, "Processing SPARQL Query: " + sparqlQuery);
      
      if (sparqlQuery.toUpperCase().contains("SELECT")) {
        ResultSet rs = QueryExecuter.executeSparqlSelectQuery(sparqlQuery);
        resultJSON = getResultSetAsJsonString(rs);
        resultModel = ResultSetFormatter.toModel(rs);
        
      } else if (sparqlQuery.toUpperCase().contains("DESCRIBE")) {
        resultModel = QueryExecuter.executeSparqlDescribeQuery(sparqlQuery);
        resultJSON = MessageUtil.serializeModel(resultModel, IMessageBus.SERIALIZATION_JSONLD);
        
      } else if (sparqlQuery.toUpperCase().contains("CONSTRUCT")) {
        resultModel = QueryExecuter.executeSparqlConstructQuery(sparqlQuery);
        resultJSON = MessageUtil.serializeModel(resultModel, IMessageBus.SERIALIZATION_JSONLD);
      }
    } else {
      LOGGER.log(Level.SEVERE, "SPARQL Query expected, but no sparql query found!");
    }
    
    switch(serialization){
      case IMessageBus.SERIALIZATION_TURTLE:
        String resultModelSerialized = MessageUtil.serializeModel(resultModel);
        replyModel.add(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.propertyResultModelTTL, resultModelSerialized);
        break;
      case IMessageBus.SERIALIZATION_JSONLD:
        replyModel.add(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.propertyJsonResult, resultJSON);
        break;
    }
    return replyModel;
  }
  
  public synchronized Model handleRequest(Model modelRequests) {
    Model responseModel = ModelFactory.createDefaultModel();
    
    try {
      DatasetAccessor accessor = getTripletStoreAccessor();
      Model tripletStoreModel = accessor.getModel();
      
      StmtIterator stmtIterator = modelRequests.listStatements();
      while (stmtIterator.hasNext()) {
        Statement currentStatement = stmtIterator.nextStatement();
        LOGGER.log(Level.INFO, "Processing Statement: " + currentStatement.toString());
        
        // Check if requested object is an adapter
        if (tripletStoreModel.contains(currentStatement.getResource(), RDFS.subClassOf, MessageBusOntologyModel.classAdapter)) {
          processAdapterRequest(tripletStoreModel, responseModel, currentStatement);
        }
        
        // Check if requested object is a testbed
        else if (tripletStoreModel.contains(currentStatement.getSubject(), RDF.type, MessageBusOntologyModel.classTestbed)) {
          processTestbedAdapterListRequest(tripletStoreModel, responseModel, currentStatement);
        }
        
        // Check if requested object is a resource instance
        else if (tripletStoreModel.contains(currentStatement)) {
          processResourceInstanceRequest(tripletStoreModel, responseModel, currentStatement);
        }
        
        // Was this a restores message? If yes, add restores property to response
        if (currentStatement.getPredicate().equals(MessageBusOntologyModel.methodRestores)) {
          responseModel.add(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodRestores, currentStatement.getResource());
        }
        
      }
    } catch (HttpException e) {
      LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Cannot connect to FUSEKI at " + FUSEKI_SERVICE_DATA);
    }
    
    return responseModel;
  }
  
  private void processResourceInstanceRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
    StmtIterator resourcePropertiesIterator = tripletStoreModel.listStatements(currentStatement.getSubject(), null, (RDFNode) null);
    while (resourcePropertiesIterator.hasNext()) {
      responseModel.add(resourcePropertiesIterator.next());
    }
  }
  
  private void processTestbedAdapterListRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
    // Get contained adapter names
    StmtIterator testbedAdapterIterator = tripletStoreModel.listStatements(currentStatement.getSubject(), MessageBusOntologyModel.propertyFiteagleContainsAdapter, (RDFNode) null);
    while (testbedAdapterIterator.hasNext()) {
      Statement currentTestbedStatement = testbedAdapterIterator.next();
      responseModel.add(currentTestbedStatement);
      
      StmtIterator adapterIterator = tripletStoreModel.listStatements(currentTestbedStatement.getResource(), RDF.type, (RDFNode) null);
      while (adapterIterator.hasNext()) {
        Statement currentAdapterStatement = adapterIterator.next();
        responseModel.add(currentAdapterStatement);
        
        // Get properties of adapter type (e.g. motor:MotorGarageAdapter)
        StmtIterator adapterPropertiesIterator = tripletStoreModel.listStatements(
            currentAdapterStatement.getResource(), (Property) null, (RDFNode) null);
        while (adapterPropertiesIterator.hasNext()) {
          responseModel.add(adapterPropertiesIterator.next());
        }
        
      }
    }
  }
  
  private void processAdapterRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
    responseModel.add(currentStatement);
    
    // Find properties of adapter instance
    StmtIterator adapterPropertiesIterator = tripletStoreModel.listStatements(currentStatement.getSubject(),
        (Property) null, (RDFNode) null);
    while (adapterPropertiesIterator.hasNext()) {
      Statement property = adapterPropertiesIterator.next();
      responseModel.add(property);
      if (property.getObject().canAs(Container.class) && property.getObject().canAs(Bag.class)) {
        Container container = property.getObject().as(Bag.class);
        responseModel.add(container.listProperties());
        NodeIterator bagItemIterator = container.iterator();
        while (bagItemIterator.hasNext()) {
          RDFNode node = bagItemIterator.next();
          StmtIterator bagItemPropertiesIterator = tripletStoreModel.listStatements(node.asResource(), (Property) null,
              (RDFNode) null);
          while (bagItemPropertiesIterator.hasNext()) {
            responseModel.add(bagItemPropertiesIterator.next());
          }
        }
      }
    }
    
    // Find properties of adapter type
    StmtIterator adapterTypePropertiesIterator = tripletStoreModel.listStatements(currentStatement.getResource(),
        (Property) null, (RDFNode) null);
    while (adapterTypePropertiesIterator.hasNext()) {
      responseModel.add(adapterTypePropertiesIterator.next());
    }
    
    // Check what kind of resource it implements
    NodeIterator implementedResourceTypes = tripletStoreModel.listObjectsOfProperty(currentStatement.getResource(),
        MessageBusOntologyModel.propertyFiteagleImplements);
    while (implementedResourceTypes.hasNext()) {
      RDFNode implementedResource = implementedResourceTypes.next();
      
      // Get implemented resource properties
      StmtIterator implementedResourcePropertiesIterator = tripletStoreModel.listStatements(
          (Resource) implementedResource, (Property) null, (RDFNode) null);
      while (implementedResourcePropertiesIterator.hasNext()) {
        responseModel.add(implementedResourcePropertiesIterator.next());
      }
      
      StmtIterator implementedResourceLinkedPropertiesIterator = tripletStoreModel.listStatements(null, RDFS.domain,
          implementedResource);
      while (implementedResourceLinkedPropertiesIterator.hasNext()) {
        StmtIterator linkedPropertiesIterator = tripletStoreModel.listStatements(
            implementedResourceLinkedPropertiesIterator.next().getSubject(), (Property) null, (RDFNode) null);
        while (linkedPropertiesIterator.hasNext()) {
          responseModel.add(linkedPropertiesIterator.next());
        }
      }
      
      // Get implemented resource instances
      StmtIterator resourceTypeIterator = tripletStoreModel.listStatements(null, RDF.type, implementedResource);
      while (resourceTypeIterator.hasNext()) {
        
        StmtIterator resourceInstancePropertiesIterator = tripletStoreModel.listStatements(resourceTypeIterator.next()
            .getSubject(), (Property) null, (RDFNode) null);
        while (resourceInstancePropertiesIterator.hasNext()) {
          responseModel.add(resourceInstancePropertiesIterator.next());
        }
        
      }
    }
  }
  
  public synchronized boolean addInformToRepository(Model modelInform) {
    try {
      DatasetAccessor accessor = getTripletStoreAccessor();
      
      Model currentModel = accessor.getModel();
      QueryExecuter.removeProblematicNsPrefixes(currentModel);
      
      removeExistingValuesFromModel(currentModel, modelInform);
      addValuesToModel(currentModel, modelInform);
      
      accessor.putModel(currentModel);
    } catch (HttpException e) {
      LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE_DATA);
      return false;
    }
    
    return true;
  }
  
  private DatasetAccessor getTripletStoreAccessor() {
    DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE_DATA);
    if (accessor == null) {
      LOGGER.log(Level.SEVERE, "Could not connect to fuseki service at:" + FUSEKI_SERVICE_DATA);
    }
    return accessor;
  }
  
  private void removeExistingValuesFromModel(Model mainModel, Model valuesToRemove) {
    StmtIterator stmtIterator = valuesToRemove.listStatements();
    while (stmtIterator.hasNext()) {
      Statement currentStatement = stmtIterator.nextStatement();
      if (!currentStatement.getSubject().equals(MessageBusOntologyModel.internalMessage)) {
        mainModel.removeAll(currentStatement.getSubject(), currentStatement.getPredicate(), null);
      }
    }
  }
  
  private void addValuesToModel(Model mainModel, Model valuesToAdd) {
    StmtIterator stmtIterator = valuesToAdd.listStatements();
    while (stmtIterator.hasNext()) {
      Statement currentStatement = stmtIterator.nextStatement();
      if (!currentStatement.getSubject().equals(MessageBusOntologyModel.internalMessage)) {
        if (currentStatement.getPredicate().getLocalName().contains("maxInstances")) {
          Model model = ModelFactory.createDefaultModel();
          Property maxInstancesCanBeCreated = model.createProperty(currentStatement.getPredicate().getNameSpace() + "maxInstancesCanBeCreated");
          mainModel.add(currentStatement.getSubject(), maxInstancesCanBeCreated, currentStatement.getObject());
        }
        mainModel.add(currentStatement);
      }
    }
  }
  
  public synchronized boolean releaseResource(Resource rscToRemove) {
    try {
      DatasetAccessor accessor = getTripletStoreAccessor();
      
      Model currentModel = accessor.getModel();
      QueryExecuter.removeProblematicNsPrefixes(currentModel);
      
      currentModel.removeAll(rscToRemove, null, null);
      
      accessor.putModel(currentModel);
      
    } catch (HttpException e) {
      LOGGER.log(Level.SEVERE, this.toString() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE_DATA);
      return false;
    }
    return true;
  }
  
  public String getQueryFromModel(Model model) {
    StmtIterator iter = model.listStatements(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest);
    Statement currentStatement = null;
    Statement rdfsComment = null;
    String sparqlQuery = "";
    while (iter.hasNext()) {
      currentStatement = iter.nextStatement();
      rdfsComment = currentStatement.getSubject().getProperty(MessageBusOntologyModel.propertySparqlQuery);
      if (rdfsComment != null) {
        sparqlQuery = rdfsComment.getObject().toString();
        break;
      }
    }
    return sparqlQuery;
  }
  
  public static String parseResultSetToJsonString(ResultSet resultSet) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ResultSetFormatter.outputAsJSON(baos, resultSet);
    String jsonString = "";
    try {
      jsonString = baos.toString(Charset.defaultCharset().toString());
    } catch (UnsupportedEncodingException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    return jsonString;
  }
  
  public static String parseResultSetToModel(ResultSet resultSet) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Model resultModel = ResultSetFormatter.toModel(resultSet);
    String rdfString = MessageUtil.serializeModel(resultModel);
    try {
      rdfString = baos.toString(Charset.defaultCharset().toString());
    } catch (UnsupportedEncodingException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    return rdfString;
  }
  
  public static String getResultSetAsJsonString(ResultSet resultSet) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ResultSetFormatter.outputAsJSON(baos, resultSet);
    String jsonString = "";
    try {
      jsonString = baos.toString(Charset.defaultCharset().toString());
      baos.close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    return jsonString;
  }
  
}
