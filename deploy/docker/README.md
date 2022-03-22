# edu_sharing-projects-community - deploy docker

Prerequisites
-------------

- Docker Engine 18.06.0+
- Apache Maven 3.8.4+
- Java SE Development Kit 1.8 (<11)
- PHP Framework 7.4 (<8)
- Git SCM

Install
-------

1. Please login with your credentials (if necessary):

   ```
   docker login docker.edu-sharing.com --username <...> --password <...> 
   ```

2. Start up an instance from remote docker images by calling:

   ```
   ./deploy.sh rstart
   ```

3. Request all necessary information by calling:

   ```
   ./deploy.sh info
   ```

Build
-----

1. Please add following elements to `$HOME/.m2/settings.xml` 

   ```
     <mirrors>
       <mirror>
         <id>edusharing-mirror</id>
         <url>https://artifacts.edu-sharing.com/repository/maven-mirror/</url>
         <mirrorOf>!edusharing-remote,*</mirrorOf>
       </mirror>
     </mirrors>
   ```      

   and add your credentials (if necessary):

   ```
    <servers>
      <server>
        <id>edusharing-remote</id>
        <username> ... </username>
        <password> ... </password>
      </server>
    </servers>
   ```      

2. Build local docker images by calling:

   ```
   mvn clean install
   ```

Test
----

1. [Build](#build) local docker images first.

2. Start up an instance from local docker images with dev ports by calling:

   ```
   ./deploy.sh ldebug
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

2. Start up an instance from local docker images with dev ports and local artifacts

   ```
   ./deploy.sh ldev
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

   * for changes inside backend-alfresco:

     ```
     ./deploy.sh reload alfresco
     ```
   
   * for changes inside backend-services:

     ```
     ./deploy.sh reload edu-sharing
     ```

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
