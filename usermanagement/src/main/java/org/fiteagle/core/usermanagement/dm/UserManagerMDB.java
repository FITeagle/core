package org.fiteagle.core.usermanagement.dm;

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

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.Node;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserPublicKey;
import org.fiteagle.core.aaa.authentication.PasswordUtil;
import org.fiteagle.core.usermanagement.userdatabase.JPAUserManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@MessageDriven(name="UserManagerMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = UserManager.MESSAGE_FILTER),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
    })
public class UserManagerMDB implements MessageListener {

  private static UserManager usermanager;
  private static ObjectMapper objectMapper;
  
  private static boolean connectionEstablished = false;
  
  static {
    objectMapper = new ObjectMapper();
  }
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private final static Logger logger = Logger.getLogger(UserManagerMDB.class.toString());
  
  public UserManagerMDB() {
  }
  
  private void setupConnection(){
    usermanager = JPAUserManager.getInstance();
    
    createDefaultNodeIfNecessary();
    if(!databaseContainsAdminUser()){
      createFirstAdminUser();
    }
  }
  
  private static void createDefaultNodeIfNecessary() {
    List<Node> nodes = usermanager.getAllNodes();
    if(nodes.size() > 0){
      Node defaultNode = nodes.get(0);
      logger.info("Setting default node: \""+ defaultNode.getName() + "\" with ID: " + defaultNode.getId());
      Node.setDefaultNode(defaultNode);
    }
    else{
      Node node = Node.createDefaultNode();
      logger.info("Creating First Node: \"" + node.getName() + "\"");
      usermanager.addNode(node);
    }
  }
  
  private static void createFirstAdminUser() {
    logger.info("Creating First Admin User");
    String[] passwordHashAndSalt = PasswordUtil.generatePasswordHashAndSalt("admin");
    User admin = User.createAdminUser("admin", passwordHashAndSalt[0], passwordHashAndSalt[1]);
    usermanager.addUser(admin);
  }
  
  private static boolean databaseContainsAdminUser() {
    List<User> users = usermanager.getAllUsers();
    for (User u : users) {
      if (u.getRole().equals(Role.FEDERATION_ADMIN)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public void onMessage(final Message rcvMessage) {
    
    if(connectionEstablished == false){
      setupConnection();
      connectionEstablished = true;
    }
    
    try {
      String methodName = rcvMessage.getStringProperty(IMessageBus.TYPE_REQUEST);
      logger.info("Received a message: "+methodName);

      final Message message = this.context.createMessage();
      
      String username, description, result, password;
      long classId, nodeId;
      try{
        switch(methodName){
          case UserManager.GET_ALL_USERS: 
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_USERS);
            List<User> users = usermanager.getAllUsers();
            final String usersJSON = objectMapper.writeValueAsString(users);
            message.setStringProperty(IMessageBus.TYPE_RESULT, usersJSON);
            break;          
          case UserManager.GET_USER:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_USER);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            User user = usermanager.getUser(username);
            message.setStringProperty(IMessageBus.TYPE_RESULT, objectMapper.writeValueAsString(user));
            break;
          case UserManager.ADD_USER:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.ADD_USER);
            String userJSON = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USER_JSON);
            usermanager.addUser(objectMapper.readValue(userJSON, User.class));
            break;
          case UserManager.UPDATE_USER:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.UPDATE_USER);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            String firstName = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_FIRSTNAME);
            String lastName = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_LASTNAME);
            String email = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_EMAIL);
            String affiliation = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_AFFILIATION);
            password = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PASSWORD);
            List<UserPublicKey> publicKeys = objectMapper.readValue(rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEYS), new TypeReference<List<UserPublicKey>>(){});
            usermanager.updateUser(username, firstName, lastName, email, affiliation, password, publicKeys);
            break;
          case UserManager.SET_ROLE:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.SET_ROLE);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            Role role = Role.valueOf(rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_ROLE));
            usermanager.setRole(username, role);
            break;
          case UserManager.ADD_PUBLIC_KEY:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.ADD_PUBLIC_KEY);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            UserPublicKey publicKey = objectMapper.readValue(rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY), UserPublicKey.class);
            usermanager.addKey(username, publicKey);
            break;
          case UserManager.DELETE_PUBLIC_KEY:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.DELETE_PUBLIC_KEY);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            description = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION);
            usermanager.deleteKey(username, description);
            break;
          case UserManager.RENAME_PUBLIC_KEY:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.RENAME_PUBLIC_KEY);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            description = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION);
            String newDescription = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION_NEW);
            usermanager.renameKey(username, description, newDescription);
            break;
          case UserManager.DELETE_USER:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.DELETE_USER);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            usermanager.deleteUser(username);
            break;
          case UserManager.CREATE_USER_CERT_AND_PRIVATE_KEY:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.CREATE_USER_CERT_AND_PRIVATE_KEY);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            String passphrase = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PASSPHRASE);
            result = usermanager.createUserKeyPairAndCertificate(username, passphrase);
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
          case UserManager.GET_USER_CERT_FOR_PUBLIC_KEY:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_USER_CERT_FOR_PUBLIC_KEY);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            description = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PUBLIC_KEY_DESCRIPTION);
            result = usermanager.createUserCertificateForPublicKey(username, description);
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
          case UserManager.GET_ALL_CLASSES_FROM_USER:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_CLASSES_FROM_USER);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            result = objectMapper.writeValueAsString(usermanager.getAllClassesFromUser(username));
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
          case UserManager.GET_ALL_CLASSES_OWNED_BY_USER:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_CLASSES_OWNED_BY_USER);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            result = objectMapper.writeValueAsString(usermanager.getAllClassesOwnedByUser(username));
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
          case UserManager.SIGN_UP_FOR_CLASS:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.SIGN_UP_FOR_CLASS);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
            usermanager.addParticipant(classId, username);
            break;
          case UserManager.LEAVE_CLASS:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.LEAVE_CLASS);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
            usermanager.removeParticipant(classId, username);
            break;
          case UserManager.GET_CLASS:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_CLASS);
            classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
            result = objectMapper.writeValueAsString(usermanager.getClass(classId));
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
          case UserManager.ADD_CLASS:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.ADD_CLASS);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            String classJSON = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_CLASS_JSON);
            classId = usermanager.addClass(username, objectMapper.readValue(classJSON, org.fiteagle.api.core.usermanagement.Class.class)).getId();
            message.setLongProperty(IMessageBus.TYPE_RESULT, classId);
            break;
          case UserManager.DELETE_CLASS:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.DELETE_CLASS);
            classId = rcvMessage.getLongProperty(UserManager.TYPE_PARAMETER_CLASS_ID);
            usermanager.deleteClass(classId);
            break;
          case UserManager.GET_ALL_CLASSES:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_CLASSES);
            result = objectMapper.writeValueAsString(usermanager.getAllClasses());
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
          case UserManager.VERIFY_CREDENTIALS:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.VERIFY_CREDENTIALS);
            username = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_USERNAME);
            password = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_PASSWORD);
            Boolean resultBoolean;
            resultBoolean = usermanager.verifyCredentials(username, password);
            message.setBooleanProperty(IMessageBus.TYPE_RESULT, resultBoolean);
            break;
          case UserManager.ADD_NODE:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.ADD_NODE);
            String nodeJSON = rcvMessage.getStringProperty(UserManager.TYPE_PARAMETER_NODE_JSON);
            nodeId = usermanager.addNode(objectMapper.readValue(nodeJSON, Node.class)).getId();
            message.setLongProperty(IMessageBus.TYPE_RESULT, nodeId);
            break;
          case UserManager.GET_ALL_NODES:
            message.setStringProperty(IMessageBus.TYPE_RESPONSE, UserManager.GET_ALL_NODES);
            result = objectMapper.writeValueAsString(usermanager.getAllNodes());
            message.setStringProperty(IMessageBus.TYPE_RESULT, result);
            break;
        }
        
      } catch(Exception e){
        String exceptionName = e.getClass().getSimpleName();
        message.setStringProperty(IMessageBus.TYPE_EXCEPTION, exceptionName+": "+e.getMessage());
        
      } finally{
        final String id = rcvMessage.getJMSCorrelationID();
        if(id != null){
          message.setJMSCorrelationID(id);
        }
        this.context.createProducer().send(topic, message);
      }
      
    } catch (final JMSException e) {      
        logger.log(Level.SEVERE, "Issue with JMS", e);
    }
  }
  

}
