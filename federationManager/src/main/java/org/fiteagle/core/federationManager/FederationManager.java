package org.fiteagle.core.federationManager;

import com.hp.hpl.jena.rdf.model.Model;
import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.OntologyModelUtil;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class FederationManager {


    private static final Logger LOGGER = Logger.getLogger(FederationManager.class.getName());
    private Model federationModel;


    boolean initialized;

    private Timer timer;
    @javax.annotation.PostConstruct
    public void setup() {
        initialized = false;
        federationModel = OntologyModelUtil.loadModel("ontologies/defaultFederation.ttl", IMessageBus.SERIALIZATION_TURTLE);

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

               }
           }else{
               timer.cancel();
           }
       }


   }
}
