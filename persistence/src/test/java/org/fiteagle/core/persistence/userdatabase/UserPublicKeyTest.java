//package org.fiteagle.core.persistence.userdatabase;
//
//import java.io.IOException;
//import java.security.NoSuchAlgorithmException;
//import java.security.spec.InvalidKeySpecException;
//
//import static org.junit.Assert.assertEquals;
//
//import org.fiteagle.api.FiteagleUser;
//import org.fiteagle.api.FiteagleUserPublicKey;
//import org.fiteagle.core.aaa.authentication.KeyManagement;
//import org.junit.Test;
//
//public class UserPublicKeyTest {
//  
//  private static final String PUBLICKEY_STRING = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCLq3fDWATRF8tNlz79sE4Ug5z2r5CLDG353SFneBL5z9Mwoub2wnLey8iqVJxIAE4nJsjtN0fUXC548VedJVGDK0chwcQGVinADbsIAUwpxlc2FGo3sBoGOkGBlMxLc/+5LT1gMH+XD6LljxrekF4xG6ddHTgcNO26VtqQw/VeGw==";
//  private static final String PUBLICKEY_STRING_INVALID = "ssh-rsa AAAAA3NzaC1yc2EABAADAQABAAAAgQCLq3fDWATRF8tNlz79sE4Ug5z2r5CLDG353SFneBL5z9Mwoub2wnLey8iqVJxIAE4nJsjtN0fUXC548VedJVGDK0chwcQGVinADbsIAUwpxlc2FGo3sBoGOkGBlMxLc/+5LT1gMH+XD6LljxrekF4xG6ddHTgcNO26VtqQw/VeGw==";
//
//  @Test(expected=FiteagleUser.NotEnoughAttributesException.class)
//  public void createPublicKeyWithoutDescription() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
//    new FiteagleUserPublicKey(PUBLICKEY_STRING, "");
//  }
//  
//  @Test(expected=FiteagleUser.InValidAttributeException.class)
//  public void createPublicKeyWithInvalidDescription() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
//    new FiteagleUserPublicKey(PUBLICKEY_STRING, "key#1");
//  }
//  
//  @Test(expected=KeyManagement.CouldNotParse.class)
//  public void createPublicKeyWithInvalidKeyString() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException{
//    new FiteagleUserPublicKey(PUBLICKEY_STRING_INVALID, "invalidkey");
//  }
//  
//  @Test
//  public void createPublicKeyWithDifferentConstructors() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException{
//    FiteagleUserPublicKey key1 = new FiteagleUserPublicKey(PUBLICKEY_STRING, "key1");
//    FiteagleUserPublicKey key2 = new FiteagleUserPublicKey(key1.getPublicKey(), "key2");
//    assertEquals(key1, key2);
//    assertEquals(key1.getPublicKeyString(), key2.getPublicKeyString());
//  }
//}
