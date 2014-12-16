package org.fiteagle.core.repo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

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
  
  public String handleSPARQLRequest(Model requestModel, String serialization) {
    String sparqlQuery = getQueryFromModel(requestModel);
    Model resultModel = null;
    String resultJSON = "";
    
    LOGGER.log(Level.INFO, "Processing SPARQL Query:\n" + sparqlQuery);
    
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
    
    switch(serialization){
      case IMessageBus.SERIALIZATION_TURTLE:
        return MessageUtil.serializeModel(resultModel);
      case IMessageBus.SERIALIZATION_JSONLD:
        return resultJSON;
    }
    return null;
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
    if (sparqlQuery.isEmpty()) {
      LOGGER.log(Level.SEVERE, "SPARQL Query expected, but no sparql query found!");
    }
    return sparqlQuery;
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
