package org.fiteagle.core.aaa.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.iharder.Base64;

import org.fiteagle.api.core.usermanagement.User.InValidAttributeException;

public class PasswordUtil {
  
  private final static int MINIMUM_PASSWORD_LENGTH = 3;
	
  public static String[] generatePasswordHashAndSalt(String password){
    byte[] salt = generatePasswordSalt();
    String passwordSalt = Base64.encodeBytes(salt);        
    String passwordHash = generatePasswordHash(salt, password);
    if(passwordHash == null || passwordSalt == null){
    	return new String[] {"", ""};
    }
    return new String[] {passwordHash, passwordSalt};
  }
  
  private static byte[] generatePasswordSalt(){
    SecureRandom random = new SecureRandom();
    return random.generateSeed(20);
  }

  private static String generatePasswordHash(byte[] salt, String password){
	  if(password == null || password.length() < MINIMUM_PASSWORD_LENGTH){
	    return null;
	  }
	  
	  byte[] passwordBytes = null;
	  try {
	    passwordBytes = createHash(salt, password);
	  } catch (NoSuchAlgorithmException e) {
		  throw new InValidAttributeException("could not generate passwordHash");
	  }
	  return Base64.encodeBytes(passwordBytes);
  }

  private static byte[] createHash(byte[] salt, String password) throws NoSuchAlgorithmException {    
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.reset();
    digest.update(salt);
    return digest.digest(password.getBytes());
  }
  
}
