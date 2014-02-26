//package org.fiteagle.core.persistence.userdatabase;
//
//import org.fiteagle.api.FiteagleUser;
//import org.fiteagle.api.User;
//import org.fiteagle.api.User.Role;
//import org.junit.Assert;
//import org.junit.Test;
//
//public class UserTest {
//  
//  @Test(expected=FiteagleUser.NotEnoughAttributesException.class)
//  public void testCreateUserWithoutPassword() {
//    new FiteagleUser("test1", "test1", "test1", "test1@test.de", "test1", "", null);
//  }
//  
//  @Test
//  public void testCreateDefaultUser(){
//    User user = FiteagleUser.createDefaultUser("test1");
//    Assert.assertEquals("default", user.getAffiliation());
//  }
//  
//  @Test
//  public void testCreateAdminUser(){
//    User user = FiteagleUser.createAdminUser("admin", "admin");
//    Assert.assertEquals(Role.ADMIN, user.getRole());
//  }
//  
//  @Test(expected=FiteagleUser.InValidAttributeException.class)
//  public void testCreateUserWithInvalidUsername(){
//    new FiteagleUser("test!", "test1", "test1", "test1@test.de", "test1", "test1", null);
//  }
//  
//  @Test(expected=FiteagleUser.InValidAttributeException.class)
//  public void testCreaateUserWithInvalidEmail(){
//    new FiteagleUser("test1", "test1", "test1", "te@st1@test.de", "test1", "test1", null);
//  }
//  
//  @Test(expected=FiteagleUser.InValidAttributeException.class)
//  public void testCreateUserWithInvalidEmail2(){
//    new FiteagleUser("test1", "test1", "test1", "test1@testde", "test1", "test1", null);
//  }
//  
//}
