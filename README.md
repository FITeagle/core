[![Build Status](https://travis-ci.org/FITeagle/core.svg?branch=master)](https://travis-ci.org/FITeagle/core)
[![Coverage Status](https://coveralls.io/repos/FITeagle/core/badge.svg)](https://coveralls.io/r/FITeagle/core)

FITeagle Core Modules
=====================

Core System

Requirements
---

  - The 'api' module must be available
  - Optimally there should be an 'Federation.ttl' File in the /home/$User/.fiteagle folder with your own Testbed-Ontology
  - If there is no file found while deploying, Fiteagle creates one empty file, which you have to edit and then Re-Deploy     the FederationManager with "mvn wildfly:deploy"
  

Examples
---
Example for an Federation.ttl file:

    @prefix wgs:  <http://www.w3.org/2003/01/geo/wgs84_pos#> .
    @prefix owl:  <http://www.w3.org/2002/07/owl#> .
    @prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
    @prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .
    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
    @prefix omn:  <http://open-multinet.info/ontology/omn#> .
    @prefix dummy:   <http://federation.your-domain.com/about#> .
    @prefix omn-federation: <http://open-multinet.info/ontology/omn-federation#> .
     
    dummy:Dummy_Communication_Testbed a omn-federation:Infrastructure,
                                        owl:NamedIndividual;
                                         rdfs:label "Dummy Communication Testbed" ;
                                         rdfs:seeAlso "https://federation.your-domain.com" ;
                                         wgs:lat       "58.836473" ;
                                         wgs:long      "10.018503" .


FAQ
---
* Q: FITeagle tests seem to hang while testing cryptography methods on Linux
* A: The current version uses /dev/urandom as random source (```-Djava.security.egd=file:/dev/./urandom```)
* A: ~~Setup rng-tools:~~
  * ~~then add the line ```HRNGDEVICE=/dev/urandom``` to ```/etc/default/rng-tools```.~~
  * ~~afterwards start the rng-tools daemon: ```sudo /etc/init.d/rng-tools start```.~~
