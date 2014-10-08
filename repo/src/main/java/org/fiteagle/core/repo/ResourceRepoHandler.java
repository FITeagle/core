package org.fiteagle.core.repo;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryFactory;
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

    public Model handleSPARQLRequest(Model modelRequest){
        
        Model resultModel = ModelFactory.createDefaultModel();
        String sparqlQuery = getQueryFromModel(modelRequest);
        String jsonString = "";

        // This is a request message, so query the database with the given sparql query
        if (!sparqlQuery.isEmpty()) {
            try{
                QueryFactory.create(sparqlQuery);
                ResultSet resultSet = QueryExecuter.queryModelFromDatabase(sparqlQuery);
                jsonString = getResultSetAsJsonString(resultSet);
            }catch(QueryException e){
                LOGGER.log(Level.INFO, "Comment of message was no valid query");
            }

            resultModel.add(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.propertyJsonResult, jsonString);
        } else {
          LOGGER.log(Level.SEVERE, "SPARQL Query expected, but no sparql query found!");
        }
        
        return resultModel;
    }

    public synchronized Model handleRequest(Model modelRequests) {
        Model responseModel = ModelFactory.createDefaultModel();

        try {
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
            Model tripletStoreModel = accessor.getModel();

            StmtIterator stmtIterator = modelRequests.listStatements();
            while (stmtIterator.hasNext()) {
                Statement currentStatement = stmtIterator.nextStatement();

                // Check if requested object is an adapter
                if (tripletStoreModel.contains(currentStatement.getResource(), RDFS.subClassOf, MessageBusOntologyModel.classAdapter)) {
                    processAdapterRequest(tripletStoreModel, responseModel, currentStatement);
                }

                // Check if requested object is a testbed
                // :FITEAGLE_Testbed rdf:type fiteagle:Testbed.
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
        } catch (org.apache.jena.atlas.web.HttpException e) {
            ResourceRepoHandler.LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
        }

        return responseModel;
    }

    private void processResourceInstanceRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
        StmtIterator resourcePropertiesIterator = tripletStoreModel.listStatements(new SimpleSelector(currentStatement.getSubject(), (Property) null, (RDFNode) null));
        while (resourcePropertiesIterator.hasNext()) {
            responseModel.add(resourcePropertiesIterator.next());
        }
    }

    private void processTestbedAdapterListRequest(Model tripletStoreModel, Model responseModel, Statement currentStatement) {
        // Get contained adapter names
        // :FITEAGLE_Testbed fiteagle:containsAdapter :ADeployedMotorAdapter1.
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
            ResourceRepoHandler.LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
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
            ResourceRepoHandler.LOGGER.log(Level.SEVERE, this.toString() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
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
                LOGGER.log(Level.INFO, "SPARQL Query found " + sparqlQuery);
                break;
            }
        }
        return sparqlQuery;
    }

    /**
     * Gets the result set as json string
     *
     * @param resultSet resultset to be converted to json format
     * @return String containing the converted resultset in json format
     */
    public String getResultSetAsJsonString(ResultSet resultSet) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, resultSet);
        String jsonString = "";
    try {
      jsonString = baos.toString(Charset.defaultCharset().toString());
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
        return jsonString;
    }

}
