# edu-sharing community - deploy docker repository

Prerequisites
-------------

- Docker Engine 18.06.0+
- Apache Maven 3.8.4+
- Java SE Development Kit 1.8
- Git SCM

Install
-------

1. Start up an instance from remote docker images by calling:

   ```
   ./deploy.sh rstart
   ```

2. Request all necessary information by calling:

   ```
   ./deploy.sh info
   ```

Build
-----

0. Please add following elements to `$HOME/.m2/settings.xml` und add your credentials:

   ```
     <mirrors>
       <mirror>
         <id>edusharing-mirror</id>
         <url>https://artifacts.edu-sharing.com/repository/maven-mirror/</url>
         <mirrorOf>!edusharing-remote,*</mirrorOf>
       </mirror>
     </mirrors>
     <servers>
       <server>
         <id>edusharing-remote</id>
         <username>...</username>
         <password>...</password>
       </server>
     </servers>
   ```      

   To switch on/off plugins, please set following environment variables:

   ```
   export PLUGIN_ELASTIC_ENABLED="true"
   export PLUGIN_MONGO_ENABLED="true"
   ```

1. Build local docker images by calling:

   ```
   mvn install
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

Debugging
---------

1. [Build](#build) local docker images first.

2. Start up an instance from local docker images with dev ports and artifacts from your local   
   [edu-sharing-community-repository](https://scm.edu-sharing.com/edu-sharing/community/repository/edu-sharing-community-repository) project by calling:

   ```
   ./deploy.sh ldebug -repo <path>
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

Uninstall
---------

1. Shut down an instance by calling:

   ```
   ./deploy.sh stop
   ```

2. Clean up all data volumes by calling:

   ```
   ./deploy.sh remove
   ```
   
---
If you need more information, please consult
our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
