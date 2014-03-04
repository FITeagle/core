package org.fiteagle.core;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.fiteagle.api.core.IResourceRepository;
import org.fiteagle.api.core.IResourceRepository.Serialization;
import org.fiteagle.core.repository.ResourceRepository;
import org.fiteagle.core.repository.dm.ResourceRepositoryEJB;
import org.fiteagle.core.repository.dm.ResourceRepositoryMDB;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ResourceRepositoryTest {
	@Test
	public void testListResources() {
		final IResourceRepository repo = new ResourceRepository();
		String result = repo.listResources(Serialization.TTL);
		Assert.assertTrue(result.contains("@prefix"));
		result = repo.listResources(Serialization.XML);
		Assert.assertTrue(result.contains("<rdf"));
	}

	@Test
	public void testListResourcesViaMDB() throws JMSException {
		final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"vm://localhost?broker.persistent=false");
		final Topic topic = new ActiveMQTopic("test");
		new ResourceRepositoryMDB(connectionFactory, topic);
	}

	@Test
	public void testListResourcesViaEJB() {
		final IResourceRepository repo = new ResourceRepositoryEJB();
		String result = repo.listResources(Serialization.TTL);
		Assert.assertTrue(result.contains("@prefix"));
		result = repo.listResources(Serialization.XML);
		Assert.assertTrue(result.contains("<rdf"));
	}

}
