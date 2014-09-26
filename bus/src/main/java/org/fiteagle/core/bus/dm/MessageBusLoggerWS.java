package org.fiteagle.core.bus.dm;


import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Enumeration;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.json.*;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.jena.atlas.json.JsonBuilder;
import org.fiteagle.api.core.IMessageBus;
import org.hornetq.utils.json.JSONObject;

@ServerEndpoint("/api/logger")
@MessageDriven(name = "LoggerMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class MessageBusLoggerWS implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(MessageBusLoggerWS.class.getName());

  //  private Session wsSession;
    private static Queue<Session> queue = new ConcurrentLinkedQueue<Session>();

//    @OnOpen
//    public void onOpen(final Session wsSession, final EndpointConfig config) throws IOException {
//        LOGGER.log(Level.INFO, "Opening WebSocket connection with " + wsSession.getId() + "...");
//        this.wsSession = wsSession;
//    }
    
//    @OnMessage
//    public void onMessage(Session session, String msg) {
//   //provided for completeness, in out scenario clients don't send any msg.
//     try {   
//      System.out.println("received msg "+msg+" from "+session.getId());
//     } catch (Exception e) {
//      e.printStackTrace();
//     }
//    }

     @OnOpen
    public void open(Session session) {
         LOGGER.log(Level.INFO, "Opening WebSocket connection with " + session.getId() + "...");
     queue.add(session);
    }

    @OnError
    public void error(Session session, Throwable t) {
         LOGGER.log(Level.INFO, "Error on session "+session.getId());
     queue.remove(session); 
    }

     @OnClose
    public void closedConnection(Session session) { 
         LOGGER.log(Level.INFO, "Closed session: "+session.getId());
     queue.remove(session);
    }
    
    private static void sendAll(String msg) {
     try {
      /* Send the new rate to all open WebSocket sessions */  
      ArrayList<Session > closedSessions= new ArrayList<Session>();
      for (Session session : queue) {
       if(!session.isOpen())
       {
           LOGGER.log(Level.INFO, "Closed session: "+session.getId());
        closedSessions.add(session);
       }
       else
       {
        session.getBasicRemote().sendText(msg);
       }    
      }
      queue.removeAll(closedSessions);
      // LOGGER.log(Level.INFO, "Sending message to "+queue.size()+" clients");
     } catch (Throwable e) {
      e.printStackTrace();
     }
    }

    public void onMessage(final Message message) {
        try {
//            LOGGER.log(Level.INFO, "Logging JMS message...");
//            LOGGER.log(Level.INFO, "" + this.wsSession.isOpen());
//            if (null != this.wsSession && this.wsSession.isOpen()) {
                String result = messageToJson(message);
                sendAll(result);
//                Set<Session> sessions = this.wsSession.getOpenSessions();
//                for (Session client : sessions) {
//                    client.getAsyncRemote().sendText(result);
//                }
//            } else {
//                LOGGER.log(Level.INFO, "No client to talk to");
//            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }
    private String messageToJson(Message message) throws JMSException{
        JsonObjectBuilder job = Json.createObjectBuilder();
        try {

            if (message.getJMSMessageID() == null) {
                job.add("MessageID", "N.A.");
            } else {
                job.add("MessageID", message.getJMSMessageID());
            }
            if (message.getJMSCorrelationID() == null) {
                job.add("JMSCorrelationID", "N.A.");
            } else {
                job.add("JMSCorrelationID", message.getJMSCorrelationID());
            }
            for (Enumeration<String> properties = message.getPropertyNames(); properties.hasMoreElements(); ) {
                String currentProperty = properties.nextElement();
                job.add(currentProperty ,message.getStringProperty(currentProperty));
            }
        } catch (Error e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        JsonObject model = job.build();
        StringWriter stWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stWriter)) {
            jsonWriter.writeObject(model);
        }

        String jsonString = stWriter.toString();
        return jsonString;
    }
    private String messageToString(Message message) throws JMSException {
        String result = "";

        result += "Message ID: " + message.getJMSMessageID() + "\n";
        result += "  * JMSCorrelationID: " + message.getJMSCorrelationID() + "\n";

        for (Enumeration<String> properties = message.getPropertyNames(); properties.hasMoreElements();) {
            String currentProperty = properties.nextElement();
            result += "  * " + currentProperty + " : " + message.getStringProperty(currentProperty) + "\n";
        }

        return result;
    }
}


