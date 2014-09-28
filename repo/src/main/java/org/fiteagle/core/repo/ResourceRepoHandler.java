package org.fiteagle.core.repo;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
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

    // public synchronized static boolean addInformToRepository(String resourceDescription, String serialization){
    //
    // // create an empty model
    // Model modelToAdd = ModelFactory.createDefaultModel();
    // InputStream is = new ByteArrayInputStream( resourceDescription.getBytes() );
    // // read the RDF/XML file
    // modelToAdd.read(is, null, serialization);
    //
    // DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
    //
    // // Download the updated model
    // Model currentModel = accessor.getModel();
    // currentModel.add(modelToAdd);
    // accessor.add(currentModel);
    //
    // System.err.println("Sucess writing to fuseki");
    //
    // return true;
    // }

    public synchronized Model handleRequest(Model modelRequests) {
        
        System.err.println("hlandling request");

        Model responseModel = ModelFactory.createDefaultModel();

        try {
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
            Model tripletStoreModel = accessor.getModel();

            StmtIterator stmtIterator = modelRequests.listStatements();
            while (stmtIterator.hasNext()) {
                Statement currentStatement = stmtIterator.nextStatement();
                System.err.println(currentStatement.toString());

                // Check if requested object is an adapter
                if (tripletStoreModel.contains(currentStatement.getResource(), RDFS.subClassOf, MessageBusOntologyModel.classAdapter)) {
                    System.err.println("found adapter request");
                    processAdapterRequest(tripletStoreModel, responseModel, currentStatement);
                }

                // Check if requested object is a testbed
                // :FITEAGLE_Testbed rdf:type fiteagle:Testbed.
                else if (tripletStoreModel.contains(currentStatement.getSubject(), RDF.type, MessageBusOntologyModel.classTestbed)) {
                    System.err.println("found testbed request");
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
            ResourceRepoHandler.LOGGER.log(Level.INFO, this.toString() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
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
                    // System.err.println("in: " + adapterPropertiesIterator.next());
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
            ResourceRepoHandler.LOGGER.log(Level.INFO, this.toString() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
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
            ResourceRepoHandler.LOGGER.log(Level.INFO, this.toString() + " : Cannot connect to FUSEKI at " + FUSEKI_SERVICE);
            return false;
        }

        return true;
    }

    // private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/data"; //query

    // private String queryDB(String query, String serialization) {
    //
    // try {
    //
    // DatasetAccessor dataAccessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
    // Model model = dataAccessor.getModel();
    //
    // QueryExecution queryExec = QueryExecutionFactory.create(QueryFactory.create(query), model);
    // ResultSet result = queryExec.execSelect();
    //
    // Model resultModel = result.getResourceModel();
    //
    // StringWriter writer = new StringWriter();
    // resultModel.write(writer, serialization);
    //
    // return writer.toString();
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // return "found no data in the repository";
    //
    // }

}
