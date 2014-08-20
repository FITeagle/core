package org.fiteagle.core.usermanagement.userdatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.fiteagle.api.core.usermanagement.Class;
import org.fiteagle.api.core.usermanagement.Node;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.PublicKeyNotFoundException;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicateEmailException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicatePublicKeyException;
import org.fiteagle.api.core.usermanagement.UserManager.DuplicateUsernameException;
import org.fiteagle.api.core.usermanagement.UserManager.UserNotFoundException;
import org.fiteagle.api.core.usermanagement.UserPublicKey;
import org.fiteagle.core.aaa.authentication.KeyManagement;
import org.fiteagle.core.aaa.authentication.KeyManagement.CouldNotParse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserManagerTest {
  
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
  
  protected static Class CLASS1;
  
  private static Node defaultNode;
  
  private static UserManager manager;
  
  private void createUser1() {
    KEYS1 = new ArrayList<UserPublicKey>();
    try {
      KEYS1.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key1String), "key1", key1String));
      KEYS1.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key2String), "key2", key2String));
    } catch (User.NotEnoughAttributesException | InvalidKeySpecException | NoSuchAlgorithmException | CouldNotParse | IOException e) {
      e.printStackTrace();
    }
    USER1 = new User("test1", "mitja", "nikolaus", "test1@test.org", "mitjasAffiliation", defaultNode, "mitjasPasswordHash", "mitjasPasswordSalt", KEYS1);
  }
  
  private void createUser2() {
    KEYS2 = new ArrayList<UserPublicKey>(); 
    try {
      KEYS2.add(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key3String), "key3", key3String));
    } catch (User.NotEnoughAttributesException | InvalidKeySpecException | NoSuchAlgorithmException | CouldNotParse | IOException e) {
      e.printStackTrace();
    }
    USER2 = new User("test2", "hans", "schmidt", "hschmidt@test.org", "hansAffiliation", defaultNode, "hansPasswordHash", "hansPasswordSalt", KEYS2);
  }
  
  private void createUser3() {
     USER3 = new User("test3", "mitja", "nikolaus", "mitja@test.org", "mitjaAffiliation", defaultNode, "mitjasPasswordHash", "mitjasPasswordSalt", new ArrayList<UserPublicKey>());    
  }
  
  private void createUser4() {
     USER4 = new User("test4", "mitja", "nikolaus", "mitja@test.org", "mitjaAffiliation", defaultNode, "mitjasPasswordHash", "mitjasPasswordSalt", new ArrayList<UserPublicKey>());
  }
  
  private void createAndAddClass1WithUser1(){
    createUser1();
    manager.addUser(USER1);
    CLASS1 = new Class("class1", "my first description");
    CLASS1.addNode(defaultNode);
    manager.addClass(USER1.getUsername(), CLASS1);   
  }
  
  @BeforeClass
  public static void setUp(){
    manager = new InMemoryUserManager();
  }
  
  @Before
  public void addNode(){
    defaultNode = new Node("TU Berlin");
    manager.addNode(defaultNode);
  }
  
  @Test
  public void testGetNode(){
    List<Node> nodes = manager.getAllNodes();
    assertTrue(nodes.size() > 0); 
    manager.getNode(nodes.get(0).getId());
  }
  
  @Test
  public void testGet(){   
    createUser1();
    manager.addUser(USER1);    
    assertTrue(USER1.equals(manager.getUser(USER1)));  
    assertTrue(manager.getAllUsers().size() > 0); 
  }
  
  @Test(expected=DuplicateUsernameException.class)
  public void testAddFails() {
    createUser1();
    createUser2();
    manager.addUser(USER2);
    USER1.setUsername(USER2.getUsername());
    manager.addUser(USER1);
  }

  @Test
  public void testGetUserWhoHasNoKeys() throws DuplicateUsernameException, NoSuchAlgorithmException{
    createUser3();
    manager.addUser(USER3);
    assertTrue(USER3.equals(manager.getUser(USER3)));
  }
  
  @Test(expected=UserNotFoundException.class)
  public void testGetFails() {
    createUser1();
    createUser2();
    manager.addUser(USER1);
    manager.getUser(USER2);
  }
  
  @Test(expected=JPAUserManager.UserNotFoundException.class)
  public void testDelete(){
    createUser1();
    manager.addUser(USER1);    
    manager.deleteUser(USER1);   
    manager.getUser(USER1);
  }
    
  @Test
  public void testUpdate() throws InterruptedException{
    createUser2();
    manager.addUser(USER2);
    Thread.sleep(1);
    manager.updateUser(USER2.getUsername(), "herbert", null, null, null, null, null);
    User updatedUser = manager.getUser(USER2);
    assertTrue("herbert".equals(updatedUser.getFirstName()));
    Date created = updatedUser.getCreated();
    Date lastModified = updatedUser.getLastModified();
    assertTrue(created.before(lastModified));
  }
  
  @Test(expected=UserNotFoundException.class)
  public void testUpdateFails() {
    manager.updateUser("test1", null, null, null, null, null, null);
  }
  
  @Test
  public void testSetRole(){
    createUser1();
    manager.addUser(USER1);
    manager.setRole(USER1.getUsername(), Role.FEDERATION_ADMIN);
    Assert.assertEquals(Role.FEDERATION_ADMIN, manager.getUser(USER1).getRole());
  }
  
  @Test
  public void testAddKey() throws UserNotFoundException, InvalidKeySpecException, NoSuchAlgorithmException, IOException{
    createUser1();
    manager.addUser(USER1);    
    manager.addKey(USER1.getUsername(), new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key4", key4String));
    assertTrue(manager.getUser(USER1).getPublicKeys().contains(new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key4", key4String)));
  }
    
  @Test(expected = DuplicatePublicKeyException.class)
  public void testAddDuplicateKey() {
    createUser1();
    manager.addUser(USER1);  
    manager.addKey(USER1.getUsername(), KEYS1.get(0));
  }
  
  @Test(expected = DuplicatePublicKeyException.class)
  public void testAddDuplicateKeysWithDifferentDescription() throws UserNotFoundException, DuplicatePublicKeyException, InvalidKeySpecException, NoSuchAlgorithmException, IOException{
    createUser1();
    manager.addUser(USER1);  
    manager.addKey(USER1.getUsername(), new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key5", key4String));
    manager.addKey(USER1.getUsername(), new UserPublicKey(KeyManagement.getInstance().decodePublicKey(key4String), "key6", key4String));
  }

  @Test
  public void testDeleteKey() {
    createUser2();
    String key = KEYS2.get(0).getDescription();
    manager.addUser(USER2);
    manager.deleteKey(USER2.getUsername(), key);
    assertTrue(!manager.getUser(USER2).getPublicKeys().contains(key));
  } 
  
  @Test
  public void testRenameKey() {
    createUser2();
    manager.addUser(USER2);
    manager.renameKey(USER2.getUsername(), "key3", "my new description");
    assertEquals("my new description", manager.getUser(USER2).getPublicKeys().get(0).getDescription());
  }
  
  @Test(expected = DuplicatePublicKeyException.class)
  public void testRenameKeyDuplicateDescription() {
    createUser1();
    manager.addUser(USER1);
    manager.renameKey(USER1.getUsername(), "key1", "key2");
  }
  
  @Test(expected = PublicKeyNotFoundException.class)
  public void testRenameKeyNotFound() {
    createUser1();
    manager.addUser(USER1);
    manager.renameKey(USER1.getUsername(), "key5", "my new description");
  }
  
  @Test(expected = DuplicateEmailException.class)
  public void testDuplicateEmailExeptionWhenAdd(){
    createUser3();
    createUser4();
    manager.addUser(USER3);
    manager.addUser(USER4);
  }

  @Test(expected = DuplicateEmailException.class)
  public void testDuplicateEmailExeptionWhenUpdate(){
    createUser1();
    createUser4();
    manager.addUser(USER1);
    manager.addUser(USER4);
    manager.updateUser(USER4.getUsername(), "mitja", "nikolaus", "test1@test.org", "mitjaAffiliation", "mitjasPassword", null);
  }
  
  @Test
  public void testGetClass(){
    createAndAddClass1WithUser1();
    assertTrue(CLASS1.equals(manager.getClass(CLASS1)));
    assertTrue(CLASS1.getNodes().size() > 0);
    assertTrue(manager.getAllClasses().size() > 0); 
    List<Node> nodes = manager.getAllNodes();
    assertTrue(nodes.get(0).getClasses().size() > 0);
  }
  
  @Test(expected=UserManager.FiteagleClassNotFoundException.class)
  public void testDeleteClass(){
    createAndAddClass1WithUser1();
    manager.deleteClass(CLASS1);
    manager.getClass(CLASS1);
  }
  
  @Test
  public void testAddParticipant(){
    createAndAddClass1WithUser1();
    createUser2();
    manager.addUser(USER2);
    manager.addParticipant(CLASS1.getId(), USER2.getUsername());
    assertEquals(manager.getClass(CLASS1).getParticipants().get(0),USER2);
    assertEquals(manager.getUser(USER2).getJoinedClasses().get(0),CLASS1);
    assertEquals(1, manager.getUser(USER2).getJoinedClasses().size());
  }
  
  @Test
  public void testDeleteClassWithParticipant(){
    createAndAddClass1WithUser1();
    createUser2();
    manager.addUser(USER2);
    manager.addParticipant(CLASS1.getId(), USER2.getUsername());
    manager.deleteClass(CLASS1);
    assertTrue(manager.getUser(USER2).getJoinedClasses().isEmpty());
  }
  
  @Test
  public void testDeleteUserWithCourse(){
    createAndAddClass1WithUser1();
    createUser2();
    manager.addUser(USER2);
    manager.addParticipant(CLASS1.getId(), USER2.getUsername());
    manager.deleteUser(USER2);
    assertTrue(manager.getClass(CLASS1).getParticipants().isEmpty());
  }
  
  @After
  public void deleteUsers() {
    try{
      manager.deleteUser("test1");
    }catch (UserNotFoundException e){}
    try{
      manager.deleteUser("test2");
    }catch (UserNotFoundException e){}
    try{
      manager.deleteUser("test3");
    }catch (UserNotFoundException e){}
    try{
      manager.deleteUser("test4");
    }catch (UserNotFoundException e){}
    manager.deleteAllEntries();
  }
  
}
