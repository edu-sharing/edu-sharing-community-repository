
# edu-sharing community - deploy installer repository

Prerequisites
-------------
- Debian:Bullseye
- Postgresql 13 with JDBC 42.3.1
- Libreoffice
- Alfresco 5.2g [(Alfresco Community Edition 201707 GA)](https://hub.alfresco.com/t5/alfresco-content-services-hub/alfresco-community-edition-201707-ga-file-list/ba-p/290487)
- JDK 8
- Apache Maven 3.6.3 or later
- Apache-Tomcat 8.5.73
- Apache2 (Optional)
- Elasticsearch 7 (Optional)
- xmlstarlet
- ruby-hocon
- systemd

Build
-----

0. If you have switched on additional plugins (see below), 
   then you have to add your credentials for each plugin in `$HOME/.m2/settings.xml` too:
   
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

   Build installer artifact with maven. After that you can find the installer artifact inside the `target` subdirectory.
   
Installation or Update
----------------------

- Before the first launch, the installer creates a snapshot of the current state of the alfresco installation.
  Do not delete or move these alfresco-base-SNAPSHOT, it is required for the update process!
  During the update, the installation program saves the persistent settings and then rolls back the edu-sharing installation.
  Each update is treated as a new installation. At the end, the saved persistent settings are restored.
- This installer provides the edu-sharing repository and the elastic tracker service. 
  Both can be installed on the same machine, but they can also be installed on independent machines.
  In order to install elastic tracker, it is necessary that the edu-sharing repository is already in operation.

0. Install [Alfresco Community Platform 5.2.g](https://hub.alfresco.com/t5/alfresco-content-services-hub/alfresco-community-edition-201707-ga-file-list/ba-p/290487)
   by using a PostgreSQL 13+ database. Have a look at the [Dockerfile](scripts/debian/bullseye/src/main/build/Dockerfile)
   for complete installation process of the required applications.


1. Set the environment variable `ALF_HOME` with the home directory of your Alfresco installation, for example:
   ```
   export ALF_HOME=/opt/alfresco
   ```

2. Stop Tomcat before continuing with the installation process
   ```
   "${ALF_HOME}/alfresco.sh" stop
   ```

3. Download the specific edu-sharing version from Maven repository 
   ```
   BRANCH=maven-release-6.1
   mvn -q dependency:get \
       -Dartifact="org.edu_sharing:edu_sharing-community-deploy-installer-repository-scripts-debian-bullseye:${BRANCH}:tar.gz:bin" \
       -DremoteRepositories= \
            myreleases::::https://artifacts.edu-sharing.com/repository/community-releases/, \
            mysnapshots::::https://artifacts.edu-sharing.com/repository/community-snapshots/ \
       -Dtransitive=false
      
   mvn -q dependency:copy \
       -Dartifact="org.edu_sharing:edu_sharing-community-deploy-installer-repository-scripts-debian-bullseye:${BRANCH}:tar.gz:bin" \
       -DoutputDirectory=.
   ```
4. Unzip the installer 
   ```
   tar xzf "./edu_sharing-community-deploy-installer-repository-scripts-debian-bullseye-${BRANCH}-bin.tar.gz"
   rm "./edu_sharing-community-deploy-installer-repository-scripts-debian-bullseye-${BRANCH}-bin.tar.gz"
   ```
      
5. Run the installer

   To install or update repository and elastic tracker:
   ```
   "./install.sh" --all
   ```
   OR to install or update repository only:
   ```
   "./install.sh" --repository
   ``` 
   OR to install or update elastic tracker only:
   ```
   "./install.sh" --elastictracker
   ```
   Next to the installer you will find an .env.base file which contains the default configuration of edu-sharing.
   Since this file is supplied with the installer and will be overwritten by the next update, the file should not be modified!
   Instead, create a separate .env file and define only the relevant settings you want to adjust.
   In addition, a log file with the used configuration will be created next to the installer.
   ```
   "./install.sh" --all -f .env
   ```
   To use a local build of edu-sharing.
   You will need to run the [distribution](distribution) until install in maven, before you can use it by the installer locally.
   ```
   "./install.sh" --all --local
   ```
   
6. Start Tomcat
   ```
   "${ALF_HOME}/alfresco.sh" start
   ```
---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
