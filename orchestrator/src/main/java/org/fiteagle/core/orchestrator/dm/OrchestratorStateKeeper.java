package org.fiteagle.core.orchestrator.dm;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import java.util.HashMap;

/**
 * Created by dne on 04.02.15.
 */

@Singleton
public class OrchestratorStateKeeper{


    private HashMap<String, Request> waitingForResponse;


    @PostConstruct
    public void initialize(){
        this.waitingForResponse  = new HashMap<>();
    }

    public Request getRequest(String jmsCorrelationId){
        return waitingForResponse.get(jmsCorrelationId);

    }

    public void addRequest(Request request){
        waitingForResponse.put(request.getRequestId(), request);
    }

    public void removeRequest(String requestId){

        waitingForResponse.remove(requestId);
    }
}

