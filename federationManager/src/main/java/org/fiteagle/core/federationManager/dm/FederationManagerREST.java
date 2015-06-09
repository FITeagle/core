package org.fiteagle.core.federationManager.dm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.apache.jena.atlas.logging.Log;
import org.fiteagle.abstractAdapter.AbstractAdapter;
import org.fiteagle.abstractAdapter.AbstractAdapter.ProcessingException;
import org.fiteagle.api.core.IConfig;
import org.fiteagle.core.federationManager.FederationManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@Path("/")
public class FederationManagerREST{
FederationManager restManager;
Writer writer;

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
  
}
