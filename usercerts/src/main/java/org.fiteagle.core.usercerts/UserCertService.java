package org.fiteagle.core.usercerts;

import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.fiteagle.api.core.Config;
import org.fiteagle.api.core.usermanagement.UserPublicKey;


import javax.security.auth.x500.X500Principal;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import net.iharder.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Created by dne on 21.11.15.
 */
@Path("/")
public class UserCertService {
    private final String CA_PRK_PASS = System.getProperty("ca_prk_pass");
    private final String CA_ALIAS =System.getProperty("ca_alias");
    private final String RESOURCE_STORE_PASS = System.getProperty("resourceStore_pass");
    private final String TRUSTSTORE_PASSWORD =  System.getProperty("truststore_pass");
    private static final String KEYSTORE_LOCATION ="" ;
    private final String KEYSTORE_PASSWORD = System.getProperty("keystore_pass");
    private final String TRUSTSTORE_LOCATION= System.getProperty("jboss.server.config.dir") + System.getProperty("file.separator") + System.getProperty("truststore_name");

    private enum StoreType{
        KEYSTORE,TRUSTSTORE,RESOURCESTORE;
    }
    private static String prefix = "-----BEGIN CERTIFICATE-----\n";
    private static String suffix = "\n-----END CERTIFICATE-----\n";
    Config config = new Config("fiteagle");

    Logger log = LoggerFactory.getLogger(this.getClass());

    @GET
    @Produces("text/plain")
    public String getUserCert(@QueryParam("id")String username, @QueryParam("pw")String password, @QueryParam("valid")String daysValid){
        int valid = 0;
        if(daysValid == null) {
            valid = 1;
        }
        else {
            valid = getIntValid(daysValid);
        }
        long validSeconds= getSeconds(valid);

        try {
            return createUserCertificate(username, password, generateKeyPair() , validSeconds);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private long getSeconds(int valid) {

        return 3600 * 24 * valid;
    }

    private int getIntValid(String daysValid) {
        int ret = 0;
        try {
            ret = Integer.parseInt(daysValid);
        }catch (Exception e){
            ret = 1;
        }
        return ret;
    }

    private String createUserCertificate(String username, String password, KeyPair keyPair, long validSeconds) throws Exception {
        //String pubKeyEncoded = encodePublicKey(keyPair.getPublic());
        String userCertString = createUserCertificate(username,	keyPair.getPublic(), validSeconds);
        String privateKeyEncoded = encryptPrivateKey(keyPair.getPrivate(), password);
        return privateKeyEncoded + "\n" + userCertString;
    }

    private String encryptPrivateKey(PrivateKey aPrivate, String password) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PEMWriter writer = new PEMWriter(new BufferedWriter(new OutputStreamWriter(out)));
        if(password.length() > 0){
            JcePEMEncryptorBuilder builder = new JcePEMEncryptorBuilder("DES-EDE3-CBC");
            PEMEncryptor encryptor = builder.build(password.toCharArray());
            writer.writeObject(aPrivate, encryptor);
        }
        else{
            writer.writeObject(aPrivate);
        }
        writer.flush();
        writer.close();
        String returnString = out.toString();
        out.close();
        return returnString;
    }

    private String createUserCertificate(String username, PublicKey aPublic, long validSeconds) throws Exception {

        X509Certificate cert = createX509Certificate(username, aPublic, validSeconds);
        return getCertficateEncoded(cert);
    }

    private String getCertficateEncoded(X509Certificate cert) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        bout.write(Base64.encodeBytesToBytes(cert.getEncoded(), 0, cert.getEncoded().length, Base64.NO_OPTIONS));

        String encodedCert = new String(bout.toByteArray());
        bout.close();
        int i = 0;
        String returnCert="";
        while(i< encodedCert.length()){
            int max = i +64;
            if(max < encodedCert.length()){
                returnCert =returnCert +  encodedCert.subSequence(i, max)+"\n";
            }else{
                returnCert = returnCert + encodedCert.subSequence(i, encodedCert.length());
            }
            i+=64;
        }
        returnCert = prefix + returnCert + suffix;
        return returnCert;
    }


    private X509Certificate createX509Certificate(String username, PublicKey aPublic, long validSeconds) throws CertificateException, IOException, OperatorCreationException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        X509Certificate caCert = getCACert();
        X500Name issuer = new JcaX509CertificateHolder(caCert).getSubject();
        PrivateKey caPrivateKey = getCAPrivateKey();
        ContentSigner contentsigner = new JcaContentSignerBuilder(
                "SHA1WithRSAEncryption").build(caPrivateKey);

        X500Name subject = createX500Name(username);
        SubjectPublicKeyInfo subjectsPublicKeyInfo = getSubjectPublicKey(aPublic);
        X509v3CertificateBuilder ca_gen = new X509v3CertificateBuilder(issuer,
                new BigInteger(new SecureRandom().generateSeed(256)),
                new Date(),
                new Date(System.currentTimeMillis() + validSeconds), subject,
                subjectsPublicKeyInfo);
        BasicConstraints ca_constraint = new BasicConstraints(false);
        ca_gen.addExtension(X509Extension.basicConstraints, true, ca_constraint);
        GeneralNames subjectAltName = new GeneralNames(new GeneralName(
                GeneralName.uniformResourceIdentifier, getURN(username)));

        X509Extension extension = new X509Extension(false, new DEROctetString(
                subjectAltName));
        ca_gen.addExtension(X509Extension.subjectAlternativeName, false,
                extension.getParsedValue());
        X509CertificateHolder holder = (X509CertificateHolder) ca_gen
                .build(contentsigner);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf
                .generateCertificate(new ByteArrayInputStream(holder
                        .getEncoded()));
    }

    private SubjectPublicKeyInfo getSubjectPublicKey(PublicKey aPublic) throws IOException {
        SubjectPublicKeyInfo subPubInfo = new SubjectPublicKeyInfo(
                (ASN1Sequence) ASN1Sequence.fromByteArray(aPublic.getEncoded()));
        return subPubInfo;
    }

    private X500Name createX500Name(String username) {
        X500Principal prince = new X500Principal("CN=" + username);
        X500Name x500Name = new X500Name(prince.getName());
        return x500Name;
    }

    private String getURN(String username) {
        return "urn:publicid:IDN+"+config.getProperty("hostname")+"+user+"+username;
    }

    private PrivateKey getCAPrivateKey() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableEntryException {
        PrivateKey privateKey = null;
        KeyStore  ks = loadKeyStore(StoreType.TRUSTSTORE);
        KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(getCAPrivateKeyPassword());
        KeyStore.Entry keyStoreEntry = ks.getEntry(getCAAlias(), protection);
        if(keyStoreEntry instanceof KeyStore.PrivateKeyEntry){
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStoreEntry;
            privateKey = privateKeyEntry.getPrivateKey();

        }else {
            throw new RuntimeException();
        }
        return privateKey;
    }

    private char[] getCAPrivateKeyPassword() {
        return CA_PRK_PASS.toCharArray();
    }

    private X509Certificate getCACert() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = loadKeyStore(StoreType.TRUSTSTORE);
        X509Certificate caCert = (X509Certificate) ks.getCertificate(getCAAlias());
        return caCert;
    }

    private String getCAAlias() {
        return CA_ALIAS;
    }

    private KeyStore loadKeyStore(StoreType type) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(getStorePath(type));
        char[] pass = getStorePass(type);
        ks.load(fis, pass);
        return ks;
    }

    private char[] getStorePass(StoreType type) {
            switch(type) {
                case KEYSTORE:
                    return getKeyStorePassword();

                case TRUSTSTORE:
                    return getTrustStorePassword();

                case RESOURCESTORE:
                    return getResourceStorePass();

                default:
                    return getTrustStorePassword();
            }

    }

    private char[] getResourceStorePass() {
        return RESOURCE_STORE_PASS.toCharArray();
    }

    private char[] getTrustStorePassword() {
        return TRUSTSTORE_PASSWORD.toCharArray();
    }

    private char[] getKeyStorePassword() {
        return KEYSTORE_PASSWORD.toCharArray();
    }

    private String getStorePath(StoreType type) {
        switch(type) {
            case KEYSTORE:
                return getKeyStorePath();
            case TRUSTSTORE:
                return getTrustStorePath();

            case RESOURCESTORE:
                return getResourceStorePath();

            default:
                return getTrustStorePath();
        }
    }

    private String getResourceStorePath() {
        return null;
    }

    private String getTrustStorePath() {
        return TRUSTSTORE_LOCATION;
    }

    private String getKeyStorePath() {
        return KEYSTORE_LOCATION;
    }

    private String encodePublicKey(PublicKey pubKey) throws IOException {
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
            throw new RuntimeException("Unknown public key encoding: " + pubKey.getAlgorithm());
        }
    }



    private KeyPair generateKeyPair() {
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
