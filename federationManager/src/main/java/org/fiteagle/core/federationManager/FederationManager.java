package org.fiteagle.core.federationManager;

import info.openmultinet.ontology.exceptions.InvalidModelException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.api.core.TimerHelper;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor;
import org.fiteagle.api.tripletStoreAccessor.TripletStoreAccessor.ResourceRepositoryException;
import org.fiteagle.core.federationManager.dm.FederationManagerREST;

import com.hp.hpl.jena.rdf.model.Model;

@Startup
@Singleton
@ApplicationPath("/")
public class FederationManager extends Application{

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(FederationManagerREST.class);
        return s;
    }

    private static final Logger LOGGER = Logger.getLogger(FederationManager.class.getName());
    private Model federationModel;
    private static FederationManager manager;


    public boolean initialized;

    @javax.annotation.PostConstruct
    public void setup() {
    	manager = this;
        initialized = false;
        
        // TODO Make it not so confusing (Maybe Switch-Case)
        File federationOntologie = Paths.get(System.getProperty("user.home")).resolve(".fiteagle").resolve("Federation.ttl").toFile();
        if(federationOntologie.exists()){
        	if(federationModel == null){
            	federationModel = OntologyModelUtil.loadModel(federationOntologie.toString(), IMessageBus.SERIALIZATION_TURTLE);
        		
            	if(federationModel.isEmpty()){
                	federationModel = OntologyModelUtil.loadModel("ontologies/defaultFederation.ttl", IMessageBus.SERIALIZATION_TURTLE);
                    LOGGER.log(Level.SEVERE,"Please add your Federation-Ontology to the '/home/User/.fiteagle/Federation.ttl' File and Re-Deploy the FederationManager ");
            	}   
        	}
    
        }else {
        	try {
				federationOntologie.createNewFile();
			} catch (IOException e) {
                LOGGER.log(Level.SEVERE,"Couldn't load Federation-Ontology. Tried to create new file '/home/User/.fiteagle/Federation.ttl' but Errored");
			}
        	federationModel = OntologyModelUtil.loadModel("ontologies/defaultFederation.ttl", IMessageBus.SERIALIZATION_TURTLE);
            LOGGER.log(Level.SEVERE,"Please add your Federation-Ontology to the '/home/User/.fiteagle/Federation.ttl' File and Re-Deploy the FederationManager ");
        }
        
        
        runSetup();
    }


    public void runSetup(){

        if(!initialized){
            TimerHelper timer = new TimerHelper(new SetupTask());
        }
    }
   
   public static FederationManager getManager(){
	   return manager;
   }

//   @PreDestroy
//	public void deleteFederationManagerApi() {
//		TimerHelper timer = new TimerHelper(new DeleteFederationApi());
//	}
   
   private class SetupTask implements Callable<Void> {
   	
		@Override
		public Void call() throws ResourceRepositoryException, InvalidModelException, Exception  {

			TripletStoreAccessor.addModel(federationModel);
			FederationManager.manager.initialized = true;
           return null;
		}


   }
   
   final class DeleteFederationApi implements Callable<Void> {

		@Override
		public Void call() throws ResourceRepositoryException, InvalidModelException {
				TripletStoreAccessor.deleteModel(federationModel);
			return null;
		}
	}

}
