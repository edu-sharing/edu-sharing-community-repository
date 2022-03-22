# edu_sharing-projects-community - deploy docker

Prerequisites
-------------

- Docker Engine 18.06.0+

Install
-------

1. Please login with your credentials (if necessary):

   ```
   docker login docker.edu-sharing.com --username <...> --password <...> 
   ```

2. Start up an instance from remote docker images by calling:

   ```
   ./deploy.sh start
   ```

3. Request all necessary informations by calling:

   ```
   ./deploy.sh info
   ```

Uninstall
---------

1. Shut down an instance by calling:

   ```
   ./deploy.sh stop
   ```

2. Clean up all container by calling:

   ```
   ./deploy.sh remove
   ```

   or clean up all container and data volumes by calling:

   ```
   ./deploy.sh purge
   ```
---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
