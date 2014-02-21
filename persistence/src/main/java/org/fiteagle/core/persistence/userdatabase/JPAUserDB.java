package org.fiteagle.core.persistence.userdatabase;


import java.util.List;


//import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.fiteagle.api.User;
import org.fiteagle.api.User.Role;
import org.fiteagle.api.UserDB;
import org.fiteagle.api.UserPublicKey;
import org.hibernate.exception.ConstraintViolationException;


//@Stateless
public class JPAUserDB implements UserDB{
  
  private final String PERSISTENCE_TYPE;  
  private EntityManagerFactory factory;
  
//  private static final String DEFAULT_DATABASE_PATH = System.getProperty("user.home")+"/.fiteagle/db/";
//  private static FiteaglePreferences preferences = new FiteaglePreferencesXML(JPAUserDB.class);
  
//  private static final String PERSISTENCE_UNIT_NAME_DERBY = "Users_Derby";
  private static final String PERSISTENCE_UNIT_NAME_INMEMORY = "users_inmemory";
  
//  private static JPAUserDB derbyInstance;
  private static UserDB inMemoryInstance;
  
  private JPAUserDB(String persistenceUnitName) {
    PERSISTENCE_TYPE = persistenceUnitName;
  }
	
  @PersistenceContext(unitName="usersDB")
  EntityManager entityManager;
  
  public static UserDB getInMemoryInstance(){
    if(inMemoryInstance == null){
      inMemoryInstance = new JPAUserDB(PERSISTENCE_UNIT_NAME_INMEMORY);
    }
    return inMemoryInstance;
  }
//  
//  public static JPAUserDB getDerbyInstance(){
//    if(derbyInstance == null){
//      derbyInstance = new JPAUserDB(PERSISTENCE_UNIT_NAME_DERBY);
//    }
//    return derbyInstance;
//  }
  
//  private static String getDatabasePath() {
//    if(preferences.get("databasePath") == null){
//      preferences.put("databasePath", DEFAULT_DATABASE_PATH);
//    }
//    return preferences.get("databasePath");
//  }
  
  
  static{
    try {
      Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private synchronized EntityManager getEntityManager() {
    if (entityManager == null){
      factory = Persistence.createEntityManagerFactory(PERSISTENCE_TYPE);
      entityManager = factory.createEntityManager();
    }
    return entityManager;
  }
  
  @Override
  public void add(User user){
    EntityManager em = getEntityManager();
    if(em.contains(user)){
      throw new DuplicateUsernameException();
    }
    try{
      em.getTransaction().begin();
      em.persist(user);
      em.getTransaction().commit();
    } catch(Exception e){
      if(e.getCause() != null && e.getCause().getCause() instanceof ConstraintViolationException){
        ConstraintViolationException ec = (ConstraintViolationException) e.getCause().getCause();
        if(ec.getConstraintName().contains("EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES")){
          em.clear();
          throw new DuplicateEmailException();
        }
      }
      throw e;
    }finally{
//      em.close();
    }
  }
  
  @Override
  public User get(User user) throws UserNotFoundException{
    return get(user.getUsername());
  }
  
  @Override
  public FiteagleUser get(String username) throws UserNotFoundException{
    EntityManager em = getEntityManager();
    try{
      FiteagleUser user = em.find(FiteagleUser.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      return user;
    }finally{
//      em.close();
    }
  }
  
  @Override
  public void delete(User user){
    EntityManager em = getEntityManager();
    try{
      em.getTransaction().begin();
      em.remove(em.merge(user));
      em.getTransaction().commit();
    }finally{
//      em.close();
    }
  }
  
  @Override
  public void delete(String username){
    delete(get(username));
  }
 
  @Override
  public void update(String username, String firstName, String lastName, String email, String affiliation, String password, List<UserPublicKey> publicKeys) {
    EntityManager em = getEntityManager();
    try{
      User user = em.find(FiteagleUser.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      em.getTransaction().begin();
      user.updateAttributes(firstName, lastName, email, affiliation, password, publicKeys);
      em.getTransaction().commit();
    }catch(Exception e){
      if(e.getCause() != null && e.getCause().getCause() instanceof ConstraintViolationException){
        ConstraintViolationException ec = (ConstraintViolationException) e.getCause().getCause();
        if(ec.getConstraintName().contains("EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES")){
          em.clear();
          throw new DuplicateEmailException();
        }
      }
      throw e;
    }finally{
//      em.close();
    }
  }

  @Override
  public void setRole(String username, Role role) {
    EntityManager em = getEntityManager();
    try{
      User user = em.find(FiteagleUser.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      em.getTransaction().begin();
      user.setRole(role);
      em.getTransaction().commit();
    }finally{
//      em.close();
    }
  }

  
  @Override
  public void addKey(String username, UserPublicKey publicKey){
    EntityManager em = getEntityManager();
    try{
      User user = em.find(FiteagleUser.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      if(user.getPublicKeys().contains(publicKey)){
        throw new DuplicatePublicKeyException();
      }
      em.getTransaction().begin();
      user.addPublicKey(publicKey);
      em.getTransaction().commit();
    }finally{
//      em.close();
    }
  }
  
  @Override
  public void deleteKey(String username, String description){
    EntityManager em = getEntityManager();
    try{
      User user = em.find(FiteagleUser.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      em.getTransaction().begin();
      user.deletePublicKey(description);
      em.getTransaction().commit();
    }finally{
//      em.close();
    }
  }
  
  @Override
  public void renameKey(String username, String description, String newDescription){
    EntityManager em = getEntityManager();
    try{
      User user = em.find(FiteagleUser.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      if(user.hasKeyWithDescription(newDescription)){
        throw new DuplicatePublicKeyException();
      }
//      em.getTransaction().begin();
      user.renamePublicKey(description, newDescription);
//      em.getTransaction().commit();
    }finally{
//      em.close();
    }
  }
  
  @Override
  public List<User> getAllUsers(){
    EntityManager em = getEntityManager();
    try{
      Query query = em.createQuery("SELECT u FROM User u");
      @SuppressWarnings("unchecked")
      List<User> resultList = (List<User>) query.getResultList();
      return resultList;
    }finally{
//      em.close();
    }
  }
  
  public static class UserNotFoundException extends RuntimeException {    
    private static final long serialVersionUID = 2315125279537534064L;
    
    public UserNotFoundException(){
      super("no user with this username could be found in the database");
    }
  }
  
  public static class DuplicateUsernameException extends RuntimeException {
    private static final long serialVersionUID = -7242105025265481986L;   
    
    public DuplicateUsernameException(){
      super("another user with the same username already exists in the database");
    }
  }
  
  public static class DuplicateEmailException extends RuntimeException {
    private static final long serialVersionUID = 5986984055945876422L;
    
    public DuplicateEmailException(){
      super("another user with the same email already exists in the database");
    }
  }
  
  public static class DuplicatePublicKeyException extends RuntimeException {
    private static final long serialVersionUID = -8863826365649086008L; 
    
    public DuplicatePublicKeyException(){
      super("either this public key already exists or another public key with the same description already exists for this user");
    }
  }
  
}
