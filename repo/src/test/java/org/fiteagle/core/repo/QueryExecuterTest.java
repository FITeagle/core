package org.fiteagle.core.repo;

import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.repo.ResourceRepoHandler.ResourceRepositoryException;
import org.junit.Test;

import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.resultset.RDFInput;

public class QueryExecuterTest {

	@Test(expected = ResourceRepositoryException.class)
	public void testInvalidQuery() throws ResourceRepositoryException {
		QueryExecuter.executeSparqlDescribeQuery("This is no valid SPARQL Query");
	}

//	@Test
	public void test2() throws ResourceRepositoryException {
		Model rs = QueryExecuter
				.executeSparqlConstructQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
						+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
						+ "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
						+ "CONSTRUCT { ?testbed rdf:type omn:Testbed. ?testbed rdfs:label ?label. "
						+ "?testbed rdfs:seeAlso ?seeAlso. ?testbed wgs:long ?long. ?testbed wgs:lat ?lat. } "
						+ "FROM <http://localhost:3030/ds/query> "
						+ "WHERE {?testbed rdf:type omn:Testbed. "
						+ "OPTIONAL {?testbed rdfs:label ?label. ?testbed rdfs:seeAlso ?seeAlso. ?testbed wgs:long ?long. ?testbed wgs:lat ?lat. } }");
		System.out.println(rs.getGraph());
	}
	
//	 @Test
	  public void testListResources() throws ResourceRepositoryException {
		  String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
					+ "CONSTRUCT { ?resource omn:partOfGroup <http://federation.av.tu-berlin.de/about#AV_Smart_Communication_Testbed>."
					+ "?resource rdfs:label ?label. "
					+ "?resource rdfs:comment ?comment."
					+ "?resource rdf:type ?type. "
					+ "?resource wgs:lat ?lat. "
					+ "?resource wgs:long ?long. } "
					+ "FROM <http://localhost:3030/ds/query> "
					+ "WHERE {?resource omn:partOfGroup <http://federation.av.tu-berlin.de/about#AV_Smart_Communication_Testbed>. "
					+ "OPTIONAL {?resource rdfs:label ?label. "
					+ "?resource rdfs:comment ?comment. "
					+ "?resource rdf:type ?type. "
					+ "?resource wgs:lat ?lat. "
					+ "?resource wgs:long ?long. } }";
		  
		  
		  Model rs = QueryExecuter.executeSparqlConstructQuery(query);
		  
		  StmtIterator iter = rs.listStatements();
		  while(iter.hasNext()){
			  System.out.println(iter.next());
		  }
	  }
	  
	//	 @Test
		  public void testgetExtensions() throws ResourceRepositoryException {
			  String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
			          + "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
			          + "PREFIX av: <http://federation.av.tu-berlin.de/about#> "
			          + "CONSTRUCT { ?resource omn:partOfGroup av:AV_Smart_Communication_Testbed."
			          + "?resource wgs:lat ?lat. ?resource wgs:long ?long. ?resource <http://open-multinet.info/ontology/resource/>. "
			          + "} "
			          + "FROM <http://localhost:3030/ds/query> "
			          + "WHERE {?resource omn:partOfGroup av:AV_Smart_Communication_Testbed. "
			          + "OPTIONAL {?resource <http://open-multinet.info/ontology/resource/>. } "
			          + "}";
			  
			  
			  Model rs = QueryExecuter.executeSparqlConstructQuery(query);
			  
			  StmtIterator iter = rs.listStatements();
			  while(iter.hasNext()){
				  System.out.println(iter.next());
			  }
		  }
		  
//	     @Test
      public void testDescribe() throws ResourceRepositoryException {
       String query = "DESCRIBE ?property WHERE {?property <http://www.w3.org/2000/01/rdf-schema#domain>  <http://open-multinet.info/ontology/resource/motor#Motor> . }";
        
        Model rs = QueryExecuter.executeSparqlDescribeQuery(query);
        
        String result = MessageUtil.serializeModel(rs);
        System.out.println(result);
      }

      //@Test
      public void TestSelectMaxInstances() throws ResourceRepositoryException{
    	  
    	  ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(" SELECT ?amount WHERE {<http://federation.av.tu-berlin.de/about#MotorGarage-1> <http://open-multinet.info/ontology/omn#maxInstances> ?amount } ");
    	  while(rs.hasNext()){
    		  System.out.println(rs.next().getLiteral("amount").getInt());
    	  }
      }
      
     //@Test
      public void TestSelectInstances() throws ResourceRepositoryException{
    	  ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(" SELECT ?instance WHERE {?instance a ?resourceType . <http://federation.av.tu-berlin.de/about#MotorGarage-1> a ?adapterType . ?adapterType <http://open-multinet.info/ontology/omn#implements> ?resourceType .} ");
    	  int amount = 0;
    	  while(rs.hasNext()){
    		  amount++;
    		  rs.next();
    	  }
    	  System.out.println(amount);
      }
     
    //@Test
     public void TestSelectMaxInstancesCanBeCreated() throws QueryParseException, ResourceRepositoryException{
   	  
   	  ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(" SELECT ?amount WHERE {<http://federation.av.tu-berlin.de/about#MotorGarage-1> <http://open-multinet.info/ontology/omn#maxInstancesCanBeCreated> ?amount } ");
   	  while(rs.hasNext()){
   		  System.out.println(rs.next().getLiteral("amount").getInt());
   	  }
     }
     
 		 //@Test
	  public void testget() throws ResourceRepositoryException {
		  String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
		          + "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
		          + "PREFIX av: <http://federation.av.tu-berlin.de/about#> "
		          + "CONSTRUCT {"
		          + "?motoradapter a omn:MotorGarage ."
		          + "} "
		          + "FROM <http://localhost:3030/ds/query> "
		          + "WHERE { "
		          + "OPTIONAL {av:MotorGarage-1 omn:maxInstances ?maxInstances . }"
		          + "OPTIONAL {av:MotorGarage-1 omn:maxInstances ?reservedInstances . }"
		          + "}";
		  
		  
		  Model rs = QueryExecuter.executeSparqlConstructQuery(query);
		  
		  StmtIterator iter = rs.listStatements();
		  while(iter.hasNext()){
			  System.out.println(iter.next());
		  }
	  }
//	  + "<http://federation.av.tu-berlin.de/about#MotorGarage-1> a ?adapterType ."
//		+ "?adapterType <http://open-multinet.info/ontology/omn#implements> ?resourceType ."
 		 
 		// @Test
 	      public void TestSelecterhhrjt() throws ResourceRepositoryException{
 			 String query = "SELECT ?maxInstances ?reservedInstances ?instance WHERE {"
 			 		+ "OPTIONAL { ?instance a ?resourceType ."
 			 		+ "<http://federation.av.tu-berlin.de/about#MotorGarage-1> a ?adapterType ."
 			 		+ "?adapterType <http://open-multinet.info/ontology/omn#implements> ?resourceType .}"
 			 		+ "OPTIONAL { <http://federation.av.tu-berlin.de/about#MotorGarage-1> <http://open-multinet.info/ontology/omn#maxInstances> ?maxInstances  }"
 			 		+ "OPTIONAL { <http://federation.av.tu-berlin.de/about#MotorGarage-1> <http://open-multinet.info/ontology/omn#reservedInstances> ?reservedInstances } "
 			 		+ "}";
 			 
 	    	 ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(query);
 	    	 Model resultModel = ResultSetFormatter.toModel(rs);
 	    	StmtIterator iter = resultModel.listStatements();
 	    	
 	    	Resource maxInstSubject = null;
 			while(iter.hasNext()){
 				Statement st = iter.next();
 				if(st.getObject().isLiteral()){
 					if(st.getLiteral().toString().equals("maxInstances")){
 						maxInstSubject = st.getSubject();
// 						Statement st2 = maxInstSubject.getProperty(resultModel.createProperty("<http://www.w3.org/2001/sw/DataAccess/tests/result-set#value>"));
 					}
 				}
 	  	  	}
 			iter = resultModel.listStatements();
 			while(iter.hasNext()){
 				Statement st = iter.next();
 				if(st.getSubject().equals(maxInstSubject) && st.getPredicate().getLocalName().toString().equals("value")){
 					System.out.println(st.getInt());
 				}
 	  	  	}
 /*	    	 System.out.println(resultModel.getGraph());
 	    	  int amount = 0;
 	    	  while(rs.hasNext()){
 	    		  amount++;
// 	    		  System.out.println(rs.next());
 	    	  }*/
 	    	  
 	      }
 		 
		// @Test
	      public void TestSelecterhhrjt1() throws ResourceRepositoryException{
			 String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
			 		+ "PREFIX av: <http://federation.av.tu-berlin.de/about#> "
			 		+ "SELECT ?maxInstances ?reservedInstances ?instance WHERE {"
			 		+ "OPTIONAL { ?instance a ?resourceType ."
			 		+ "av:MotorGarage-1 a ?adapterType ."
			 		+ "?adapterType omn:implements ?resourceType .}"
			 		+ "OPTIONAL { av:MotorGarage-1 omn:maxInstances ?maxInstances  }"
			 		+ "OPTIONAL { av:MotorGarage-1 omn:reservedInstances ?reservedInstances } "
			 		+ "}";
			 
	    	 ResultSet rs  = QueryExecuter.executeSparqlSelectQuery(query);
	    	 Model resultModel = ResultSetFormatter.toModel(rs);
	    	 
	    	 ResultSet result = new RDFInput(resultModel);
	    	 int maxInstances = 0;
	    	 int instances = 0;
	    	 while(result.hasNext()){
	    		 QuerySolution qs = result.next();
	    		 if(qs.contains("maxInstances")){
	    			 maxInstances = qs.getLiteral("maxInstances").getInt();
	    		 }
	    		 if(qs.contains("instance")){
	    			 instances = instances +1; 
	    			 }
	    		 }
	    	 System.out.println("maxInstances are " + maxInstances);
	    	 System.out.println("created instances " + instances);
	    	 
		 }
}

 