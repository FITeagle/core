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

3. Get resources in JSON-LD serialization    

    1. Via MDB calls: curl http://localhost:8080/repo/api/mdb/resources.jsonld
    
4. Query resources via SPARQL endpoint

    curl \
      -G -H 'Accept: text/turtle' \
      --data-urlencode "query=SELECT ?s ?o WHERE {?s <http://fiteagle.org/ontology#isInstantiatedBy> ?o} LIMIT 1" \
      http://localhost:8080/repo/api/sparql

Tipps
-----

1. Use http://librdf.org/raptor/rapper.html to validate and convert between formats
2. Use https://github.com/AKSW/Xturtle to edit RDF files in Eclipe
3. Use http://protege.stanford.edu to work with RDF files
4. Install Jena to work with RDF via CLI: brew install jena

Undeploy
--------

1. Make sure you have started the WildFly server.
2. Open a command line and navigate to the root directory of this project.
3. When you are finished testing, type this command to undeploy the archive:

        mvn wildfly:undeploy

