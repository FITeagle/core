package org.fiteagle.core.usermanagement.userdatabase;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class InMemoryUserManager extends JPAUserManager {
  
  private static final String PERSISTENCE_UNIT_NAME_INMEMORY = "users_inmemory";  
  
  public InMemoryUserManager() {
  }
  
  @Override
  protected synchronized EntityManager getEntityManager() {
    if(entityManager == null) {
      try {
        java.lang.Class.forName("org.h2.Driver");
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME_INMEMORY);
      entityManager = factory.createEntityManager();
    }
    return entityManager;
  }
  
  @Override
  protected void beginTransaction(EntityManager em) {
    em.getTransaction().begin();
  }
  
  @Override
  protected void commitTransaction(EntityManager em) {
    em.getTransaction().commit();
  }
  
  @Override
  protected void flushTransaction(EntityManager em){
  }
  
}
