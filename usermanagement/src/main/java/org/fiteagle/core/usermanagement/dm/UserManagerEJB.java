package org.fiteagle.core.usermanagement.dm;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.fiteagle.api.core.usermanagement.Class;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.PublicKeyNotFoundException;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserPublicKey;
import org.fiteagle.core.aaa.authentication.PasswordUtil;

@Stateless
@Remote(UserManager.class)
public class UserManagerEJB implements UserManager{

	private final UserManager usermanager;
	
	public UserManagerEJB() throws NamingException{
	  Context context;
    context = new InitialContext();
    usermanager = (UserManager) context.lookup("java:global/usermanagement/JPAUserManager");
    if(!databaseContainsAdminUser()){
      createFirstAdminUser();
    }
  }
  
  private void createFirstAdminUser() {
//    log.info("Creating First Admin User");
    String[] passwordHashAndSalt = PasswordUtil.generatePasswordHashAndSalt("admin");
    User admin = User.createAdminUser("admin", passwordHashAndSalt[0], passwordHashAndSalt[1]);
    usermanager.add(admin);
  }
  
  private boolean databaseContainsAdminUser() {
    List<User> users = usermanager.getAllUsers();
    for (User u : users) {
      if (u.getRole().equals(Role.FEDERATION_ADMIN)) {
        return true;
      }
    }
    return false;
  }
  
	@Override
	public void add(User user) {
		usermanager.add(user);
	}

	@Override
	public User getUser(User user) {
		return usermanager.getUser(user);
	}

	@Override
	public User getUser(String username) {
		return usermanager.getUser(username);
	}

	@Override
	public void delete(User user) {
		usermanager.delete(user);		
	}

	@Override
	public void delete(String username) {
		usermanager.delete(username);
	}

	@Override
	public void update(String username, String firstName, String lastName,
			String email, String affiliation, String password,
			List<UserPublicKey> publicKeys) {
		usermanager.update(username, firstName, lastName, email, affiliation, password, publicKeys);		
	}

	@Override
	public void setRole(String username, Role role) {
		usermanager.setRole(username, role);		
	}

	@Override
	public void addKey(String username, UserPublicKey publicKey) {
		usermanager.addKey(username, publicKey);
	}

	@Override
	public void deleteKey(String username, String description) {
		usermanager.deleteKey(username, description);		
	}

	@Override
	public void renameKey(String username, String description,
			String newDescription) {
		usermanager.renameKey(username, description, newDescription);
	}

	@Override
	public String createUserKeyPairAndCertificate(String username,
			String passphrase) throws Exception {
		return usermanager.createUserKeyPairAndCertificate(username, passphrase);
	}

	@Override
	public String createUserCertificateForPublicKey(String username,
			String description) throws Exception, PublicKeyNotFoundException {
		return usermanager.createUserCertificateForPublicKey(username, description);
	}

	@Override
	public boolean verifyCredentials(String username, String password)
			throws NoSuchAlgorithmException, IOException, UserNotFoundException {
		return usermanager.verifyCredentials(username, password);
	}

	@Override
	public List<User> getAllUsers() {
		return usermanager.getAllUsers();
	}

	@Override
	public Class addClass(String ownerUsername, Class targetClass) {
		return usermanager.addClass(ownerUsername, targetClass);
	}

	@Override
	public Class get(Class targetClass) {
		return usermanager.get(targetClass);
	}

	@Override
	public Class get(long id) {
		return usermanager.get(id);
	}

	@Override
	public void delete(Class targetClass) {
		usermanager.delete(targetClass);
	}

	@Override
	public void delete(long id) {
		usermanager.delete(id);
	}

	@Override
	public void addParticipant(long id, String username) {
		usermanager.addParticipant(id, username);
	}

	@Override
	public void removeParticipant(long id, String username) {
		usermanager.removeParticipant(id, username);
	}

	@Override
	public List<Class> getAllClassesFromUser(String username) {
		return usermanager.getAllClassesFromUser(username);
	}

	@Override
	public List<Class> getAllClassesOwnedByUser(String username) {
		return usermanager.getAllClassesOwnedByUser(username);
	}

	@Override
	public List<Class> getAllClasses() {
		return usermanager.getAllClasses();
	}

	@Override
	public void deleteAllEntries() {
		usermanager.deleteAllEntries();
	}

}
