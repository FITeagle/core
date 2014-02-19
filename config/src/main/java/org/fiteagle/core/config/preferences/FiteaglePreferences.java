package org.fiteagle.core.config.preferences;

public abstract class FiteaglePreferences {
    
  public abstract void put(String key, String value);
  
  public abstract String get(String key); 
 
  
}
