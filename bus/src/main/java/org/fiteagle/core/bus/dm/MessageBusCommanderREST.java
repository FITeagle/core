package org.fiteagle.core.bus.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class MessageBusCommanderREST {

	private static Logger LOGGER = Logger
			.getLogger(MessageBusCommanderREST.class.toString());
	
	public MessageBusCommanderREST() throws NamingException {
		LOGGER.log(Level.INFO, "Started REST");
	}

	@GET
	@Path("/commander")
	public String commander() {
		LOGGER.log(Level.INFO, "Commander REST API call...");
		return "hello";
	}

}
