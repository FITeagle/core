package org.fiteagle.core.repo;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class ResourceRequestListener {
    
    private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/data";
    
    private static Logger LOGGER = Logger.getLogger(ResourceRequestListener.class.toString());

    public static Model queryModelFromDatabase(String sparqlQuery){
  
        ResourceRequestListener.LOGGER.log(Level.INFO, "Querying Fuseki Service");
        
    	Model queryResult = submitSparqlQuery(sparqlQuery);

        return queryResult;        
    }
    
    /**
     * Submits a given sparql query string to the db
     * @param queryString String containing the sparql query to be issued
     * @return Model retrieved from db, null on failure
     */
    private static Model submitSparqlQuery(String queryString){
    	try { 
    		// Get current Model from DB
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
            Model currentModel = accessor.getModel();
            ResourceRequestListener.LOGGER.log(Level.INFO, "Remote Fuseki Model obtained");
            
            // Generate query from query string and execute it on the current Model
			Query query = QueryFactory.create(queryString);
			QueryExecution qE = QueryExecutionFactory.create(query, currentModel);
			ResultSet results = qE.execSelect();	

			// Generate Return Model
			Model returnModel = results.getResourceModel();
			ResourceRequestListener.LOGGER.log(Level.INFO, "Query Result model generated");
			return returnModel;
 		} catch (Exception e){
 			e.printStackTrace();
 		}
		return null;
	}
    

}
