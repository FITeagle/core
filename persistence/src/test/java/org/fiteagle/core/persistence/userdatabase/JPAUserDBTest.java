package org.fiteagle.core.persistence.userdatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;

import org.fiteagle.api.UserPublicKey;
import org.fiteagle.api.User;
import org.fiteagle.api.User.PublicKeyNotFoundException;
import org.fiteagle.api.User.Role;
import org.fiteagle.api.UserDB;
import org.fiteagle.api.UserDB.DuplicateEmailException;
import org.fiteagle.api.UserDB.DuplicatePublicKeyException;
import org.fiteagle.api.UserDB.DuplicateUsernameException;
import org.fiteagle.api.UserDB.UserNotFoundException;
import org.fiteagle.core.aaa.authentication.KeyManagement;
import org.fiteagle.core.aaa.authentication.KeyManagement.CouldNotParse;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JPAUserDBTest {
  
  private final static String key1String = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCarsTCyAf8gYXwei8rhJnLTqYI6P88weRaY5dW9j3DT4mvfQPna79Bjq+uH4drmjbTD2n3s3ytqupFfNko1F0+McstA2EEkO8pAo5NEPcreygUcB2d49So032GKGPETB8chRkDsaBCm/KKL2vXdQoicofli8JJRPK2kXfUW34qww==";
  private final static String key2String = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCOHoq0DYsW793kyhbW1sj6aNm5OWeRn3HQ6nZxU9ax3FnDmtJsxvq2u0RwtPQki5JEMG58aqJPs3s4Go6LrTyw4jqnodKyOfcFupUYHTbQYnzxudLwyU59RfBmH01cLiyu26ECdVNXX+Y1mgofRUx72thBTtY6vyuM5nR1l7UNTw==";
  private final static String key3String = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDKpQJGxnReKal3p7d/95G5d3RQb002gso1QJrjxFKED+1cD157FsT2bCPcWpTYdLeTFRWBDUQa91yUPdkjkvoMsL2e3ah7nugRD6QfrFki0Po9oENrbujzaExPV8SAvXSuqcCG4/pidqEqjXJlAMXrphJcoFdKSzXLJtjUwfxyEw==";
  private final static String key4String = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCQf/Ub9v6jR/8C58zC2MMakX5sOHfpl6asymHBnYBQ5xqL+P94A3lrViXRbss/G4ozBgGINvshdLAMjclmwgK7wSOcTlIAORhggU+iBM7V+YCa5Dj0gR0mMzDBxL71l9dCQ3wL+GWMI/bwoeuq+83rLes1T1Yyk7Fp27gR+P05VQ==";
      
  protected static ArrayList<UserPublicKey> KEYS1;
  protected static ArrayList<UserPublicKey> KEYS2; 
  protected static User USER1;
  protected static User USER2;
  protected static User USER3;
  protected static User USER4;
  
  private static UserDB manager;
  
  private void createUser1() {
    KEYS1 = new ArrayList<UserPublicKey>();
    try {
      KEYS1.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key1String), "key1", key1String));
      KEYS1.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key2String), "key2", key2String));
    } catch (User.NotEnoughAttributesException | InvalidKeySpecException | NoSuchAlgorithmException | CouldNotParse | IOException e) {
      e.printStackTrace();
    }
    USER1 = new User("test1", "mitja", "nikolaus", "test1@test.org", "mitjasAffiliation", "mitjasPassword", KEYS1);
  }
  
  private void createUser2() {
    KEYS2 = new ArrayList<UserPublicKey>(); 
    try {
      KEYS2.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key3String), "key3", key3String));
    } catch (User.NotEnoughAttributesException | InvalidKeySpecException | NoSuchAlgorithmException | CouldNotParse | IOException e) {
      e.printStackTrace();
    }
    USER2 = new User("test2", "hans", "schmidt", "hschmidt@test.org", "hansAffiliation", "hansPassword", KEYS2);
  }
  
  private void createUser3() {
     USER3 = new User("test3", "mitja", "nikolaus", "mitja@test.org", "mitjaAffiliation", "mitjasPassword", new ArrayList<UserPublicKey>());    
  }
  
  private void createUser4() {
     USER4 = new User("test4", "mitja", "nikolaus", "mitja@test.org", "mitjaAffiliation", "mitjasPassword", new ArrayList<UserPublicKey>());
  }
  
  
  @BeforeClass
  public static void setUp(){
    manager = JPAUserDB.getInMemoryInstance();
  }
  
//  @Test
//  public void testGet(){   
//    createUser1();
//    manager.add(USER1);    
//    assertTrue(USER1.equals(manager.get(USER1)));  
//    assertTrue(manager.getAllUsers().size() > 0); 
//  }
  
  @Test(expected=DuplicateUsernameException.class)
  public void testAddFails() {
    createUser1();
    createUser2();
    manager.add(USER2);
    USER1.setUsername(USER2.getUsername());
    manager.add(USER2);
  }

  @Test
  public void testGetUserWhoHasNoKeys() throws DuplicateUsernameException, NoSuchAlgorithmException{
    createUser3();
    manager.add(USER3);
    assertTrue(USER3.equals(manager.get(USER3)));
  }
  
  @Test(expected=UserNotFoundException.class)
  public void testGetFails() {
    createUser1();
    createUser2();
    manager.add(USER1);
    manager.get(USER2);
  }
  
  @Test(expected=JPAUserDB.UserNotFoundException.class)
  public void testDelete(){
    createUser1();
    manager.add(USER1);    
    manager.delete(USER1);   
    manager.get(USER1);
  }
    
  @Test
  public void testUpdate() throws InterruptedException{
    createUser2();
    manager.add(USER2);
    Thread.sleep(1);
    manager.update(USER2.getUsername(), "herbert", null, null, null, null, null);
    User updatedUser = manager.get(USER2);
    assertTrue("herbert".equals(updatedUser.getFirstName()));
    Date created = updatedUser.getCreated();
    Date lastModified = updatedUser.getLastModified();
    assertTrue(created.before(lastModified));
  }
  
  @Test(expected=UserNotFoundException.class)
  public void testUpdateFails() {
    manager.update("test1", null, null, null, null, null, null);
  }
  
  @Test
  public void testSetRole(){
    createUser1();
    manager.add(USER1);
    manager.setRole(USER1.getUsername(), Role.ADMIN);
    Assert.assertEquals(Role.ADMIN, manager.get(USER1).getRole());
  }
  
  @Test
  public void testAddKey() throws UserNotFoundException, InvalidKeySpecException, NoSuchAlgorithmException, IOException{
    createUser1();
    manager.add(USER1);    
    manager.addKey(USER1.getUsername(), new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key4", key4String));
    assertTrue(manager.get(USER1).getPublicKeys().contains(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key4", key4String)));
  }
    
  @Test(expected = DuplicatePublicKeyException.class)
  public void testAddDuplicateKey() {
    createUser1();
    manager.add(USER1);  
    manager.addKey(USER1.getUsername(), KEYS1.get(0));
  }
  
  @Test(expected = DuplicatePublicKeyException.class)
  public void testAddDuplicateKeysWithDifferentDescription() throws UserNotFoundException, DuplicatePublicKeyException, InvalidKeySpecException, NoSuchAlgorithmException, IOException{
    createUser1();
    manager.add(USER1);  
    manager.addKey(USER1.getUsername(), new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key5", key4String));
    manager.addKey(USER1.getUsername(), new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key6", key4String));
  }

  @Test
  public void testDeleteKey() {
    createUser2();
    String key = KEYS2.get(0).getDescription();
    manager.add(USER2);
    manager.deleteKey(USER2.getUsername(), key);
    assertTrue(!manager.get(USER2).getPublicKeys().contains(key));
  } 
  
  @Test
  public void testRenameKey() {
    createUser2();
    manager.add(USER2);
    manager.renameKey(USER2.getUsername(), "key3", "my new description");
    assertEquals("my new description", manager.get(USER2).getPublicKeys().get(0).getDescription());
  }
  
  @Test(expected = DuplicatePublicKeyException.class)
  public void testRenameKeyDuplicateDescription() {
    createUser1();
    manager.add(USER1);
    manager.renameKey(USER1.getUsername(), "key1", "key2");
  }
  
  @Test(expected = PublicKeyNotFoundException.class)
  public void testRenameKeyNotFound() {
    createUser1();
    manager.add(USER1);
    manager.renameKey(USER1.getUsername(), "key5", "my new description");
  }
  
//  @Test(expected = DuplicateEmailException.class)
//  public void testDuplicateEmailExeptionWhenAdd(){
//    createUser3();
//    createUser4();
//    manager.add(USER3);
//    manager.add(USER4);
//  }

  @Test(expected = DuplicateEmailException.class)
  public void testDuplicateEmailExeptionWhenUpdate(){
    createUser1();
    createUser4();
    manager.add(USER1);
    manager.add(USER4);
    manager.update(USER4.getUsername(), "mitja", "nikolaus", "test1@test.org", "mitjaAffiliation", "mitjasPassword", null);
  }
  
  @After
  public void deleteUsers() {
    try{
      manager.delete("test1");
    }catch (UserNotFoundException e){}
    try{
      manager.delete("test2");
    }catch (UserNotFoundException e){}
    try{
      manager.delete("test3");
    }catch (UserNotFoundException e){}
    try{
      manager.delete("test4");
    }catch (UserNotFoundException e){}
  }   
  
}
