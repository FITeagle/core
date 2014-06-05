package org.fiteagle.core.usermanagement.userdatabase;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import net.iharder.Base64;

import org.bouncycastle.operator.OperatorCreationException;
import org.fiteagle.api.core.usermanagement.Class;
import org.fiteagle.api.core.usermanagement.User;
import org.fiteagle.api.core.usermanagement.User.Role;
import org.fiteagle.api.core.usermanagement.UserManager;
import org.fiteagle.api.core.usermanagement.UserPublicKey;
import org.fiteagle.core.aaa.authentication.AuthenticationHandler;
import org.fiteagle.core.aaa.authentication.CertificateAuthority;
import org.fiteagle.core.aaa.authentication.KeyManagement;
import org.fiteagle.core.aaa.authentication.KeyManagement.CouldNotParse;
import org.fiteagle.core.aaa.authentication.PasswordUtil;
import org.fiteagle.core.aaa.authentication.x509.X509Util;
import org.fiteagle.core.config.preferences.InterfaceConfiguration;

@Stateless
public class JPAUserManager implements UserManager {
  
  private static final String PERSISTENCE_UNIT_NAME_INMEMORY = "users_inmemory";
  
  private static UserManager inMemoryInstance;
  private static UserManager instance;
  
  public JPAUserManager() {
  }
  
  @PersistenceContext(unitName = "usersDB")
  EntityManager entityManager;

  public static UserManager getInMemoryInstance() {
    if (inMemoryInstance == null) {
      inMemoryInstance = new JPAUserManager();
    }
    return inMemoryInstance;
  }
  
  public static UserManager getInstance(){
    if(instance == null){
      instance = new JPAUserManager();
    }
    return instance;
  }
  
  static {
    try {
      java.lang.Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private synchronized EntityManager getEntityManager() {
    if (entityManager == null) {
      EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME_INMEMORY);
      entityManager = factory.createEntityManager();
    }
    return entityManager;
  }
  
  private void beginTransaction(EntityManager em) {
    if (this == inMemoryInstance) {
      em.getTransaction().begin();
    }
  }
  
  private void commitTransaction(EntityManager em) {
    if (this == inMemoryInstance) {
      em.getTransaction().commit();
    }
  }
  
  @Override
  public void add(User user) {
    EntityManager em = getEntityManager();
    
    user.setUsername(addDomain(user.getUsername()));
    
    List<User> users = getAllUsers();
    for (User u : users) {
      if (u.getUsername().equals(user.getUsername())) {
        throw new DuplicateUsernameException();
      }
      if (u.getEmail().equals(user.getEmail())) {
        throw new DuplicateEmailException();
      }
    }
    
    beginTransaction(em);
    em.persist(user);
    commitTransaction(em);
  }
  
  @Override
  public User getUser(User user) throws UserNotFoundException {
    return get(user.getUsername());
  }
  
  @Override
  public User get(String username) throws UserNotFoundException {
    EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(username));
    if (user == null) {
      throw new UserNotFoundException();
    }
    return user;
  }
  
  @Override
  public void delete(User user) {
    EntityManager em = getEntityManager();
    user.setUsername(addDomain(user.getUsername()));
    beginTransaction(em);
    em.remove(em.merge(user));
    commitTransaction(em);
  }
  
  @Override
  public void delete(String username) {
    delete(get(username));
  }
  
  @Override
  public void update(String username, String firstName, String lastName, String email, String affiliation,
      String password, List<UserPublicKey> publicKeys) {
    EntityManager em = getEntityManager();
    
    User user = em.find(User.class, addDomain(username));
    if (user == null) {
      throw new UserNotFoundException();
    }
    
    List<User> users = getAllUsers();
    for (User u : users) {
      if (u.getEmail().equals(email) && !u.getUsername().equals(addDomain(username))) {
        throw new DuplicateEmailException();
      }
    }
    
    beginTransaction(em);
    String[] passwordHashAndSalt = PasswordUtil.generatePasswordHashAndSalt(password);
    user.updateAttributes(firstName, lastName, email, affiliation, passwordHashAndSalt[0], passwordHashAndSalt[1], publicKeys);
    commitTransaction(em);
  }
  
  @Override
  public void setRole(String username, Role role) {
    EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(username));
    if (user == null) {
      throw new UserNotFoundException();
    }
    beginTransaction(em);
    user.setRole(role);
    commitTransaction(em);
  }
  
  @Override
  public void addKey(String username, UserPublicKey publicKey) {
    EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(username));
    if (user == null) {
      throw new UserNotFoundException();
    }
    if (user.getPublicKeys().contains(publicKey)) {
      throw new DuplicatePublicKeyException();
    }
    beginTransaction(em);
    user.addPublicKey(publicKey);
    commitTransaction(em);
  }
  
  @Override
  public void deleteKey(String username, String description) {
    EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(username));
    if (user == null) {
      throw new UserNotFoundException();
    }
    beginTransaction(em);
    user.deletePublicKey(description);
    commitTransaction(em);
  }
  
  @Override
  public void renameKey(String username, String description, String newDescription) {
    EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(username));
    if (user == null) {
      throw new UserNotFoundException();
    }
    if (user.hasKeyWithDescription(newDescription)) {
      throw new DuplicatePublicKeyException();
    }
    beginTransaction(em);
    try {
      user.renamePublicKey(description, newDescription);
    } finally {
      commitTransaction(em);
    }
  }
  
  public User getUserFromCert(X509Certificate userCert) {
    try {
      String username = "";
      username = X509Util.getUserNameFromX509Certificate(userCert);
      
      User identifiedUser = get(username);
      return identifiedUser;
    } catch (CertificateParsingException e1) {
      throw new RuntimeException(e1);
    }
  }
  
  public X509Certificate createCertificate(X509Certificate xCert) throws Exception {
    User User = getUserFromCert(xCert);
    PublicKey pubkey = xCert.getPublicKey();
    return CertificateAuthority.getInstance().createCertificate(User, pubkey);
  }
  
  public boolean verifyPassword(String password, String passwordHash, String passwordSalt) throws IOException,
      NoSuchAlgorithmException {
    byte[] passwordHashBytes = Base64.decode(passwordHash);
    byte[] passwordSaltBytes = Base64.decode(passwordSalt);
    byte[] proposedDigest = createHash(passwordSaltBytes, password);
    return Arrays.equals(passwordHashBytes, proposedDigest);
  }
  
  @Override
  public boolean verifyCredentials(String username, String password) throws NoSuchAlgorithmException, IOException,
      UserNotFoundException {
    username = addDomain(username);
    User User = get(username);
    return verifyPassword(password, User.hash(), User.salt());
  }
  
  private String createUserCertificate(String username, PublicKey publicKey) {
    User u = get(username);
    CertificateAuthority ca = CertificateAuthority.getInstance();
    X509Certificate cert;
    String encoded = "";
    try {
      cert = ca.createCertificate(u, publicKey);
      encoded = X509Util.getCertficateEncoded(cert);
    } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableEntryException
        | OperatorCreationException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return encoded;
  }
  
  private byte[] createHash(byte[] salt, String password) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.reset();
    digest.update(salt);
    return digest.digest(password.getBytes());
  }
  
  public String createUserCertificate(String username, String passphrase, KeyPair keyPair) throws IOException,
      GeneralSecurityException {
    username = addDomain(username);
    String pubKeyEncoded = KeyManagement.getInstance().encodePublicKey(keyPair.getPublic());
    addKey(username, new UserPublicKey(keyPair.getPublic(), "created at " + System.currentTimeMillis(), pubKeyEncoded));
    String userCertString = createUserCertificate(username, keyPair.getPublic());
    String privateKeyEncoded = KeyManagement.getInstance().encryptPrivateKey(keyPair.getPrivate(), passphrase);
    return privateKeyEncoded + "\n" + userCertString;
  }
  
  public String createUserKeyPairAndCertificate(String username, String passphrase) {
    String cert = "";
    try {
      cert = createUserCertificate(addDomain(username), passphrase, KeyManagement.getInstance().generateKeyPair());
    } catch (IOException | GeneralSecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return cert;
  }
  
  public String createUserCertificateForPublicKey(String username, String description) {
    username = addDomain(username);
    PublicKey publicKey = get(username).getPublicKey(description).publicKey();
    return createUserCertificate(username, publicKey);
  }
  
  public boolean areAuthenticatedCertificates(X509Certificate[] certificates) throws KeyStoreException,
      NoSuchAlgorithmException, CertificateException, IOException, InvalidAlgorithmParameterException,
      CertPathValidatorException, SQLException, InvalidKeySpecException, CouldNotParse {
    
    X509Certificate cert = certificates[0];
    try {
      if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
        
        User identifiedUser = getUserFromCert(cert);
        return verifyUserSignedCertificate(identifiedUser, cert);
        
      } else {
        
        if (AuthenticationHandler.getInstance().isValid(0, certificates, certificates[0].getSubjectX500Principal())) {
          // if(userIsUnknown(cert))
          // storeNewUser(cert);
          return true;
        }
        
      }
    } catch (RuntimeException e) {
      return false;
    }
    return false;
    
  }
  
  private boolean verifyUserSignedCertificate(User identifiedUser, X509Certificate certificate) throws IOException,
      InvalidKeySpecException, NoSuchAlgorithmException, CouldNotParse {
    boolean verified = false;
    KeyManagement keydecoder = KeyManagement.getInstance();
    if (identifiedUser.getPublicKeys() == null || identifiedUser.getPublicKeys().size() == 0) {
      identifiedUser.addPublicKey(new UserPublicKey(certificate.getPublicKey(), "created at "
          + System.currentTimeMillis(), keydecoder.encodePublicKey(certificate.getPublicKey())));
      addKey(identifiedUser.getUsername(), identifiedUser.getPublicKeys().get(0));
    }
    for (UserPublicKey oldUserPublicKey : identifiedUser.getPublicKeys()) {
      PublicKey pubKey = oldUserPublicKey.publicKey();
      
      verified = AuthenticationHandler.getInstance().verifyCertificateWithPublicKey(certificate, pubKey);
      if (verified) {
        return true;
      }
    }
    throw new AuthenticationHandler.KeyDoesNotMatchException();
  }
  
  private String addDomain(String username) {
    InterfaceConfiguration configuration = null;
    if (!username.contains("@")) {
      configuration = InterfaceConfiguration.getInstance();
      username = username + "@" + configuration.getDomain();
    }
    return username;
  }
  
  @Override
  public List<User> getAllUsers() {
    EntityManager em = getEntityManager();
    Query query = em.createQuery("SELECT u FROM User u");
    @SuppressWarnings("unchecked")
    List<User> resultList = (List<User>) query.getResultList();
    return resultList;
  }
  
  @Override
  public Class addClass(String ownerUsername, Class targetClass) {
	EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(ownerUsername));
    if (user == null) {
      throw new UserNotFoundException();
    }
    if (user.classesOwned().contains(targetClass)) {
      throw new DuplicateClassException();
    }
    beginTransaction(em);
    user.addOwnedClass(targetClass);
    commitTransaction(em);
    return targetClass;
  }
  
  @Override
  public Class get(Class targetClass) {
    return get(targetClass.getId());
  }
  
  @Override
  public Class get(long id) {
    EntityManager em = getEntityManager();
    Class targetClass = em.find(Class.class, id);
    if (targetClass == null) {
      throw new CourseNotFoundException();
    }
    return targetClass;
  }
  
  @Override
  public void delete(Class targetClass) {
	EntityManager em = getEntityManager();
    User user = em.find(User.class, addDomain(targetClass.getOwner().getUsername()));
    if (user == null) {
      throw new UserNotFoundException();
    }
    beginTransaction(em);
    user.removeOwnedClass(targetClass);
    commitTransaction(em);
  }
  
  @Override
  public void delete(long id) {
    delete(get(id));
  }

  @Override
  public void addParticipant(long id, String username){
    User participant = get(username);
    EntityManager em = getEntityManager();
    Class targetCourse = em.find(Class.class, id);
    if(targetCourse == null){
      throw new CourseNotFoundException();
    }
    beginTransaction(em);
    targetCourse.addParticipant(participant);
    commitTransaction(em);
  }
  
  @Override
  public void removeParticipant(long id, String username){
    User participant = get(username);
    EntityManager em = getEntityManager();
    Class targetCourse = em.find(Class.class, id);
    if(targetCourse == null){
      throw new CourseNotFoundException();
    }
    beginTransaction(em);
    targetCourse.removeParticipant(participant);
    commitTransaction(em);
  }
  
  @Override
  public List<Class> getAllClassesFromUser(String username) {
    User u = get(username);
    return u.joinedClasses();
  }
  
  @Override
  public List<Class> getAllClassesOwnedByUser(String username) {
    User u = get(username);
    return u.classesOwned();
  }
  
  @Override
  public List<Class> getAllClasses() {
    EntityManager em = getEntityManager();
    Query query = em.createQuery("SELECT c FROM Class c");
    @SuppressWarnings("unchecked")
    List<Class> resultList = (List<Class>) query.getResultList();
    return resultList;
  }
  
  @Override
  public void deleteAllEntries(){
    EntityManager em = getEntityManager();
    beginTransaction(em);
    try{
      Query query = em.createQuery("DELETE FROM Class");
      query.executeUpdate();
      query = em.createQuery("DELETE FROM UserPublicKey");
      query.executeUpdate();
      query = em.createQuery("DELETE FROM User");
      query.executeUpdate();
      commitTransaction(em);
    } catch(Exception e){
      System.out.println(e.getMessage());
    }
  }
}
