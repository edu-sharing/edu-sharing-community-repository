# edu-sharing community - deploy docker repository

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

0. If you have switched on additional plugins (see below), then you have to add your credentials for each plugin
   in `$HOME/.m2/settings.xml` too:

   ```
      <server>  
        <id>edu-sharing.plugin.remote.releases</id>
        <username> ... </username>
        <password> ... </password>
      </server>
      <server>
        <id>edu-sharing.plugin.remote.snapshots</id>
        <username> ... </username>
        <password> ... </password>
      </server>  
   ```      

   Then setting following environment variables:

   ```
   export PLUGIN_REMOTE_ENABLED="true"
   ```

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
   [edu-sharing-community-repository](https://scm.edu-sharing.com/edu-sharing/community/repository/edu-sharing-community-repository) project by calling:

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

4. If you have made changes then you can reload the local artifacts by calling:

    * for changes inside backend-services modules:

      ```
      ./deploy.sh reload
      ```

    * for changes inside frontend-modules:

      > You have to start the Angular dev server once at the beginning by calling
      (inside [edu-sharing-community-repository](https://scm.edu-sharing.com/edu-sharing/community/repository/edu-sharing-community-repository)):
      > ```
      > ./node/npm run start
      > ```     
      > and use the special URL shown (instead of the usual one).

5. Shut down the instance by calling:

   ```
   ./deploy.sh remove
   ```

6. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```

---
If you need more information, please consult
our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
