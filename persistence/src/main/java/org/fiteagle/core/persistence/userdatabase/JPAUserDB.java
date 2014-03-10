package org.fiteagle.core.persistence.userdatabase;


import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.fiteagle.api.User;
import org.fiteagle.api.User.Role;
import org.fiteagle.api.UserDB;
import org.fiteagle.api.UserPublicKey;

@Stateless
@Remote(UserDB.class)
public class JPAUserDB implements UserDB{
  
  private static final String PERSISTENCE_UNIT_NAME_INMEMORY = "users_inmemory";
  
  private static UserDB inMemoryInstance;
  
  public JPAUserDB(){
  }
	
  @PersistenceContext(unitName="usersDB")
  EntityManager entityManager;
  
  public static UserDB getInMemoryInstance(){
    if(inMemoryInstance == null){
      inMemoryInstance = new JPAUserDB();
    }
    return inMemoryInstance;
  }
  
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
      EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME_INMEMORY);
      entityManager = factory.createEntityManager();
    }
    return entityManager;
  }
  
  private void beginTransaction(EntityManager em){
    if(this == inMemoryInstance){
      em.getTransaction().begin();
    }
  }
  
  private void commitTransaction(EntityManager em){
    if(this == inMemoryInstance){
      em.getTransaction().commit();
    }
  }
  
  @Override
  public void add(User user){
    EntityManager em = getEntityManager();
    
    List<User> users = getAllUsers();
    for(User u : users){
      if(u.getEmail().equals(user.getEmail())){
        throw new DuplicateEmailException();
      }
      if(u.getUsername().equals(user.getUsername())){
        throw new DuplicateUsernameException();
      }
    }
    
//    try{
    beginTransaction(em);
    em.persist(user);
    commitTransaction(em);
//    } catch(Exception e){
//      if(e.getCause() != null && e.getCause().getCause() instanceof ConstraintViolationException){
//        ConstraintViolationException ec = (ConstraintViolationException) e.getCause().getCause();
//        if(ec.getConstraintName().contains("EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES")){
//          em.clear();
//          throw new DuplicateEmailException();
//        }
//      }
//      throw e;
//    }
  }
  
  @Override
  public User get(User user) throws UserNotFoundException{
    return get(user.getUsername());
  }
  
  @Override
  public User get(String username) throws UserNotFoundException{
    EntityManager em = getEntityManager();
    User user = em.find(User.class, username);
    if(user == null){
      throw new UserNotFoundException();
    }
    return user;
  }
  
  @Override
  public void delete(User user){
    EntityManager em = getEntityManager();
    beginTransaction(em);
    em.remove(em.merge(user));
    commitTransaction(em);
  }
  
  @Override
  public void delete(String username){
    delete(get(username));
  }
 
  @Override
  public void update(String username, String firstName, String lastName, String email, String affiliation, String password, List<UserPublicKey> publicKeys) {
    EntityManager em = getEntityManager();
//    try{
      User user = em.find(User.class, username);
      if(user == null){
        throw new UserNotFoundException();
      }
      
      List<User> users = getAllUsers();
      for(User u : users){
        if(u.getEmail().equals(email) && !u.getUsername().equals(username)){
          throw new DuplicateEmailException();
        }
      }
      
      beginTransaction(em);
      user.updateAttributes(firstName, lastName, email, affiliation, password, publicKeys);
      commitTransaction(em);
//    }catch(Exception e){
//      if(e.getCause() != null && e.getCause().getCause() instanceof ConstraintViolationException){
//        ConstraintViolationException ec = (ConstraintViolationException) e.getCause().getCause();
//        if(ec.getConstraintName().contains("EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES")){
//          em.clear();
//          throw new DuplicateEmailException();
//        }
//      }
//      throw e;
//    }
  }

  @Override
  public void setRole(String username, Role role) {
    EntityManager em = getEntityManager();
    User user = em.find(User.class, username);
    if(user == null){
      throw new UserNotFoundException();
    }
    beginTransaction(em);
    user.setRole(role);
    commitTransaction(em);
  }

  
  @Override
  public void addKey(String username, UserPublicKey publicKey){
    EntityManager em = getEntityManager();
    User user = em.find(User.class, username);
    if(user == null){
      throw new UserNotFoundException();
    }
    if(user.getPublicKeys().contains(publicKey)){
      throw new DuplicatePublicKeyException();
    }
    beginTransaction(em);
    user.addPublicKey(publicKey);
    commitTransaction(em);
  }
  
  @Override
  public void deleteKey(String username, String description){
    EntityManager em = getEntityManager();
    User user = em.find(User.class, username);
    if(user == null){
      throw new UserNotFoundException();
    }
    beginTransaction(em);
    user.deletePublicKey(description);
    commitTransaction(em);
  }
  
  @Override
  public void renameKey(String username, String description, String newDescription){
    EntityManager em = getEntityManager();
    User user = em.find(User.class, username);
    if(user == null){
      throw new UserNotFoundException();
    }
    if(user.hasKeyWithDescription(newDescription)){
      throw new DuplicatePublicKeyException();
    }
    beginTransaction(em);
    try{
      user.renamePublicKey(description, newDescription);
    } finally {
      commitTransaction(em);
    }    
  }
  
  @Override
  public List<User> getAllUsers(){
    EntityManager em = getEntityManager();
    Query query = em.createQuery("SELECT u FROM User u");
    @SuppressWarnings("unchecked")
    List<User> resultList = (List<User>) query.getResultList();
    return resultList;
  }
}
