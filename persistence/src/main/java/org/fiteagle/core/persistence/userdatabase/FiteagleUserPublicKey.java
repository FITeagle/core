package org.fiteagle.core.persistence.userdatabase;

import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.fiteagle.api.User;
import org.fiteagle.api.UserPublicKey;
import org.fiteagle.core.aaa.authentication.KeyManagement;


@Entity
@Table(name="PUBLICKEYS", uniqueConstraints=@UniqueConstraint(columnNames={"owner_username", "description"}))
public class FiteagleUserPublicKey implements Serializable, UserPublicKey{
  
  private static final long serialVersionUID = -374246341434116808L;
  
  private final static Pattern KEY_DESCRIPTION_PATTERN = Pattern.compile("[\\w|\\s]+");
  
  @JsonIgnore
  @Column(length=1024)
  private PublicKey publicKey;  
  
  private String description;
  
  @JsonIgnore
  @Id
  @JoinColumn(name="owner_username")
  @ManyToOne
  private User owner;
  
  @Id
  @Column(length=1024)
  private String publicKeyString; 
  
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;    
  
   
  protected FiteagleUserPublicKey() {
  } 
  
  public FiteagleUserPublicKey(String publicKeyString, String description) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException{
    checkPublicKeyString(publicKeyString);
    this.publicKeyString = publicKeyString;
    
    this.publicKey = KeyManagement.getInstance().decodePublicKey(publicKeyString);    
    
    checkDescription(description); 
    this.description = description;      
  }
  
  public FiteagleUserPublicKey(PublicKey publicKey, String description) throws FiteagleUser.NotEnoughAttributesException, IOException {
    this.publicKey = publicKey;
    
    this.publicKeyString = KeyManagement.getInstance().encodePublicKey(publicKey);
    checkPublicKeyString(publicKeyString);
    
    checkDescription(description); 
    this.description = description;
  } 
  
  private void checkDescription(String description) throws FiteagleUser.NotEnoughAttributesException {
    if(description == null || description.length() == 0){
      throw new FiteagleUser.NotEnoughAttributesException("no description for public key given");
    }
    if(!KEY_DESCRIPTION_PATTERN.matcher(description).matches()){
      throw new FiteagleUser.InValidAttributeException("empty or invalid key description, only letters, numbers and whitespace is allowed: "+description);
    }
  }   
  
  private void checkPublicKeyString(String publicKeyString) throws FiteagleUser.NotEnoughAttributesException {
    if(publicKeyString == null || publicKeyString.length() == 0){
      throw new FiteagleUser.NotEnoughAttributesException("no publicKeyString given");
    }
  }   
  
  @PrePersist
  public void updateTimeStamps() {
    if(created == null) {
      created = new Date();
    }
  }
  
  @Override
  public String toString() {
    return "PublicKey [publicKey=" + publicKey + ", description=" + description + ", created=" + created + "]";
  }
  
  @Override
  public PublicKey getPublicKey() {
    return publicKey;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    checkDescription(description);
    this.description = description;
  }

  @Override
  public String getPublicKeyString() {
    return publicKeyString;
  }

  @Override
  public Date getCreated() {
    return created;
  }

  @Override
  public User getOwner() {
    return owner;
  }

  @Override
  public void setOwner(User owner) {
    this.owner = owner;
  }

  @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((publicKeyString == null) ? 0 : publicKeyString.hashCode());
		return result;
	}

  @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FiteagleUserPublicKey other = (FiteagleUserPublicKey) obj;
		if (publicKey == null) {
			if (other.publicKey != null)
				return false;
		} else if (!publicKey.equals(other.publicKey))
			return false;
		return true;
	}

}
