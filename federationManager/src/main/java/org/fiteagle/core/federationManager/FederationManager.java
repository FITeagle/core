package org.fiteagle.core.federationManager;

import info.openmultinet.ontology.exceptions.InvalidModelException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.jena.atlas.web.HttpException;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.fiteagle.core.federationManager.dm.FederationManagerREST;

import com.hp.hpl.jena.rdf.model.Model;

@Startup
@Singleton
@ApplicationPath("/")
public class FederationManager extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(FederationManagerREST.class);
		return s;
	}

	private static final Logger LOGGER = Logger
			.getLogger(FederationManager.class.getName());
	protected Model federationModel;
	protected static FederationManager manager;
	private int failureCounter = 0;

	@javax.annotation.Resource
	public TimerService timerService;

	public boolean initialized;

	@javax.annotation.PostConstruct
	public void setup() {
		manager = this;
		initialized = false;

		// TODO Make it not so confusing (Maybe Switch-Case)
		File federationOntologie = Paths.get(System.getProperty("user.home"))
				.resolve(".fiteagle").resolve("Federation.ttl").toFile();
		if (federationOntologie.exists()) {
			if (federationModel == null) {
				federationModel = OntologyModelUtil.loadModel(
						federationOntologie.toString(),
						IMessageBus.SERIALIZATION_TURTLE);

				if (federationModel.isEmpty()) {
					federationModel = OntologyModelUtil.loadModel(
							"ontologies/defaultFederation.ttl",
							IMessageBus.SERIALIZATION_TURTLE);
					LOGGER.log(
							Level.SEVERE,
							"Please add your Federation-Ontology to the '/home/User/.fiteagle/Federation.ttl' File and Re-Deploy the FederationManager ");
				}
			}

		} else {
			try {
				federationOntologie.createNewFile();
			} catch (IOException e) {
				LOGGER.log(
						Level.SEVERE,
						"Couldn't load Federation-Ontology. Tried to create new file '/home/User/.fiteagle/Federation.ttl' but Errored");
			}
			federationModel = OntologyModelUtil.loadModel(
					"ontologies/defaultFederation.ttl",
					IMessageBus.SERIALIZATION_TURTLE);
			LOGGER.log(
					Level.SEVERE,
					"Please add your Federation-Ontology to the '/home/User/.fiteagle/Federation.ttl' File and Re-Deploy the FederationManager ");
		}
		runSetup();

	}

	public void runSetup() {

		if (!initialized) {
	    	TimerConfig config = new TimerConfig();
			config.setPersistent(false);
			timerService.createIntervalTimer(0, 5000, config);
		}
	}

	public static FederationManager getManager() {
		return manager;
	}

	@Timeout
	public void timerMethod(Timer timer) {
		if (failureCounter < 100) {
			try {
				TripletStoreAccessor.addModel(federationModel);
				initialized = true;
				timer.cancel();
			} catch (ResourceRepositoryException e) {

				LOGGER.log(Level.INFO,
						 "Errored while adding something to Database - will try again");
				failureCounter++;

			} catch (InvalidModelException e) {
				e.printStackTrace();
				failureCounter++;

			}catch (Exception e) {
                LOGGER.log(Level.INFO,
                        "Errored while working with the Database - will try again");
                failureCounter++;
            }
		} else {
			LOGGER.log(
					Level.SEVERE,
					"Tried to add something to Database several times, but failed. Please check the OpenRDF-Database");
			timer.cancel();
		}

	}

}