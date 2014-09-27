package org.fiteagle.core.repo;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fiteagle.api.core.MessageBusOntologyModel;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
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

        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
        Model currentModel = accessor.getModel();

        Model responseModel = ModelFactory.createDefaultModel();

        StmtIterator stmtIterator = modelRequests.listStatements();
        while (stmtIterator.hasNext()) {
            Statement currentStatement = stmtIterator.nextStatement();

            // Check if requested object is an adapter
            if (currentModel.contains(currentStatement.getResource(), RDFS.subClassOf, MessageBusOntologyModel.classAdapter)) {

                responseModel.add(currentStatement);

                // Find properties of adapter instance
                StmtIterator adapterPropertiesIterator = currentModel.listStatements(new SimpleSelector(currentStatement.getSubject(), (Property) null, (RDFNode) null));
                while (adapterPropertiesIterator.hasNext()) {
                    responseModel.add(adapterPropertiesIterator.next());
                }

                // Find properties of adapter type
                StmtIterator adapterTypePropertiesIterator = currentModel.listStatements(new SimpleSelector(currentStatement.getResource(), (Property) null, (RDFNode) null));
                while (adapterTypePropertiesIterator.hasNext()) {
                    responseModel.add(adapterTypePropertiesIterator.next());
                }

                // Check what kind of resource it implements
                NodeIterator implementedResourceTypes = currentModel.listObjectsOfProperty(currentStatement.getResource(), MessageBusOntologyModel.propertyFiteagleImplements);
                while (implementedResourceTypes.hasNext()) {
                    RDFNode implementedResource = implementedResourceTypes.next();
                    
                    // Get implemented resource properties
                    StmtIterator implementedResourcePropertiesIterator = currentModel.listStatements(new SimpleSelector((Resource) implementedResource, (Property) null, (RDFNode) null));
                    while (implementedResourcePropertiesIterator.hasNext()) {
                        responseModel.add(implementedResourcePropertiesIterator.next());
                    }
                    
                    StmtIterator implementedResourceLinkedPropertiesIterator = currentModel.listStatements(new SimpleSelector(null, RDFS.domain, implementedResource));
                    while (implementedResourceLinkedPropertiesIterator.hasNext()) {
                        StmtIterator linkedPropertiesIterator = currentModel.listStatements(new SimpleSelector(implementedResourceLinkedPropertiesIterator.next().getSubject(), (Property) null, (RDFNode) null));
                        while (linkedPropertiesIterator.hasNext()) {
                            responseModel.add(linkedPropertiesIterator.next());
                        }
                    }
                    
                    // Get implemented resource instances
                    StmtIterator resourceTypeIterator = currentModel.listStatements(new SimpleSelector(null, RDF.type, implementedResource));
                    while (resourceTypeIterator.hasNext()) {

                        StmtIterator resourceInstancePropertiesIterator = currentModel.listStatements(new SimpleSelector(resourceTypeIterator.next().getSubject(), (Property) null, (RDFNode) null));
                        while (resourceInstancePropertiesIterator.hasNext()) {
                            responseModel.add(resourceInstancePropertiesIterator.next());
                        }

                    }
                }
            }

            // Check if requested object is a testbed

            // :FITEAGLE_Testbed rdf:type fiteagle:Testbed.
            else if (currentModel.contains(currentStatement.getSubject(), RDF.type, MessageBusOntologyModel.classTestbed)) {

                // Get contained adapter names
                // :FITEAGLE_Testbed fiteagle:containsAdapter :ADeployedMotorAdapter1.
                StmtIterator testbedAdapterIterator = currentModel.listStatements(new SimpleSelector(currentStatement.getSubject(), MessageBusOntologyModel.propertyFiteagleContainsAdapter,
                        (RDFNode) null));
                while (testbedAdapterIterator.hasNext()) {
                    Statement currentTestbedStatement = testbedAdapterIterator.next();
                    responseModel.add(currentTestbedStatement);

                    StmtIterator adapterIterator = currentModel.listStatements(new SimpleSelector(currentTestbedStatement.getResource(), RDF.type, (RDFNode) null));
                    while (adapterIterator.hasNext()) {
                        Statement currentAdapterStatement = adapterIterator.next();
                        responseModel.add(currentAdapterStatement);

                        // Get properties of adapter type (e.g. motor:MotorGarageAdapter)
                        StmtIterator adapterPropertiesIterator = currentModel.listStatements(new SimpleSelector(currentAdapterStatement.getResource(), (Property) null, (RDFNode) null));
                        while (adapterPropertiesIterator.hasNext()) {
                            // System.err.println("in: " + adapterPropertiesIterator.next());
                            responseModel.add(adapterPropertiesIterator.next());
                        }

                    }
                }
            }
            // Check if requested object is a resource instance
            else if (currentModel.contains(currentStatement)) {
                System.err.println("got: " + currentStatement.toString());
                StmtIterator resourcePropertiesIterator = currentModel.listStatements(new SimpleSelector(currentStatement.getSubject(), (Property) null, (RDFNode) null));
                while (resourcePropertiesIterator.hasNext()) {
                    responseModel.add(resourcePropertiesIterator.next());
                }
            }

            // Was this a restores message? If yes, add restores property to response
            if (currentStatement.getPredicate().equals(MessageBusOntologyModel.methodRestores)) {
                responseModel.add(MessageBusOntologyModel.internalMessage, MessageBusOntologyModel.methodRestores, currentStatement.getResource());
            }

        }

        return responseModel;
    }

    public synchronized boolean addInformToRepository(Model modelInform) {

        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

        Model currentModel = accessor.getModel();

        // First remove old values
        removeExistingValuesFromModel(currentModel, modelInform);

        // Now add new values
        addValuesToModel(currentModel, modelInform);

        accessor.putModel(currentModel);

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

        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

        Model currentModel = accessor.getModel();

        currentModel.removeAll(rscToRemove, null, null);

        accessor.putModel(currentModel);

        return true;
    }

    // private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/data"; //query

    private String queryDB(String query, String serialization) {

        try {

            DatasetAccessor dataAccessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
            Model model = dataAccessor.getModel();

            QueryExecution queryExec = QueryExecutionFactory.create(QueryFactory.create(query), model);
            ResultSet result = queryExec.execSelect();

            Model resultModel = result.getResourceModel();

            StringWriter writer = new StringWriter();
            resultModel.write(writer, serialization);

            return writer.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "found no data in the repository";

    }

}
