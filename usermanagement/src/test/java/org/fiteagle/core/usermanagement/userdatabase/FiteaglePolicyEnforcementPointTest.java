package org.fiteagle.core.usermanagement.userdatabase;


import org.junit.Assert;
import org.junit.Test;

public class FiteaglePolicyEnforcementPointTest {
  
  FiteaglePolicyEnforcementPoint policyEnforcementPoint = new FiteaglePolicyEnforcementPoint();
  
  @Test
  public void authorizeRequestWithoutAction() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", null, "STUDENT", false, false));
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("anyUser", "mnikolaus", "", "STUDENT", false, false));
  }
  
  @Test
  public void authorizeGETRequestSameIDs() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "STUDENT", false, false));
  }
  
  @Test
  public void authorizeGETRequestDifferntIDs() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "GET", "STUDENT", false, false));
  }
  
  @Test
  public void authorizePOSTRequestSameIDsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "FEDERATION_ADMIN", false, false));
  }
  
  @Test
  public void authorizeDELETERequestDifferntIDsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "DELETE", "FEDERATION_ADMIN", false, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "anotherUser", "POST", "FEDERATION_ADMIN", true, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsUser() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "STUDENT", true, false));
  }
  
  @Test
  public void authorizePOSTRequestWhichRequiresAdminRightsAsTBOwner() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "POST", "CLASSOWNER", true, false));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsUser() throws Exception{
    Assert.assertFalse(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "STUDENT", false, true));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsTBOwner() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "CLASSOWNER", false, true));
  }
  
  @Test
  public void authorizeGETRequestWhichRequiresTBOwnerRightsAsAdmin() throws Exception{
    Assert.assertTrue(policyEnforcementPoint.isRequestAuthorized("mnikolaus", "mnikolaus", "GET", "FEDERATION_ADMIN", false, true));
  }
}
