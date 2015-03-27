[![Build Status](https://travis-ci.org/FITeagle/core.svg?branch=master)](https://travis-ci.org/FITeagle/core)
[![Coverage Status](https://coveralls.io/repos/FITeagle/core/badge.svg)](https://coveralls.io/r/FITeagle/core)

FITeagle Core Modules
=====================

Core System

Requirements
---

  The 'api' module must be available


FAQ
---
* Q: FITeagle tests seem to hang while testing cryptography methods on Linux
* A: The current version uses /dev/urandom as random source (```-Djava.security.egd=file:/dev/./urandom```)
* A: ~~Setup rng-tools:~~
  * ~~then add the line ```HRNGDEVICE=/dev/urandom``` to ```/etc/default/rng-tools```.~~
  * ~~afterwards start the rng-tools daemon: ```sudo /etc/init.d/rng-tools start```.~~
