package org.fiteagle.core.persistence.userdatabase;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
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

import net.iharder.Base64;

import org.codehaus.jackson.annotate.JsonIgnore;


@Entity
@Table(name="PUBLICKEYS", uniqueConstraints=@UniqueConstraint(columnNames={"owner_username", "description"}))
public class UserPublicKey implements Serializable{
  
  private static final long serialVersionUID = -374246341434116808L;
  
  private final static Pattern KEY_DESCRIPTION_PATTERN = Pattern.compile("[\\w|\\s]+");
  
  @JsonIgnore
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
  
  @JsonIgnore
  private byte[] bytes;
  @JsonIgnore
  private int pos;
  
  protected UserPublicKey() {
  } 
  
  public UserPublicKey(String publicKeyString, String description) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException{
    checkPublicKeyString(publicKeyString);
    this.publicKeyString = publicKeyString;
    
    this.publicKey = decodePublicKey(publicKeyString);    
    
    checkDescription(description); 
    this.description = description;      
  }
  
  public UserPublicKey(PublicKey publicKey, String description) throws User.NotEnoughAttributesException, IOException {
    this.publicKey = publicKey;
    
    this.publicKeyString = encodePublicKey(publicKey);
    checkPublicKeyString(publicKeyString);
    
    checkDescription(description); 
    this.description = description;
  } 
  
  private void checkDescription(String description) throws User.NotEnoughAttributesException {
    if(description == null || description.length() == 0){
      throw new User.NotEnoughAttributesException("no description for public key given");
    }
    if(!KEY_DESCRIPTION_PATTERN.matcher(description).matches()){
      throw new User.InValidAttributeException("empty or invalid key description, only letters, numbers and whitespace is allowed: "+description);
    }
  }   
  
  private void checkPublicKeyString(String publicKeyString) throws User.NotEnoughAttributesException {
    if(publicKeyString == null || publicKeyString.length() == 0){
      throw new User.NotEnoughAttributesException("no publicKeyString given");
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
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    UserPublicKey other = (UserPublicKey) obj;
    if (publicKey == null) {
      if (other.publicKey != null)
        return false;
    } else if (!publicKey.equals(other.publicKey))
      return false;
    return true;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    checkDescription(description);
    this.description = description;
  }

  public String getPublicKeyString() {
    return publicKeyString;
  }

  public Date getCreated() {
    return created;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }
  
  public PublicKey decodePublicKey(String keyLine) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
	    bytes = null;
	    pos = 0;
	    // look for the Base64 encoded part of the line to decode
	    // both ssh-rsa and ssh-dss begin with "AAAA" due to the length bytes
	    for (String part : keyLine.split(" ")) {
	      if (part.startsWith("AAAA")) {
	        bytes = Base64.decode(part);
	        break;
	      }
	    }
	    if (bytes == null) {
	       throw new CouldNotParseException("Could not find base64 part");
	    }
	    
	    String type = decodeType();
	    if (type.equals("ssh-rsa")) {
	      BigInteger e = decodeBigInt();
	      BigInteger m = decodeBigInt();
	      RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
	      return KeyFactory.getInstance("RSA").generatePublic(spec);
	    } else if (type.equals("ssh-dss")) {
	      BigInteger p = decodeBigInt();
	      BigInteger q = decodeBigInt();
	      BigInteger g = decodeBigInt();
	      BigInteger y = decodeBigInt();
	      DSAPublicKeySpec spec = new DSAPublicKeySpec(y, p, q, g);
	      return KeyFactory.getInstance("DSA").generatePublic(spec);
	    } else {
	      throw new CouldNotParseException("unknown type " + type);
	    }
	  }
	  
	  public String encodePublicKey(PublicKey pubKey) throws IOException {
	    String publicKeyEncoded;
	    if (pubKey.getAlgorithm().equals("RSA")) {
	      RSAPublicKey rsaPublicKey = (RSAPublicKey) pubKey;
	      ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
	      DataOutputStream dos = new DataOutputStream(byteOs);
	      dos.writeInt("ssh-rsa".getBytes().length);
	      dos.write("ssh-rsa".getBytes());
	      dos.writeInt(rsaPublicKey.getPublicExponent().toByteArray().length);
	      dos.write(rsaPublicKey.getPublicExponent().toByteArray());
	      dos.writeInt(rsaPublicKey.getModulus().toByteArray().length);
	      dos.write(rsaPublicKey.getModulus().toByteArray());
	      publicKeyEncoded = new String(Base64.encodeBytes(byteOs.toByteArray()));
	      return "ssh-rsa " + publicKeyEncoded;
	    } else if (pubKey.getAlgorithm().equals("DSA")) {
	      DSAPublicKey dsaPublicKey = (DSAPublicKey) pubKey;
	      DSAParams dsaParams = dsaPublicKey.getParams();
	      
	      ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
	      DataOutputStream dos = new DataOutputStream(byteOs);
	      dos.writeInt("ssh-dss".getBytes().length);
	      dos.write("ssh-dss".getBytes());
	      dos.writeInt(dsaParams.getP().toByteArray().length);
	      dos.write(dsaParams.getP().toByteArray());
	      dos.writeInt(dsaParams.getQ().toByteArray().length);
	      dos.write(dsaParams.getQ().toByteArray());
	      dos.writeInt(dsaParams.getG().toByteArray().length);
	      dos.write(dsaParams.getG().toByteArray());
	      dos.writeInt(dsaPublicKey.getY().toByteArray().length);
	      dos.write(dsaPublicKey.getY().toByteArray());
	      publicKeyEncoded = new String(Base64.encodeBytes(byteOs.toByteArray()));
	      return "ssh-dss " + publicKeyEncoded;
	    } else {
	      throw new CouldNotParseException("Unknown public key encoding: " + pubKey.getAlgorithm());
	    }
	  }
	  
	  private BigInteger decodeBigInt() {
		    int len = decodeInt();
		    byte[] bigIntBytes = new byte[len];
		    System.arraycopy(bytes, pos, bigIntBytes, 0, len);
		    pos += len;
		    return new BigInteger(bigIntBytes);
		  }
	  
	  private int decodeInt() {
		    return ((bytes[pos++] & 0xFF) << 24) | ((bytes[pos++] & 0xFF) << 16) | ((bytes[pos++] & 0xFF) << 8)
		        | (bytes[pos++] & 0xFF);
		  }
	  
	  private String decodeType() {
		    int len = decodeInt();
		    String type = new String(bytes, pos, len);
		    pos += len;
		    return type;
		  }
	  
	  public class CouldNotParseException extends RuntimeException {
		
		private static final long serialVersionUID = 3047910680052663625L;

		public CouldNotParseException(String message){
		      super(message);
		    }
	  }
}
