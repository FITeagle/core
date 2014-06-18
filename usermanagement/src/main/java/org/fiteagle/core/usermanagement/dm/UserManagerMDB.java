package org.fiteagle.core.usermanagement.dm;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
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
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserManager.UserNotFoundException;
import org.fiteagle.api.core.usermanagement.UserPublicKey;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


@MessageDriven(name="UserManagerMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = UserManager.MESSAGE_FILTER),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
    })
public class UserManagerMDB implements MessageListener {

  private final UserManager usermanager;
  private final Gson gsonBuilder;
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private final static Logger logger = Logger.getLogger(UserManagerMDB.class.toString());
  
  public UserManagerMDB() throws NamingException{
    Context context;
    context = new InitialContext();
    usermanager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager");
    gsonBuilder = new GsonBuilder()
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
  }
  
  private HashMap<String, Exception> exceptions = new HashMap<>();
  
  @Override
  public void onMessage(final Message rcvMessage) {
    try {
      if(rcvMessage.getJMSRedelivered()){
        Thread.sleep(100);
        final String id = rcvMessage.getJMSCorrelationID();
        Exception e = exceptions.remove(id);
        String exceptionName = e.getClass().getSimpleName();
        final Message message = this.context.createMessage();
        message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_USER);
        message.setStringProperty(IMessageBus.TYPE_EXCEPTION, exceptionName+": "+e.getMessage());        
        if(id != null){
          message.setJMSCorrelationID(id);
        }
        this.context.createProducer().send(topic, message);
        return;
      }
      
      String methodName = rcvMessage.getStringProperty(IMessageBus.TYPE_REQUEST);
      logger.info("Received a message: "+methodName);

      if(methodName == null){
        return;
      }
      final Message message = this.context.createMessage();
      final String id = rcvMessage.getJMSCorrelationID();
      
      String username, description, result, password;
      long classId;
      switch(methodName){
        case UserManager.GET_ALL_USERS: 
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_USERS);
          List<User> users = usermanager.getAllUsers();
          final String usersJSON = gsonBuilder.toJson(users);
          message.setStringProperty(IMessageBus.TYPE_RESULT, usersJSON);
          break;          
        case UserManager.GET_USER:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_USER);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          User user = null;
          try{
            user = usermanager.getUser(username);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, gsonBuilder.toJson(user));
          break;
        case UserManager.ADD_USER:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_USER);
          String userJSON = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USER_JSON);
          try{
            usermanager.add(gsonBuilder.fromJson(userJSON, User.class));
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.UPDATE_USER:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.UPDATE_USER);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          String firstName = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_FIRSTNAME);
          String lastName = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_LASTNAME);
          String email = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_EMAIL);
          String affiliation = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_AFFILIATION);
          password = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PASSWORD);
          Type listType = new TypeToken<ArrayList<UserPublicKey>>() {}.getType();
          List<UserPublicKey> publicKeys = gsonBuilder.fromJson(rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEYS), listType);
          try{
            usermanager.update(username, firstName, lastName, email, affiliation, password, publicKeys);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.SET_ROLE:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.SET_ROLE);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          Role role = Role.valueOf(rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_ROLE));
          try{
            usermanager.setRole(username, role);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.ADD_PUBLIC_KEY:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.ADD_PUBLIC_KEY);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          UserPublicKey publicKey = gsonBuilder.fromJson(rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY), UserPublicKey.class);
          try{
            usermanager.addKey(username, publicKey);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.DELETE_PUBLIC_KEY:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.DELETE_PUBLIC_KEY);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          description = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION);
          try{
            usermanager.deleteKey(username, description);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.RENAME_PUBLIC_KEY:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.RENAME_PUBLIC_KEY);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          description = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION);
          String newDescription = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION_NEW);
          try{
            usermanager.renameKey(username, description, newDescription);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.DELETE_USER:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.DELETE_USER);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          try{
            usermanager.delete(username);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.CREATE_USER_CERT_AND_PRIVATE_KEY:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.CREATE_USER_CERT_AND_PRIVATE_KEY);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          String passphrase = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PASSPHRASE);
          try{
            result = usermanager.createUserKeyPairAndCertificate(username, passphrase);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          } catch (Exception e) {
            exceptions.put(id, e);
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, result);
          break;
        case UserManager.GET_USER_CERT_FOR_PUBLIC_KEY:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_USER_CERT_FOR_PUBLIC_KEY);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          description = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION);
          try{
            result = usermanager.createUserCertificateForPublicKey(username, description);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          } catch (Exception e) {
            exceptions.put(id, e);
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, result);
          break;
        case UserManager.GET_ALL_CLASSES_FROM_USER:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_CLASSES_FROM_USER);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          try{
            result = gsonBuilder.toJson(usermanager.getAllClassesFromUser(username));
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, result);
          break;
        case UserManager.GET_ALL_CLASSES_OWNED_BY_USER:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_CLASSES_OWNED_BY_USER);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          try{
            result = gsonBuilder.toJson(usermanager.getAllClassesOwnedByUser(username));
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, result);
          break;
        case UserManager.SIGN_UP_FOR_CLASS:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.SIGN_UP_FOR_CLASS);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
          try{
            usermanager.addParticipant(classId, username);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.LEAVE_CLASS:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.LEAVE_CLASS);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
          try{
            usermanager.removeParticipant(classId, username);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.GET_CLASS:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_CLASS);
          classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
          try{
            result = gsonBuilder.toJson(usermanager.get(classId));
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, result);
          break;
        case UserManager.ADD_CLASS:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.ADD_CLASS);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          String classJSON = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_CLASS_JSON);
          try{
            classId = usermanager.addClass(username, gsonBuilder.fromJson(classJSON, org.fiteagle.api.core.usermanagement.Class.class)).getId();
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          message.setLongProperty(IMessageBus.TYPE_RESULT, classId);
          break;
        case UserManager.DELETE_CLASS:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.DELETE_CLASS);
          classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
          try{
            usermanager.delete(classId);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          break;
        case UserManager.GET_ALL_CLASSES:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_CLASSES);
          try{
            result = gsonBuilder.toJson(usermanager.getAllClasses());
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          }
          message.setStringProperty(IMessageBus.TYPE_RESULT, result);
          break;
        case UserManager.VERIFY_CREDENTIALS:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.VERIFY_CREDENTIALS);
          username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
          password = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PASSWORD);
          Boolean resultBoolean;
          try{
            resultBoolean = usermanager.verifyCredentials(username, password);
          } catch(EJBException e){            
            exceptions.put(id, e.getCausedByException());
            return;
          } catch (NoSuchAlgorithmException | UserNotFoundException | IOException e) {
            exceptions.put(id, e);
            return;
          }
          message.setBooleanProperty(IMessageBus.TYPE_RESULT, resultBoolean);
          break;
      }
      
      if(id != null){
        message.setJMSCorrelationID(id);
      }
      this.context.createProducer().send(topic, message);
      
    } catch (final JMSException | InterruptedException e) {      
        logger.log(Level.SEVERE, "Issue with JMS", e);
    }
  }
  

}
