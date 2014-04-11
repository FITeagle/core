package org.fiteagle.core.usermanagement.userdatabase;


import org.junit.Assert;
import org.junit.Test;

public class FiteaglePolicyEnforcementPointTest {
  
  FiteaglePolicyEnforcementPoint policyEnforcementPoint = new FiteaglePolicyEnforcementPoint();
  
  @Test
  public void authorizeRequestWithoutAction() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", null, "STUDENT", true, false, false));
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", "", "STUDENT", true, false, false));
  }
  
  @Test
  public void authorizePUTRequest() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", "PUT", "STUDENT", false, false, false));
  }
  
  @Test
  public void authorizeGETRequestSameIDs() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "STUDENT", true, false, false));
  }
  
  @Test
  public void authorizeGETRequestDifferntIDs() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "GET", "STUDENT", true, false, false));
  }
  
  @Test
  public void authorizePOSTRequestSameIDsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "FEDERATION_ADMIN", true, false, false));
  }
  
  @Test
  public void authorizeDELETERequestDifferntIDsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "DELETE", "FEDERATION_ADMIN", true, false, false));
  }
  
  @Test
  public void authorizeGETRequestWithoutAuthentication() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "STUDENT", false, false, false));
  }
  
  @Test
  public void authorizeGETRequestWithoutAuthenticationAdmin() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "GET", "FEDERATION_ADMIN", false, false, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "POST", "FEDERATION_ADMIN", true, true, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsUser() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "STUDENT", true, true, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsTBOwner() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "CLASSOWNER", true, true, false));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsUser() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "STUDENT", true, false, true));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsTBOwner() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "CLASSOWNER", true, false, true));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "FEDERATION_ADMIN", true, false, true));
  }
}
