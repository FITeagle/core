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
    
    
//    public synchronized static boolean addInformToRepository(String resourceDescription, String serialization){
//        
//        // create an empty model
//        Model modelToAdd = ModelFactory.createDefaultModel();
//        InputStream is = new ByteArrayInputStream( resourceDescription.getBytes() );        
//        // read the RDF/XML file
//        modelToAdd.read(is, null, serialization);
//        
//        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
//
//     // Download the updated model
//        Model currentModel = accessor.getModel();
//        currentModel.add(modelToAdd);
//        accessor.add(currentModel);
//        
//        System.err.println("Sucess writing to fuseki");
//
//        return true;        
//    }
    
    
    public synchronized Model handleRequest(Model modelRequests){
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
        Model currentModel = accessor.getModel();
        
        Model responseModel = ModelFactory.createDefaultModel();
        
       


        
        StmtIterator stmtIterator = modelRequests.listStatements();
        while (stmtIterator.hasNext()) {
            Statement currentStatement = stmtIterator.nextStatement();
            
            // Check if it is an adapter
          //  model.listStatements(new SimpleSelector(currentStatement.getResource(), RDFS.subClassOf, MessageBusOntologyModel.classAdapter));
            
            if(currentModel.contains(currentStatement.getResource(), RDFS.subClassOf, MessageBusOntologyModel.classAdapter)){
                
                StmtIterator adapterPropertiesIterator = currentModel.listStatements(new SimpleSelector(currentStatement.getSubject(), (Property) null, (RDFNode) null)); 
                while (adapterPropertiesIterator.hasNext()) {
                    responseModel.add(adapterPropertiesIterator.next());
                }
                
                com.hp.hpl.jena.rdf.model.Resource message = responseModel.createResource("http://fiteagleinternal#Message");
                message.addProperty(MessageBusOntologyModel.methodRestores, currentStatement.getSubject());
                
                // Check what resource it implements
                NodeIterator implementedResourceTypes = currentModel.listObjectsOfProperty(currentStatement.getResource(), MessageBusOntologyModel.propertyFiteagleImplements);
                while(implementedResourceTypes.hasNext()){
                    StmtIterator resourceTypeIterator = currentModel.listStatements(new SimpleSelector(null, RDF.type, implementedResourceTypes.next())); 
                    while(resourceTypeIterator.hasNext()){
                        
                        StmtIterator resourceInstancePropertiesIterator = currentModel.listStatements(new SimpleSelector(resourceTypeIterator.next().getSubject(), (Property) null, (RDFNode) null)); 
                        while (resourceInstancePropertiesIterator.hasNext()) {
                            responseModel.add(resourceInstancePropertiesIterator.next());
                        }
                        
                    }
                }
            }
        }

        return responseModel;        
    }
    
    public synchronized boolean addInformToRepository(Model modelInform){
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

        Model currentModel = accessor.getModel();
        
        StmtIterator stmtIterator = modelInform.listStatements();
        while (stmtIterator.hasNext()) {
            Statement currentStatement = stmtIterator.nextStatement();
            currentModel.removeAll(currentStatement.getSubject(), currentStatement.getPredicate(),null);
            currentModel.add(currentStatement);
        }
        accessor.putModel(currentModel);
        
        ResourceRepoHandler.LOGGER.log(Level.INFO, "Updated Fuseki Service");

        return true;        
    }
    
    public synchronized boolean releaseResource(Resource rscToRemove){
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

        Model currentModel = accessor.getModel();
        
        currentModel.removeAll(rscToRemove, null,null);

        accessor.putModel(currentModel);        

        return true;        
    }
    
    
    

}
