package org.fiteagle.core.usermanagement.userdatabase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.fiteagle.api.core.usermanagement.PolicyEnforcementPoint;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

public class FiteaglePolicyEnforcementPoint implements PolicyEnforcementPoint {
  
  private final static Logger LOGGER = Logger.getLogger(FiteaglePolicyEnforcementPoint.class.toString());
  
  private static URI SUBJECT_ID;
  private static URI RESOURCE_ID;
  private static URI ACTION_ID;
  
  static{
    try {
      SUBJECT_ID = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
      RESOURCE_ID = new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id");
      ACTION_ID = new URI("urn:oasis:names:tc:xacml:1.0:action:action-id");
    } catch (URISyntaxException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }
  
  private UserManager usermanager;
  
  protected FiteaglePolicyEnforcementPoint(UserManager usermanager){
    this.usermanager = usermanager;
  }
  
  private FiteaglePolicyEnforcementPoint(){
    usermanager = JPAUserManager.getInstance();
  };
  
  private static FiteaglePolicyEnforcementPoint instance = null;
  
  public static FiteaglePolicyEnforcementPoint getInstance(){
    if(instance == null){
      instance = new FiteaglePolicyEnforcementPoint();
    }
    return instance;
  }  
  
  private static PolicyDecisionPoint policyDecisionPoint = PolicyDecisionPoint.getInstance();
  
  @Override
  public boolean isRequestAuthorized(String subjectUsername, String resource, String action) {
    subjectUsername = JPAUserManager.addDomain(subjectUsername);
    if(resource.startsWith("class")){
      if(action.equals("GET")){
        return true;
      }
      if(subjectUsername == null){
        return false;
      }
      Role role = Role.STUDENT;
      try { 
        role = getRole(subjectUsername);
      } catch (Exception e) {
        return false;
      }
      if(role.equals(Role.STUDENT)){
        return false;
      }
      return true;
    }   
    if(resource.startsWith("node")){
      if(!action.equals("GET")){
        if(subjectUsername == null){
          return false;
        }
        Role role;
        try { 
          role = getRole(subjectUsername);
        } catch (Exception e) {
          return false;
        }
        if(role.equals(Role.STUDENT) || role.equals(Role.CLASSOWNER)){
          return false;
        }
      }
      return true;
    }
    
    RequestCtx request = createRequest(subjectUsername, resource, action);
    return policyDecisionPoint.evaluateRequest(request);
  }
  
  private Role getRole(String subjectUsername){
    Role role = Role.STUDENT;
    role = usermanager.getUser(subjectUsername).getRole();
    return role;
  }
  
  protected String getUsername(String resource) {
    String[] splitted = resource.split("/");
    for (int i = 0; i < splitted.length - 1; i++) {
      if (splitted[i].equals("user")) {
        return JPAUserManager.addDomain(splitted[i+1]);
      }
    }
    return "";
  }
  
  private Boolean requiresAdminRights(String resource) {
    if(resource.endsWith("/role/FEDERATION_ADMIN") || resource.endsWith("/role/CLASSOWNER") || resource.endsWith("/role/NODE_ADMIN")){
      return true;
    }
    return false;
  }
  
  private Boolean requiresClassOwnerRights(String resource) {
    if(resource.endsWith("user/") || resource.endsWith("user") ){
      return true;
    }
    return false;
  }
  
  private RequestCtx createRequest(String subjectUsername, String resource, String action) {
    RequestCtx request = null;
    try {
      request = new RequestCtx(
          setSubject(subjectUsername, getRole(subjectUsername).name()),
          setResource(getUsername(resource)),
          setAction(action),
          setEnvironment(requiresAdminRights(resource), requiresClassOwnerRights(resource)));
    } catch (URISyntaxException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    return request;
  }

  private Set<Subject> setSubject(String subjectUsername, String role) throws URISyntaxException {
    HashSet<Attribute> attributeSet = new HashSet<Attribute>();

    attributeSet.add(new Attribute(SUBJECT_ID, null, null, new StringAttribute(subjectUsername)));

    attributeSet.add(new Attribute(new URI("role"), null, null, new StringAttribute(role)));

    HashSet<Subject> subjects = new HashSet<Subject>();
    subjects.add(new Subject(attributeSet));

    return subjects;
  }

  private Set<Attribute> setResource(String resourceUsername) throws URISyntaxException {
    HashSet<Attribute> resourceSet = new HashSet<Attribute>();

    resourceSet.add(new Attribute(RESOURCE_ID, null, null, new StringAttribute(resourceUsername)));

    return resourceSet;
  }
  private Set<Attribute> setAction(String action) throws URISyntaxException {
    HashSet<Attribute> actionSet = new HashSet<Attribute>();

    actionSet.add(new Attribute(ACTION_ID, null, null, new StringAttribute(action)));

    return actionSet;
  }
  
  private Set<Attribute> setEnvironment(Boolean requiresAdminRights, Boolean requiresClassOwnerRights) throws URISyntaxException {
    HashSet<Attribute> environmentSet = new HashSet<Attribute>();

    environmentSet.add(new Attribute(new URI("requiresAdminRights"), null, null, BooleanAttribute.getInstance(requiresAdminRights)));
    environmentSet.add(new Attribute(new URI("requiresClassOwnerRights"), null, null, BooleanAttribute.getInstance(requiresClassOwnerRights)));
    
    return environmentSet;
  }
  
}
