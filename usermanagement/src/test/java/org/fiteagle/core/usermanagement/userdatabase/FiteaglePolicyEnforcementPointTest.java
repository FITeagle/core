package org.fiteagle.core.usermanagement.userdatabase;


import org.junit.Assert;
import org.junit.Test;

public class FiteaglePolicyEnforcementPointTest {
  
  FiteaglePolicyEnforcementPoint policyEnforcementPoint = new FiteaglePolicyEnforcementPoint();
  
  @Test
  public void authorizeRequestWithoutAction() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", null, "USER", true, false, false));
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", "", "USER", true, false, false));
  }
  
  @Test
  public void authorizePUTRequest() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", "PUT", "USER", false, false, false));
  }
  
  @Test
  public void authorizeGETRequestSameIDs() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "USER", true, false, false));
  }
  
  @Test
  public void authorizeGETRequestDifferntIDs() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "GET", "USER", true, false, false));
  }
  
  @Test
  public void authorizePOSTRequestSameIDsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "ADMIN", true, false, false));
  }
  
  @Test
  public void authorizeDELETERequestDifferntIDsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "DELETE", "ADMIN", true, false, false));
  }
  
  @Test
  public void authorizeGETRequestWithoutAuthentication() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "USER", false, false, false));
  }
  
  @Test
  public void authorizeGETRequestWithoutAuthenticationAdmin() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "GET", "ADMIN", false, false, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "POST", "ADMIN", true, true, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsUser() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "USER", true, true, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsTBOwner() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "TBOWNER", true, true, false));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsUser() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "USER", true, false, true));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsTBOwner() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "TBOWNER", true, false, true));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "ADMIN", true, false, true));
  }
}
