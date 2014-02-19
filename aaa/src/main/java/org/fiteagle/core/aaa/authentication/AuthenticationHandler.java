package org.fiteagle.core.aaa.authentication;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationHandler {
  
  private KeyStoreManagement keyStoreManagement;
  Logger log = LoggerFactory.getLogger(this.getClass());
  
  private static AuthenticationHandler authHandler = null;
  
  public static AuthenticationHandler getInstance() {
    if (authHandler == null)
      authHandler = new AuthenticationHandler();
    
    return authHandler;
  }
  
  private AuthenticationHandler() {
    
    this.keyStoreManagement = KeyStoreManagement.getInstance();
  }
  
  
  
//  private void storeNewUser(X509Certificate certificate) {
    // TODO Auto-generated method stub
    
//  }

//  private boolean userIsUnknown(X509Certificate certificate) throws CertificateParsingException {
//    UserDBManager  userDBManager = UserDBManager.getInstance();
//    String userName = X509Util.getUserNameFromX509Certificate(certificate);
//    String domain = X509Util.getDomain(certificate);
//    try{
//      User u = userDBManager.get(userName);
//      return true;
//    }catch(UserNotFoundException e){
//      return false;
//    }
//    
//  }

  public boolean isValid(int i, X509Certificate[] certificates, X500Principal x500Principal) throws KeyStoreException,
      NoSuchAlgorithmException, CertificateException, IOException {
    boolean valid = false;
    if (i < certificates.length && x500Principal.equals(certificates[i].getSubjectX500Principal())) {
      
      if (i == certificates.length - 1) {
        valid=  isTrustworthy(certificates[i]);
        
      } else {
        valid = isValid(i + 1, certificates, certificates[i].getIssuerX500Principal());
      }
      
    }if(valid){
      return valid;
    }else {
      throw new CertificateNotTrustedException();
    }
    
  }
  
  private boolean isTrustworthy(X509Certificate trustworthyCert) throws KeyStoreException, NoSuchAlgorithmException,
      CertificateException, IOException {
    List<X509Certificate> storedCerts = keyStoreManagement.getTrustedCerts();
   
    for (X509Certificate cert : storedCerts) {
      if (cert.getIssuerX500Principal().equals(trustworthyCert.getIssuerX500Principal())) {
        
        try {
          trustworthyCert.verify(cert.getPublicKey());
          return true;
        } catch (Exception e) {
          log.error(e.getMessage(),e);
          return false;
        }
      }
    }
    return false;
  }
  
  private PublicKey getTrustedIssuerPublicKey(X500Principal issuerX500Principal) throws KeyStoreException,
      NoSuchAlgorithmException, CertificateException, IOException {
    List<X509Certificate> storedCerts = keyStoreManagement.getTrustedCerts();
    for (X509Certificate cert : storedCerts) {
      if (!isUserCertificate(cert)) {
        if (cert.getSubjectX500Principal().equals(issuerX500Principal)) {
          return cert.getPublicKey();
        }
      }
    }
    throw new CertificateNotTrustedException();
  }
  
  public boolean verifyCertificateWithPublicKey(X509Certificate certificate, PublicKey pubKey) {
    try {
      certificate.verify(pubKey);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  
  private boolean isUserCertificate(X509Certificate x509Certificate) {
    
    if (x509Certificate.getBasicConstraints() == -1) {
      return true;
    } else {
      return false;
    }
  }
  
  public static class KeyDoesNotMatchException extends RuntimeException {
    
    private static final long serialVersionUID = -6825126254061827637L;
    
  }
  
  public class CertificateNotTrustedException extends RuntimeException {
    
    private static final long serialVersionUID = 6120670655966336971L;
    
  }
  
}
