Deployment of the FITeagle Core module: Resource Repository
===========================================================
- Author: Alexander Willner
- Level: Demo
- Technologies: EJB, JMS, MDB, REST, WAR, RDF
- Target Project: FITeagle

What is it?
-----------

This project deploys the FITeagle Resource Repository as a *EJB 3.1* bean bundled in a war archive for deployment to *JBoss WildFly*. The project also includes a set of Aquillian tests for the managed bean and EJB.

Build and Deploy
----------------

1. Make sure you have started the WildFly server.
2. Open a command line and navigate to the root directory of this project.
3. Type this command to build and deploy the archive:

        mvn clean package wildfly:deploy

4. This will deploy `target/core-resourcerepository.war` to the running instance of the server.

Test
----

1. Get resources in turtle serialization

    1. Via native calls: curl http://localhost:8080/repo/api/resources.ttl
    2. Via EJB calls: curl http://localhost:8080/repo/api/ejb/resources.ttl
    3. Via MDB calls: curl http://localhost:8080/repo/api/mdb/resources.ttl

2. Get resources in xml+rdf serialization

    1. Via native calls: curl http://localhost:8080/repo/api/resources.rdf
    2. Via EJB calls: curl http://localhost:8080/repo/api/ejb/resources.rdf
    3. Via MDB calls: curl http://localhost:8080/repo/api/mdb/resources.rdf
    
Tipps
-----

1. Use http://librdf.org/raptor/rapper.html to validate and convert between formats
2. Use https://github.com/AKSW/Xturtle to edit RDF files in Eclipe
3. Use http://protege.stanford.edu to work with RDF files

Undeploy
--------

1. Make sure you have started the WildFly server.
2. Open a command line and navigate to the root directory of this project.
3. When you are finished testing, type this command to undeploy the archive:

        mvn wildfly:undeploy


Run the Arquillian Tests 
-------------------------

This project provides Arquillian tests. By default, these tests are configured to be skipped as Arquillian tests require the use of a container. 

1. Make sure you have started the WildFly server.
2. Open a command line and navigate to the root directory of this project.
3. Type the following command to run the test goal with the following profile activated:

        mvn clean test -Parq-wildfly-managed

Investigate the Console Output
------------------------------

JUnit will present you test report summary:

    Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

If you are interested in more details, check ``target/surefire-reports`` directory. 
You can check console output to verify that Arquillian has really used the real application server. 
Search for lines similar to the following ones in the server output log:

    [timestamp] INFO  [org.jboss.as.server.deployment] (MSC service thread 1-7) JBAS015876: Starting deployment of "core-resourcerepository.war"
    ...
    [timestamp] INFO  [org.jboss.as.server] (management-handler-thread - 7) JBAS018559: Deployed "core-resourcerepository.war"
    ...
    [timestamp] INFO  [org.jboss.as.server.deployment] (MSC service thread 1-1) JBAS015877: Stopped deployment core-resourcerepository.war in 51ms
    ...
    [timestamp] INFO  [org.jboss.as.server] (management-handler-thread - 5) JBAS018558: Undeployed "core-resourcerepository.war"
