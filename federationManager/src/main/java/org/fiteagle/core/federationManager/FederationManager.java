package org.fiteagle.core.federationManager;

import info.openmultinet.ontology.exceptions.InvalidModelException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.core.federationManager.dm.FederationManagerREST;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

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

    private static final Logger LOGGER = Logger.getLogger(FederationManager.class.getName());
    private Model federationModel;
    private static FederationManager manager;


    boolean initialized;

    private Timer timer;
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
        
        
        timer = new Timer();
        runSetup();
    }


    public void runSetup(){

        if(!initialized){
            try{
                SetupTask task = new SetupTask(500);
                timer.schedule(task, 0);
            } catch(IllegalStateException e){
                LOGGER.log(Level.INFO,"Attempt to schedule task on canceled timer, doesn't matter ");
            }
        }
    }

   private class SetupTask extends TimerTask {

       long delay;

       SetupTask(long delay){
           this.delay = delay;
       }
       @Override
       public void run() {

           if(!initialized && delay < 3600000 ) {
               try {
                  TripletStoreAccessor.addModel(federationModel);
                   initialized = true;
                   timer.cancel();
               } catch (TripletStoreAccessor.ResourceRepositoryException e) {
                   LOGGER.log(Level.INFO, e.getMessage());
                   delay = delay + delay;
                   timer.schedule(new SetupTask(delay), delay);

               } catch (InvalidModelException e) {
                   LOGGER.log(Level.INFO, e.getMessage());
               }
           }else{
               timer.cancel();
           }
       }


   }
   
   public static FederationManager getManager(){
	   return manager;
   }
}
