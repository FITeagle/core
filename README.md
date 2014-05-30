core
====

Core System


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
