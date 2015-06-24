package org.fiteagle.core.federationManager.dm;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fiteagle.api.core.IConfig;
import org.fiteagle.core.federationManager.FederationManager;

@Path("/")
public class FederationManagerREST{
FederationManager restManager;
Writer writer;

//@Inject ControlFilter filter;

	  @POST
	  @Path("/ontology")
	  @Consumes("text/turtle")
	  @Produces("text/html")
	  public Response updateOntology(String ontology) {
		  try{
			File ontologyFile = IConfig.PROPERTIES_DIRECTORY.resolve("Federation.ttl").toFile();
			if (!ontologyFile.exists()) ontologyFile.createNewFile();
			
			writer = new FileWriter(ontologyFile);
			writer.write(ontology);
			writer.flush();
			writer.close();
			
	    	restManager = FederationManager.getManager();
	    	restManager.setup();
	    	return Response.status(Response.Status.OK.getStatusCode()).build();
	    }catch(Exception e){
	    	
	    }
	    return Response.status(Response.Status.CONFLICT.getStatusCode()).build();
	  }
	  
	  @POST
	  @Path("/failedAuth")
	  @Consumes("text/turtle")
	  @Produces("text/html")
	  public Response failedAuthentication() {
	    return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
	  }
}
