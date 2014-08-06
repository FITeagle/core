[![Build Status](https://travis-ci.org/FITeagle/core.svg?branch=master)](https://travis-ci.org/FITeagle/core)

FITeagle Core Modules
=====================

Core System

Requirements
---

  The 'api' module must be available


FAQ
---
* Q: FITeagle tests seem to hang while testing cryptography methods on Linux
* A: Setup rng-tools:

  ```
    sudo apt-get install rng-tools
    vi /etc/default/rng-tools
  ```
  * then add the line HRNGDEVICE=/dev/urandom
  * afterwards start the rng-tools daemon:

  ```
    sudo /etc/init.d/rng-tools start
  
```
