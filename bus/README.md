Deployment of the FITeagle Core module: Message Bus
===================================================
- Author: Alexander Willner
- Level: Demo
- Technologies: EJB, WAR
- Target Project: FITeagle

What is it?
-----------

This project deploys the FITeagle Message Bus tools as a *EJB 3.1* bean bundled in a war archive for deployment to *JBoss WildFly*.

Build and Deploy
----------------

1. Make sure you have started the WildFly server.
2. Open a command line and navigate to the root directory of this project.
3. Type this command to build and deploy the archive:

        mvn clean package wildfly:deploy

4. This will deploy `target/bus.war` to the running instance of the server.

Test
----

1. Listen to the web socket at:

    ws-client ws://localhost:8080/bus/api/logger

2. Send to the web socket at:

    ws-client ws://localhost:8080/bus/api/commander

3. Send to the web socket at:

    curl http://localhost:8080/bus/api/commander
