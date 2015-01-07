package org.fiteagle.core.bus.dm;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.fiteagle.api.core.IMessageBus;

@ServerEndpoint("/api/logger")
@MessageDriven(name = "LoggerMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class MessageBusLoggerWS implements MessageListener {
  
  private static final Logger LOGGER = Logger.getLogger(MessageBusLoggerWS.class.getName());
  
  private static Queue<Session> queue = new ConcurrentLinkedQueue<Session>();
  
  @OnOpen
  public void open(Session session) {
    LOGGER.log(Level.INFO, "Opening WebSocket connection with " + session.getId() + "...");
    queue.add(session);
  }
  
  @OnError
  public void error(Session session, Throwable t) {
    LOGGER.log(Level.INFO, "Error on session " + session.getId());
    queue.remove(session);
  }
  
  @OnClose
  public void closedConnection(Session session) {
    LOGGER.log(Level.INFO, "Closed session: " + session.getId());
    queue.remove(session);
  }
  
  private static void sendToAllSessions(String msg) {
    try {
      ArrayList<Session> closedSessions = new ArrayList<Session>();
      for (Session session : queue) {
        if (!session.isOpen()) {
          LOGGER.log(Level.INFO, "Closed session: " + session.getId());
          closedSessions.add(session);
        } else {
          session.getBasicRemote().sendText(msg);
        }
      }
      queue.removeAll(closedSessions);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  public void onMessage(final Message message) {
    try {
      String serializedResult = parseToJSON(message);
      sendToAllSessions(serializedResult);
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private String parseToJSON(Message message) throws JMSException {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    try {
      if(message.getBody(String.class) == null){
        builder.add("body", "N.A.");
      }
      else{
        builder.add("body", message.getBody(String.class));
      }
      if (message.getStringProperty(IMessageBus.METHOD_TARGET) == null) {
        builder.add(IMessageBus.METHOD_TARGET, "N.A.");
      }
      else {
        builder.add("JMSCorrelationID", message.getJMSCorrelationID());
      }
      if (message.getJMSCorrelationID() == null) {
        builder.add("JMSCorrelationID", "N.A.");
      }
      else {
        builder.add("JMSCorrelationID", message.getJMSCorrelationID());
      }
      for (@SuppressWarnings("unchecked") Enumeration<String> properties = message.getPropertyNames(); properties.hasMoreElements();) {
        String currentProperty = properties.nextElement();
        builder.add(currentProperty, message.getStringProperty(currentProperty));
      }
    } catch (Error e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    JsonObject model = builder.build();
    StringWriter stWriter = new StringWriter();
    try (JsonWriter jsonWriter = Json.createWriter(stWriter)) {
      jsonWriter.writeObject(model);
    }
    
    String jsonString = stWriter.toString();
    return jsonString;
  }
}
