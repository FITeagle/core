package org.fiteagle.core.usermanagement.userdatabase;


import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FiteaglePolicyEnforcementPointTest {
  
  private static FiteaglePolicyEnforcementPoint policyEnforcementPoint;  
  private static UserManager usermanager;
  
  private static User FEDERATION_ADMIN_USER;
  private static User NODE_ADMIN_USER;
  private static User CLASSOWNER_USER;
  private static User STUDENT_USER;
  
  @BeforeClass
  public static void createUsersAndMocks(){
    FEDERATION_ADMIN_USER = new User("test", "test", "test", "test@test.de", "test", null, "test", "test", null);
    FEDERATION_ADMIN_USER.setRole(Role.FEDERATION_ADMIN);
    NODE_ADMIN_USER = new User("test", "test", "test", "test@test.de", "test", null, "test", "test", null);
    NODE_ADMIN_USER.setRole(Role.NODE_ADMIN);
    CLASSOWNER_USER = new User("test", "test", "test", "test@test.de", "test", null, "test", "test", null);
    CLASSOWNER_USER.setRole(Role.CLASSOWNER);
    STUDENT_USER = new User("test", "test", "test", "test@test.de", "test", null, "test", "test", null);
    STUDENT_USER.setRole(Role.STUDENT);
    usermanager = createMock(UserManager.class);
    policyEnforcementPoint = new FiteaglePolicyEnforcementPoint(usermanager);
  }
  
  private Boolean authorizeRequest(String subjectUsername, String resource, String action, Role role){
    switch (role) {
      case FEDERATION_ADMIN:
        expect(usermanager.getUser(subjectUsername)).andReturn(FEDERATION_ADMIN_USER);
        break;
      case NODE_ADMIN:
        expect(usermanager.getUser(subjectUsername)).andReturn(NODE_ADMIN_USER);
        break;
      case CLASSOWNER:
        expect(usermanager.getUser(subjectUsername)).andReturn(CLASSOWNER_USER);
        break;
      case STUDENT:
        expect(usermanager.getUser(subjectUsername)).andReturn(STUDENT_USER);
        break;
      default:
        break;
    }
    replay(usermanager);
    Boolean result = policyEnforcementPoint.isRequestAuthorized(subjectUsername, resource, action);
    verify(usermanager);
    reset(usermanager);
    return result;
  }
  
  @Test
  public void permitEverythingForFedAdmins() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/test", "GET", Role.FEDERATION_ADMIN));
    Assert.assertTrue(authorizeRequest("test", "user/another", "POST", Role.FEDERATION_ADMIN));
    Assert.assertTrue(authorizeRequest("test", "user/another/pubkey/key1", "DELETE", Role.FEDERATION_ADMIN));
  }
  
  @Test
  public void authorizeRequestWithoutAction() throws Exception{
    Assert.assertFalse(authorizeRequest("test", "user/test", null, Role.STUDENT));
    Assert.assertFalse(authorizeRequest("test", "user/test", "", Role.STUDENT));
  }
  
  @Test
  public void authorizeGETRequestSameIDs() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/test", "GET", Role.STUDENT));
  }
  
  @Test
  public void authorizeGETRequestDifferntIDs() throws Exception{
    Assert.assertFalse(authorizeRequest("test", "user/another", null, Role.STUDENT));
  }
  
  @Test
  public void authorizePOSTRequestSameIDsAdmin() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/test", "POST", Role.FEDERATION_ADMIN));
  }
  
  @Test
  public void authorizeDELETERequestDifferntIDsAdmin() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/another", "DELETE", Role.FEDERATION_ADMIN));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsAdmin() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/another/role/FEDERATION_ADMIN", "POST", Role.FEDERATION_ADMIN));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsUser() throws Exception{
    Assert.assertFalse(authorizeRequest("test", "user/another/role/FEDERATION_ADMIN", "POST", Role.STUDENT));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsClassOwner() throws Exception{
    Assert.assertFalse(authorizeRequest("test", "user/another/role/FEDERATION_ADMIN", "POST", Role.CLASSOWNER));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresClassOwnerRightsAsStudent() throws Exception{
    Assert.assertFalse(authorizeRequest("test", "user/", "GET", Role.STUDENT));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresClassOwnerRightsAsClassOwner() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/", "GET", Role.CLASSOWNER));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresClassOwnerRightsAsAdmin() throws Exception{
    Assert.assertTrue(authorizeRequest("test", "user/", "GET", Role.FEDERATION_ADMIN));
    Assert.assertTrue(authorizeRequest("test", "user/", "GET", Role.NODE_ADMIN));
  }
}
