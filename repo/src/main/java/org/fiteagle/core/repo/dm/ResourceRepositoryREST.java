package org.fiteagle.core.repo.dm;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.ResourceRepository;

@Path("/")
public class ResourceRepositoryREST {

	@Inject
	private JMSContext context;
	@Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
	private Topic topic;
	
	private static final String EJB_NAME = "java:global/repo/ResourceRepositoryEJB";
	
	private static Logger LOGGER = Logger
			.getLogger(ResourceRepositoryREST.class.toString());
	private IResourceRepository repoEJB;
	private IResourceRepository repo;

	public ResourceRepositoryREST() throws NamingException {
		this.repoEJB = (IResourceRepository) new InitialContext()
				.lookup(ResourceRepositoryREST.EJB_NAME);
		this.repo = new ResourceRepository();
	}

	@GET
	@Path("/ejb/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaEJB() {
		LOGGER.log(Level.INFO, "Getting resources as RDF via EJB...");
		return repoEJB.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/ejb/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTLviaEJB() {
		LOGGER.log(Level.INFO, "Getting resources as TTL via EJB...");
		return repoEJB.listResources(IResourceRepository.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaNative() {
		LOGGER.log(Level.INFO, "Getting resources as RDF...");
		return repo.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTL() {
		LOGGER.log(Level.INFO, "Getting resources as TTL...");
		return repo.listResources(IResourceRepository.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/mdb/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaMDB() throws JMSException,
			InterruptedException {
		return mdbListResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/mdb/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTLviaMDB() throws JMSException,
			InterruptedException {
		return mdbListResources(IResourceRepository.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/mdb/resources.jsonld")
	@Produces("application/ld+json")
	public String listResourcesLDviaMDB() throws JMSException,
			InterruptedException {
		return mdbListResources(IResourceRepository.SERIALIZATION_JSONLD);
	}

	private String mdbListResources(final String serialization)
			throws JMSException {
		String result = "timeout";
		
		Message message = context.createMessage();
		message.setStringProperty(IMessageBus.TYPE_REQUEST,
				IResourceRepository.LIST_RESOURCES);
		message.setStringProperty(IResourceRepository.PROP_SERIALIZATION,
				serialization);
		message.setJMSCorrelationID(UUID.randomUUID().toString());
		final String filter = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
		
		LOGGER.log(Level.INFO, "Getting resources via MDB...");
		this.context.createProducer().send(topic, message);
		Message rcvMessage = context.createConsumer(topic, filter).receive(2000);
		LOGGER.log(Level.INFO, "Received resources via MDB...");

		if (null != rcvMessage)
			result = rcvMessage.getStringProperty(IMessageBus.TYPE_RESULT);
		
		return result;
	}
}
