
# edu-sharing community - deploy docker rendering

Prerequisites
-------------

- Docker Engine 19.03.0+
- Docker Compose 1.27.4+ 
- Apache Maven 3.6.3+
- Git SCM

Startup
-------

1. Start up an instance from remote docker images by calling:

   ```
   ./deploy.sh rstart
   ```

2. Stream out the log messages by calling:

   ```
   ./deploy.sh logs
   ```

3. Shut down the instance by calling:

   ```
   ./deploy.sh remove
   ```
  
4. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```
    
Build
-----

1. Build local docker images by calling:

   ```
   ./deploy.sh build
   ```

Test
----

1. [Build](#build) local docker images first.
      
2. Start up an instance from local docker images with dev ports by calling: 

   ```
   ./deploy.sh ltest
   ```

3. Request all necessary information by calling:

   ```
   ./deploy.sh info
   ```

   and print out all running containers by calling:

   ```
   ./deploy.sh ps
   ```

   and stream out the log messages by calling:
    
   ```
   ./deploy.sh logs
   ```
   
4. Shut down the instance by calling:

   ```
   ./deploy.sh remove
   ```
  
5. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```
   
Debugging
---------

1. [Build](#build) local docker images first.

2. Start up an instance from local docker images with dev ports and artifacts from your local   
   [edu-sharing-community-services-rendering](https://scm.edu-sharing.com/edu-sharing/community/services/edu-sharing-rendering-service) project by calling:

   ```
   ./deploy.sh ldebug <path>
   ```

3. Request all necessary information by calling:

   ```
   ./deploy.sh info
   ```

   and print out all running containers by calling:

   ```
   ./deploy.sh ps
   ```

   and stream out the log messages by calling:

   ```
   ./deploy.sh logs
   ```
   
4. Shut down the instance by calling:

   ```
   ./deploy.sh remove
   ```
  
5. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```
---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
