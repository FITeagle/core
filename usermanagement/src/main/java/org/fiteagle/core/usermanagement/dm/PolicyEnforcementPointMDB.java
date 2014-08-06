package org.fiteagle.core.usermanagement.dm;

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
import javax.naming.NamingException;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.usermanagement.PolicyEnforcementPoint;
import org.fiteagle.core.usermanagement.userdatabase.FiteaglePolicyEnforcementPoint;


@MessageDriven(name="PolicyEnforcementPointMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = IMessageBus.TOPIC_CORE),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = PolicyEnforcementPoint.MESSAGE_FILTER),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
    })
public class PolicyEnforcementPointMDB implements MessageListener {

  private PolicyEnforcementPoint policyEnforcementPoint;
  
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private final static Logger logger = Logger.getLogger(PolicyEnforcementPointMDB.class.toString());
  
  public PolicyEnforcementPointMDB() throws NamingException{
    policyEnforcementPoint = new FiteaglePolicyEnforcementPoint();
  }
  
  
  @Override
  public void onMessage(final Message rcvMessage) {
    try {
      String methodName = rcvMessage.getStringProperty(IMessageBus.TYPE_REQUEST);
      logger.info("Received a message: "+methodName);
      if(methodName == null){
        return;
      }
      final Message message = this.context.createMessage();
      final String id = rcvMessage.getJMSCorrelationID();
      
      switch(methodName){
        case PolicyEnforcementPoint.IS_REQUEST_AUTHORIZED:
          message.setStringProperty(IMessageBus.TYPE_RESPONSE, PolicyEnforcementPoint.IS_REQUEST_AUTHORIZED);
          String subjectUsername = rcvMessage.getStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_SUBJECT_USERNAME);
          String resourceUsername = rcvMessage.getStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_RESOURCE_USERNAME);
          String action = rcvMessage.getStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_ACTION);
          String role = rcvMessage.getStringProperty(PolicyEnforcementPoint.TYPE_PARAMETER_ROLE);
          Boolean isAuthenticated = rcvMessage.getBooleanProperty(PolicyEnforcementPoint.TYPE_PARAMETER_IS_AUTHENTICATED);
          Boolean requiresAdminRights = rcvMessage.getBooleanProperty(PolicyEnforcementPoint.TYPE_PARAMETER_REQUIRES_ADMIN_RIGHTS);
          Boolean requiresTBOwnerRights = rcvMessage.getBooleanProperty(PolicyEnforcementPoint.TYPE_PARAMETER_REQUIRES_TBOWNER_RIGHTS);
          Boolean isAuthorized = policyEnforcementPoint.isRequestAuthorized(subjectUsername, resourceUsername, action, role, isAuthenticated, requiresAdminRights, requiresTBOwnerRights);
          message.setBooleanProperty(IMessageBus.TYPE_RESULT, isAuthorized);
          break;
      }
      
      if(id != null){
        message.setJMSCorrelationID(id);
      }
      this.context.createProducer().send(topic, message);
      
    } catch (final JMSException e) {      
        logger.log(Level.SEVERE, "Issue with JMS", e);
    }
  }

}
