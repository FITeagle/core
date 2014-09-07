package org.fiteagle.core.repo;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

public class ResourceRequestListener {
    
    private static final String FUSEKI_SERVICE = "http://localhost:3030/ds/query";
    
    private static Logger LOGGER = Logger.getLogger(ResourceRequestListener.class.toString());

    public static ResultSet queryModelFromDatabase(String sparqlQuery){
  
        ResourceRequestListener.LOGGER.log(Level.INFO, "Querying Fuseki Service");
        
    	ResultSet queryResult = submitSparqlQuery(sparqlQuery);

        return queryResult;        
    }
    
    /**
     * Submits a given sparql query string to the db
     * @param queryString String containing the sparql query to be issued
     * @return ResultSet retrieved from db, null on failure
     */
    private static ResultSet submitSparqlQuery(String queryString){
        ResultSet rs = null;
    	try {
            QueryExecution qe = QueryExecutionFactory.sparqlService(FUSEKI_SERVICE, queryString);
            LOGGER.log(Level.INFO, "Now Sending query: "+ qe.getQuery().toString());
            rs = qe.execSelect();
            // LOGGER.log(Level.INFO, )
    		// Get current Model from DB
         /*   DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(FUSEKI_SERVICE);
            ResourceRequestListener.LOGGER.log(Level.INFO, "after createHTTP");
            Model currentModel = accessor.getModel();
            ResourceRequestListener.LOGGER.log(Level.INFO, "Remote Fuseki Model obtained");
            
            // Generate query from query string and execute it on the current Model
			Query query = QueryFactory.create(queryString);
			QueryExecution qE = QueryExecutionFactory.create(query, currentModel);
			ResultSet results = qE.execSelect();	

			// Generate Return Model
			Model returnModel = results.getResourceModel();
			ResourceRequestListener.LOGGER.log(Level.INFO, "Query Result model generated");
			return returnModel;*/


 		} catch (Exception e){
 			//e.printStackTrace();
 		}
		return rs;
	}
    

}
