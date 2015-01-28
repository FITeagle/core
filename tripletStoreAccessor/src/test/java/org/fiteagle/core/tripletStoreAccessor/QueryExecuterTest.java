package org.fiteagle.core.tripletStoreAccessor;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import org.fiteagle.api.core.IGeni;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageBusOntologyModel;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.junit.Test;

import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.resultset.RDFInput;
import com.hp.hpl.jena.vocabulary.RDF;

public class QueryExecuterTest {

//	@Test(expected = ResourceRepositoryException.class)
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
        
        String result = MessageUtil.serializeModel(rs, IMessageBus.SERIALIZATION_TURTLE);
        System.out.println(result);
      }


//	  @Test
	  public void testGetGroupURI() throws ResourceRepositoryException{
		  String reservation = "urn:publicid:IDN+wall2.ilabt.iminds.be+sliver+1681677363";
			String groupQuery = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
					+ "CONSTRUCT { "
					+ "?group a omn:Group ."
					+ " } "
					+ "FROM <http://localhost:3030/ds/query> "
					+ "WHERE {?group a omn:Group . "
					+ "?group omn:hasReservation \""
					+ reservation
					+ "\" . "
					+ "}";

			String groupURI = "";
			Model model = QueryExecuter.executeSparqlDescribeQuery(groupQuery);
			StmtIterator iter = model.listStatements();
			while(iter.hasNext()){
				groupURI = iter.next().getSubject().getURI();
			}
			System.out.println("groupURI " + groupURI); 
	    	 
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
	      

//	      @Test
	      public void isSliverURNallocated() throws ResourceRepositoryException{
	    	  String sliverURN = "wall2.ilabt.iminds.be+sliver+123";
	    	  String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
	    	  		+ "ASK WHERE { <" + sliverURN + "> a omn:Reservation }";
	    	  boolean result = QueryExecuter.executeSparqlAskQuery(query);
	    	  System.out.println(result);
	      }
	      
		  
//	      @Test
	      public void TestGetReservationDetails() throws ResourceRepositoryException{
	    	  String groupId = "urn:publicid:IDN+wall2.ilabt.iminds.be+slice+test16";
			   
	  		String query = "PREFIX omn: <http://open-multinet.info/ontology/omn#> "
					+ "SELECT ?reservationId ?componentManagerId WHERE { "
					+ "" + "<"
					+ groupId + "> a omn:Group ."
					+ "?reservationId omn:partOfGroup \"" + groupId + "\" . "
					+ "?reservationId omn:reserveInstanceFrom ?componentManagerId "
					+ "}";
			ResultSet rs = QueryExecuter.executeSparqlSelectQuery(query);

			while (rs.hasNext()) {
				QuerySolution qs = rs.next();

				if (qs.contains("reservationId") && qs.contains("componentManagerId")) {
					
					System.out.println("a sliver is found");
					System.out.println("reservation " + qs.getResource("reservationId").getURI()
							+ " componentManagerId " + qs.getLiteral("componentManagerId").getString());
				}
			}
	      }
	      
}

 