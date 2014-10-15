package org.fiteagle.core.repo;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusMsgFactory;
import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ResourceRepoHandler {

    private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/data";

    private static Logger LOGGER = Logger.getLogger(ResourceRepoHandler.class.toString());

    private static ResourceRepoHandler resourceRepositoryHandlerSingleton;

    public static synchronized ResourceRepoHandler getInstance() {
        if (resourceRepositoryHandlerSingleton == null)
            resourceRepositoryHandlerSingleton = new ResourceRepoHandler();
        return resourceRepositoryHandlerSingleton;
    }

  public Model handleSPARQLRequest(Model modelRequest, String serialization) {
    Model replyModel = ModelFactory.createDefaultModel();
    String sparqlQuery = getQueryFromModel(modelRequest);
    Model resultModel = null;
    String resultJSON = null;
    
    if (!sparqlQuery.isEmpty()) {
      LOGGER.log(Level.INFO, "Processing SPARQL Query: " + sparqlQuery);
      String method = sparqlQuery.split(" ")[0].toUpperCase();
      try {
        switch (method) {
          case "SELECT":
            ResultSet resultSet = QueryExecuter.executeSparqlSelectQuery(sparqlQuery);
            resultModel = ResultSetFormatter.toModel(resultSet);
            resultJSON = getResultSetAsJsonString(resultSet);
            break;
          case "DESCRIBE":
            resultModel = QueryExecuter.executeSparqlDescribeQuery(sparqlQuery);
            break;
        }
      } catch (QueryParseException e) {
        LOGGER.log(Level.SEVERE, "Comment of message was no valid query");
      }
    } else {
      LOGGER.log(Level.SEVERE, "SPARQL Query expected, but no sparql query found!");
    }
    
    switch(serialization){
      case IMessageBus.SERIALIZATION_TURTLE:
        String resultModelSerialized = MessageBusMsgFactory.serializeModel(resultModel);
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
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
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
                
                // Check if request is get all testbeds
                else if (currentStatement.getSubject().isAnon() && currentStatement.getPredicate().equals(RDF.type) && currentStatement.getResource().equals(MessageBusOntologyModel.classTestbed)) {
                  processGetAllTestbedsRequest(tripletStoreModel, responseModel);
                }
                
                // Check if requesting for multiple objects
                else if (currentStatement.getSubject().isAnon() && !currentStatement.getPredicate().isAnon() && !currentStatement.getResource().isAnon()) {
                  addMatchingSubjectsToModel(tripletStoreModel, responseModel, currentStatement.getPredicate(), currentStatement.getResource());
                }

                // Was this a restores message? If yes, add restores property to response
                if (currentStatement.getPredicate().equals(MessageBusOntologyModel.methodRestores)) {
                    responseModel.add(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodRestores, currentStatement.getResource());
                }

            }
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
        }

        return responseModel;
    }

  private void addMatchingSubjectsToModel(Model tripletStoreModel, Model model, Property property, Resource resource){
    StmtIterator resourceIterator = tripletStoreModel.listStatements(null, property, resource);
    while (resourceIterator.hasNext()) {
      Statement resourceStatement = resourceIterator.next();
      model.add(resourceStatement);
    }
  }
  
    
    private void processGetAllTestbedsRequest(Model tripletStoreModel, Model responseModel){
        StmtIterator iterator =  tripletStoreModel.listStatements(null, RDF.type, MessageBusOntologyModel.classTestbed);
        
        while(iterator.hasNext()){
          Statement adapterStatement = iterator.next();
          responseModel.add(adapterStatement);
          System.out.println("founding a statement");
        }
      }
    
    private void processResourceInstanceRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
        StmtIterator resourcePropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(currentStatement.getSubject(), (Property) null, (RDFNode) null));
        while (resourcePropertiesIterator.hasNext()) {
            responseModel.add(resourcePropertiesIterator.next());
        }
    }

    private void processTestbedAdapterListRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
        // Get contained adapter names
        StmtIterator testbedAdapterIterator = tripletStoreModel.listStatements(new SimpleSelector(currentStatement.getSubject(), MessageBusOntologyModel.propertyFiteagleContainsAdapter,
                (RDFNode) null));
        while (testbedAdapterIterator.hasNext()) {
            Statement currentTestbedStatement = testbedAdapterIterator.next();
            responseModel.add(currentTestbedStatement);

            StmtIterator adapterIterator = tripletStoreModel.listStatements(new SimpleSelector(currentTestbedStatement.getResource(), RDF.type, (RDFNode) null));
            while (adapterIterator.hasNext()) {
                Statement currentAdapterStatement = adapterIterator.next();
                responseModel.add(currentAdapterStatement);

                // Get properties of adapter type (e.g. motor:MotorGarageAdapter)
                StmtIterator adapterPropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(currentAdapterStatement.getResource(), (Property) null, (RDFNode) null));
                while (adapterPropertiesIterator.hasNext()) {
                    responseModel.add(adapterPropertiesIterator.next());
                }

            }
        }
    }
    
    private void processAdapterRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
        responseModel.add(currentStatement);

        // Find properties of adapter instance
        StmtIterator adapterPropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(currentStatement.getSubject(), (Property) null, (RDFNode) null));
        while (adapterPropertiesIterator.hasNext()) {
            responseModel.add(adapterPropertiesIterator.next());
        }

        // Find properties of adapter type
        StmtIterator adapterTypePropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(currentStatement.getResource(), (Property) null, (RDFNode) null));
        while (adapterTypePropertiesIterator.hasNext()) {
            responseModel.add(adapterTypePropertiesIterator.next());
        }

        // Check what kind of resource it implements
        NodeIterator implementedResourceTypes = tripletStoreModel.listObjectsOfProperty(currentStatement.getResource(), MessageBusOntologyModel.propertyFiteagleImplements);
        while (implementedResourceTypes.hasNext()) {
            RDFNode implementedResource = implementedResourceTypes.next();

            // Get implemented resource properties
            StmtIterator implementedResourcePropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector((Resource) implementedResource, (Property) null, (RDFNode) null));
            while (implementedResourcePropertiesIterator.hasNext()) {
                responseModel.add(implementedResourcePropertiesIterator.next());
            }

            StmtIterator implementedResourceLinkedPropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(null, RDFS.domain, implementedResource));
            while (implementedResourceLinkedPropertiesIterator.hasNext()) {
                StmtIterator linkedPropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(implementedResourceLinkedPropertiesIterator.next().getSubject(), (Property) null,
                        (RDFNode) null));
                while (linkedPropertiesIterator.hasNext()) {
                    responseModel.add(linkedPropertiesIterator.next());
                }
            }

            // Get implemented resource instances
            StmtIterator resourceTypeIterator = tripletStoreModel.listStatements(new SimpleSelector(null, RDF.type, implementedResource));
            while (resourceTypeIterator.hasNext()) {

                StmtIterator resourceInstancePropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(resourceTypeIterator.next().getSubject(), (Property) null, (RDFNode) null));
                while (resourceInstancePropertiesIterator.hasNext()) {
                    responseModel.add(resourceInstancePropertiesIterator.next());
                }

            }
        }
    }

    public synchronized boolean addInformToRepository(Model modelInform) {
        try {
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

            Model currentModel = accessor.getModel();

            // First remove old values
            removeExistingValuesFromModel(currentModel, modelInform);

            // Now add new values
            addValuesToModel(currentModel, modelInform);

            accessor.putModel(currentModel);
        } catch (org.apache.jena.atlas.web.HttpException e) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
            return false;
        }

        return true;
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
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

            Model currentModel = accessor.getModel();

            currentModel.removeAll(rscToRemove, null, null);

            accessor.putModel(currentModel);

        } catch (org.apache.jena.atlas.web.HttpException e) {
            LOGGER.log(Level.SEVERE, this.toString() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
            return false;
        }

        return true;
    }
    
    /**
     * Get the comment section of the rdf model
     *
     * @param model
     * @return
     */
    public String getQueryFromModel(Model model) {
        StmtIterator iter = model.listStatements(new SimpleSelector(null, RDF.type, MessageBusOntologyModel.propertyFiteagleRequest));
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
    String rdfString = MessageBusMsgFactory.serializeModel(resultModel);
    try {
      rdfString = baos.toString(Charset.defaultCharset().toString());
    } catch (UnsupportedEncodingException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    return rdfString;
  }
  
  public String getResultSetAsJsonString(ResultSet resultSet) {
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

}
