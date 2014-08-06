package org.fiteagle.core.repo.dm;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.core.repo.ResourceRepository;

import com.hp.hpl.jena.query.QueryParseException;

@Path("/")
public class ResourceRepositoryREST {

	private static final int TIMEOUT = 2000;
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
		return repoEJB
				.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/ejb/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTLviaEJB() {
		LOGGER.log(Level.INFO, "Getting resources as TTL via EJB...");
		return repoEJB.listResources(IMessageBus.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaNative() {
		LOGGER.log(Level.INFO, "Getting resources as RDF...");
		return repo
				.listResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTL() {
		LOGGER.log(Level.INFO, "Getting resources as TTL...");
		return repo.listResources(IMessageBus.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/mdb/resources.rdf")
	@Produces("application/rdf+xml")
	public String listResourcesXMLviaMDB() throws JMSException,
			InterruptedException {
		return mdbListAllResources(IResourceRepository.SERIALIZATION_RDFXML_ABBREV);
	}

	@GET
	@Path("/mdb/resources.ttl")
	@Produces("text/turtle")
	public String listResourcesTTLviaMDB() throws JMSException,
			InterruptedException {
		return mdbListAllResources(IMessageBus.SERIALIZATION_TURTLE);
	}

	@GET
	@Path("/mdb/resources.jsonld")
	@Produces("application/ld+json")
	public String listResourcesLDviaMDB() throws JMSException,
			InterruptedException {
		return mdbListAllResources(IMessageBus.SERIALIZATION_JSONLD);
	}

	@GET
	@Path("/sparql")
	@Produces("text/turtle")
	public String sparql(@QueryParam("query") String query)
			throws JMSException, InterruptedException {
		String result = "unkown";
		try {
			result = this.repo.queryDatabse(query,
					IMessageBus.SERIALIZATION_JSONLD);
		} catch (QueryParseException | IllegalArgumentException e) {
			result = e.getMessage();
		}
		return result;
	}

	
	private String mdbListAllResources(final String serialization)
			throws JMSException {
		String result = "no answer - timeout";

		Message message = context.createMessage();
		
		message.setStringProperty(IMessageBus.TYPE, IMessageBus.REQUEST);
		message.setStringProperty(IMessageBus.TARGET, IResourceRepository.SERVICE_NAME);
		message.setStringProperty(IMessageBus.QUERY, "CONSTRUCT ?s ?p ?o");
		message.setStringProperty(IMessageBus.SERIALIZATION, "TTL");
		
		message.setJMSCorrelationID(UUID.randomUUID().toString());
		final String filter = "JMSCorrelationID='"
				+ message.getJMSCorrelationID() + "'";

		LOGGER.log(Level.INFO, "Getting resources via MDB...");
		this.context.createProducer().send(topic, message);
		Message rcvMessage = context.createConsumer(topic, filter)
				.receive(TIMEOUT);
		LOGGER.log(Level.INFO, "Received resources via MDB...");

		if (null != rcvMessage)
			result = rcvMessage.getStringProperty(IMessageBus.RESULT);

		return result;
	}
}
