FITeagle :: core :: UserCertificates
=============================

FITeagle is able to generate certificates which are usable in jFed and other tools.

You can get a certificate via REST using this terminal command:

$curl -H "key: secret_serverkey" "http://localhost:8080/usercerts?id=*your_username_here*&pw=*your_password_here*"

The Server-Admin should have set a Secret-Key so not everyone is able to create a Certifikate.
If he didn't set one you can delete the '-H "key: secret_serverkey"' part.


Please change the URL to the URL where FITeagle is running and also change the username and password fields in the URL.
For Example:

$curl -H "key: secretpassword" http://fiteagle.de:8080/usercerts?id=Fiteagle&pw=testpassword

You can also set a Duration for your Certificate if you want to, by adding "&valid=*duration" to the end of the URL.
For Example a Certificate with a duration of 100 days:
$curl -H "key: secretpassword" "http://fiteagle.de:8080/usercerts?id=Fiteagle&pw=testpassword&valid=100"


Now save the certificate you received in a *.PEM file and make it executable.
For example:
$sudo chmod 700 testCert.PEM
