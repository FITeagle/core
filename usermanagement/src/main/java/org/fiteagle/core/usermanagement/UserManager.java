//package org.fiteagle.core.usermanagement;
//
//import java.io.IOException;
//import java.security.InvalidAlgorithmParameterException;
//import java.security.KeyPair;
//import java.security.KeyStoreException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.security.PublicKey;
//import java.security.cert.CertPathValidatorException;
//import java.security.cert.CertificateException;
//import java.security.cert.CertificateParsingException;
//import java.security.cert.X509Certificate;
//import java.security.spec.InvalidKeySpecException;
//import java.sql.SQLException;
//import java.util.Arrays;
//import java.util.List;
//
//import net.iharder.Base64;
//
//import org.fiteagle.api.User;
//import org.fiteagle.api.User.Role;
//import org.fiteagle.api.UserDB;
//import org.fiteagle.api.UserPublicKey;
//import org.fiteagle.core.config.preferences.FiteaglePreferences;
//import org.fiteagle.core.config.preferences.FiteaglePreferencesXML;
//import org.fiteagle.core.config.preferences.InterfaceConfiguration;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class UserManager {
//
//	private FiteaglePreferences preferences = new FiteaglePreferencesXML(this.getClass());
//
//	private static enum databaseType {
//		InMemory, Persistent
//	}
//
//	private static final databaseType DEFAULT_DATABASE_TYPE = databaseType.Persistent;
//	private static UserDB database;
//
//	private static UserManager dbManager;
//	static Logger log = LoggerFactory.getLogger(UserManager.class);
//	private KeyManagement keyManager;
//	private AuthenticationHandler authenticationHandler;
//
//	public static UserManager getInstance() {
//	  if(dbManager == null){
//	    dbManager = new UserManager();
//	  }
//		return dbManager;
//	}
//	
//	private UserManager() {
//		keyManager = KeyManagement.getInstance();
//		authenticationHandler = AuthenticationHandler.getInstance();
//
//		Boolean inTestingMode = Boolean.valueOf(System.getProperty("org.fiteagle.core.usermanagement.UserManager.testing"));
//		if (inTestingMode) {
//			database = JPAUserDB.getInMemoryInstance();
//			return;
//		}
//
//		if (preferences.get("databaseType") == null) {
//			preferences.put("databaseType", DEFAULT_DATABASE_TYPE.name());
//		}
//		if (preferences.get("databaseType").equals(
//				databaseType.Persistent.name())) {
//			database = JPAUserDB.getDerbyInstance();
//		} else {
//			database = JPAUserDB.getInMemoryInstance();
//		}
//		if (!databaseContainsAdminUser()) {
//			createFirstAdminUser();
//		}
//	}
//
//	private void createFirstAdminUser(){
//	  log.info("Creating First Admin User");
//	  User admin;
//    try{
//      admin = User.createAdminUser("admin", "admin");
//    } catch(InValidAttributeException | NotEnoughAttributesException e){
//      log.error(e.getMessage());
//      return;
//    }
//    
//    try{
//      add(admin);
//    }
//    catch(Exception e){
//      log.error(e.getMessage());
//    }
//	}
//
//	private boolean databaseContainsAdminUser(){
//	  List<User> users = database.getAllUsers();
//	  for(User u : users){
//	    if(u.getRole().equals(Role.ADMIN)){
//	      return true;
//	    }
//	  }
//	  return false;
//	}
//	
//  public void add(User u) throws DuplicateUsernameException,
//			DuplicateEmailException,
//			User.NotEnoughAttributesException, User.InValidAttributeException,
//			DuplicatePublicKeyException {
//		String username = addDomain(u.getUsername());
//		u.setUsername(username);
//		database.add(u);
//	}
//
//	public void delete(String username) {
//		username = addDomain(username);
//		database.delete(username);
//	}
//
//	public void delete(User u) {
//		String username = addDomain(u.getUsername());
//		u.setUsername(username);
//		database.delete(u);
//	}
//
//	public void update(String username, String firstName, String lastName, String email, String affiliation, String password, List<UserPublicKey> publicKeys) throws UserNotFoundException,
//			DuplicateEmailException,
//			User.NotEnoughAttributesException, User.InValidAttributeException,
//			DuplicatePublicKeyException {
//		username = addDomain(username);
//		database.update(username, firstName, lastName, email, affiliation, password, publicKeys);
//	}
//
//	public void setRole(String username, Role role){
//	  database.setRole(addDomain(username), role);
//	}
//	
//	public void addKey(String username, UserPublicKey key)
//			throws UserNotFoundException,
//			User.InValidAttributeException, DuplicatePublicKeyException {
//		username = addDomain(username);
//		database.addKey(username, key);
//	}
//
//	public void deleteKey(String username, String description)
//			throws UserNotFoundException {
//		username = addDomain(username);
//		database.deleteKey(username, description);
//	}
//
//	public void renameKey(String username, String description,
//			String newDescription) throws UserNotFoundException,
//			DuplicatePublicKeyException,
//			User.InValidAttributeException, PublicKeyNotFoundException {
//		username = addDomain(username);
//		database.renameKey(username, description, newDescription);
//	}
//
//	public User get(String username) throws UserNotFoundException {
//		username = addDomain(username);
//		return database.get(username);
//	}
//
//	public User get(User u) throws UserNotFoundException {
//		String username = addDomain(u.getUsername());
//		u.setUsername(username);
//		return database.get(u);
//	}
//
//	public User getUserFromCert(X509Certificate userCert) {
//		try {
//			String username = "";
//			username = X509Util.getUserNameFromX509Certificate(userCert);
//
//			User identifiedUser = get(username);
//			return identifiedUser;
//		} catch (CertificateParsingException e1) {
//			throw new RuntimeException(e1);
//		}
//	}
//
//	public X509Certificate createCertificate(X509Certificate xCert)
//			throws Exception {
//		User User = getUserFromCert(xCert);
//		PublicKey pubkey = xCert.getPublicKey();
//		return CertificateAuthority.getInstance().createCertificate(User,
//				pubkey);
//	}
//
//	public boolean verifyPassword(String password, String passwordHash,
//			String passwordSalt) throws IOException, NoSuchAlgorithmException {
//		byte[] passwordHashBytes = Base64.decode(passwordHash);
//		byte[] passwordSaltBytes = Base64.decode(passwordSalt);
//		byte[] proposedDigest = createHash(passwordSaltBytes, password);
//		return Arrays.equals(passwordHashBytes, proposedDigest);
//	}
//
//	public boolean verifyCredentials(String username, String password)
//			throws NoSuchAlgorithmException, IOException,
//			UserNotFoundException {
//		username = addDomain(username);
//		User User = get(username);
//		return verifyPassword(password, User.getPasswordHash(), User.getPasswordSalt());
//	}
//
//	private String createUserCertificate(String username, PublicKey publicKey)
//			throws Exception {
//		User u = get(username);
//		CertificateAuthority ca = CertificateAuthority.getInstance();
//		X509Certificate cert = ca.createCertificate(u, publicKey);
//		return X509Util.getCertficateEncoded(cert);
//	}
//
//	private byte[] createHash(byte[] salt, String password)
//			throws NoSuchAlgorithmException {
//		MessageDigest digest = MessageDigest.getInstance("SHA-256");
//		digest.reset();
//		digest.update(salt);
//		return digest.digest(password.getBytes());
//	}
//
//	public String createUserCertificate(String username, String passphrase, KeyPair keyPair) throws Exception {
//		username = addDomain(username);
//		String pubKeyEncoded = keyManager.encodePublicKey(keyPair.getPublic());
//		addKey(username, new UserPublicKey(pubKeyEncoded, "created at " + System.currentTimeMillis()));
//		String userCertString = createUserCertificate(username,	keyPair.getPublic());
//		String privateKeyEncoded = keyManager.encryptPrivateKey(keyPair.getPrivate(), passphrase);
//		return privateKeyEncoded + "\n" + userCertString;
//	}
//
//	public String createUserKeyPairAndCertificate(String username, String passphrase) throws Exception{
//	  return createUserCertificate(username, passphrase, keyManager.generateKeyPair());
//	}
//	
//	public String createUserCertificateForPublicKey(String username, String description) throws Exception, PublicKeyNotFoundException {
//		username = addDomain(username);
//		PublicKey publicKey = get(username).getPublicKey(description).getPublicKey();
//		return createUserCertificate(username, publicKey);
//	}
//	
//	public boolean areAuthenticatedCertificates(X509Certificate[] certificates) throws KeyStoreException,
//    NoSuchAlgorithmException, CertificateException, IOException, InvalidAlgorithmParameterException,
//    CertPathValidatorException, SQLException, InvalidKeySpecException, CouldNotParse {
// 
//  X509Certificate cert = certificates[0];
//  try{
//  if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
//    
//    User identifiedUser = getUserFromCert(cert);
//    return verifyUserSignedCertificate(identifiedUser, cert);
//    
//  } else {
//    
//    if(authenticationHandler.isValid(0, certificates, certificates[0].getSubjectX500Principal())){
////      if(userIsUnknown(cert))
////        storeNewUser(cert);
//  	  return true;
//    }
//
//  }
//  }catch(RuntimeException e){
//  	return false;
//  }
//	return false;
//  
//	}
//	
//	private boolean verifyUserSignedCertificate(User identifiedUser, X509Certificate certificate) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, CouldNotParse {
//	    boolean verified = false;
//	    KeyManagement keydecoder = KeyManagement.getInstance();
//	    if (identifiedUser.getPublicKeys() == null || identifiedUser.getPublicKeys().size() == 0) {
//	      identifiedUser.addPublicKey(new UserPublicKey(keydecoder.encodePublicKey(certificate.getPublicKey()), null));
//	      addKey(identifiedUser.getUsername(), identifiedUser.getPublicKeys().get(0));
//	    }
//	    for (UserPublicKey oldUserPublicKey : identifiedUser.getPublicKeys()) {      
//	      PublicKey pubKey = oldUserPublicKey.getPublicKey();
//	      
//	      verified = authenticationHandler.verifyCertificateWithPublicKey(certificate, pubKey);
//	      if (verified) {
//	        return true;
//	      }
//	    }
//	    throw new AuthenticationHandler.KeyDoesNotMatchException();
//	  }
//
//	private String addDomain(String username) {
//		InterfaceConfiguration configuration = null;
//		if (!username.contains("@")) {
//			configuration = InterfaceConfiguration.getInstance();
//			username = username + "@" + configuration.getDomain();
//		}
//		return username;
//	}
//}
