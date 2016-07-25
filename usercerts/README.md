FITeagle :: core :: UserCertificates
=============================

FITeagle is able to generate certificates which are usable in jFed and other tools.

You can get a certificate via REST using your Browser or this terminal command:

curl http://localhost:8080/usercerts?id=*your_username_here*&pw=*your_password_here*

Please change the URL to the URL where FITeagle is running and also change the username and password fields in the URL.
For Example:

curl http://fiteagle.de:8080/usercerts?id=Fiteagle&pw=testpassword