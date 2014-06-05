package org.fiteagle.core.usermanagement.dm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserPublicKey;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@MessageDriven(name="UserManagerMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_USERMANAGEMENT),   
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
    })
public class UserManagerMDB implements MessageListener {

  private final UserManager usermanager;
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_USERMANAGEMENT_NAME)
  private Topic topic;
  
  private final static Logger logger = Logger.getLogger(UserManagerMDB.class.toString());
  
  public UserManagerMDB() throws NamingException{
    Context context;
    context = new InitialContext();
    usermanager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager");
  }
  
  @Override
  public void onMessage(final Message rcvMessage) {
    try {
      String methodName = rcvMessage.getStringProperty(IMessageBus.TYPE_REQUEST);
      logger.info("Received a message: "+methodName);
      Method method = null;
      if(methodName == null){
        return;
      }
      try {
        method = usermanager.getClass().getMethod(methodName);
      } catch (SecurityException e) {
      } catch (NoSuchMethodException e) {        
      }
      Object toReturn = null;
      
      //List<User> users = null;
      try{
        toReturn = method.invoke(usermanager);
      } catch (IllegalArgumentException e) {
      } catch (IllegalAccessException e) {
      } catch (InvocationTargetException e) {
      }

      final String id = rcvMessage.getJMSCorrelationID();
      
      Gson gson = new GsonBuilder()
      .setExclusionStrategies(new ExclusionStrategy() {
          public boolean shouldSkipClass(Class<?> classToSkip) {
             return false;
          }
          public boolean shouldSkipField(FieldAttributes f) {
            return ((f.getDeclaringClass() == UserPublicKey.class && (f.getName().equals("owner") || f.getName().equals("publicKey"))) ||
                (f.getDeclaringClass() == User.class && (f.getName().equals("classes") || f.getName().equals("classesOwned"))));
          }
       }) 
      .create();
      
      final String resultJSON = gson.toJson(toReturn);
      
      final Message message = this.context.createMessage();
      message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_USERS);
      message.setStringProperty(IMessageBus.TYPE_RESULT, resultJSON);
      if(id != null){
        message.setJMSCorrelationID(id);
      }
      this.context.createProducer().send(topic, message);
    } catch (final JMSException e) {      
        logger.log(Level.SEVERE, "Issue with JMS", e);
    }
  }



}
