package org.fiteagle.core.repo;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ResourceInformListener {
    
    private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/data";
    
    private static Logger LOGGER = Logger.getLogger(ResourceInformListener.class.toString());
    
    
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
    
    public synchronized static boolean addInformToRepository(Model modelInform){
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

        Model currentModel = accessor.getModel();
        
        StmtIterator stmtIterator = modelInform.listStatements();
        while (stmtIterator.hasNext()) {
            Statement currentStatement = stmtIterator.nextStatement();
            currentModel.removeAll(currentStatement.getSubject(), currentStatement.getPredicate(),null);
            currentModel.add(currentStatement);
        }
        accessor.putModel(currentModel);
        
        ResourceInformListener.LOGGER.log(Level.INFO, "Updated Fuseki Service");

        return true;        
    }
    
    public synchronized static boolean releaseResource(Resource rscToRemove){
        
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);

        Model currentModel = accessor.getModel();
        
        currentModel.removeAll(rscToRemove, null,null);

        accessor.putModel(currentModel);        

        return true;        
    }
    
    
    

}
