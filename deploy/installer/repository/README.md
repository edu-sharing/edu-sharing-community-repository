
# edu-sharing community - deploy installer repository

Prerequisites
-------------

- Apache Maven 3.6.3+
- Git SCM

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
    
1. Build installer artifact by calling:
  
   ```
   ./deploy.sh build
   ```    

   After that you can find the installer artifact inside the `target` subdirectory.
   
Installation
------------

0. Install [Alfresco Community Platform 5.2.g](https://hub.alfresco.com/t5/alfresco-content-services-hub/alfresco-community-edition-201707-ga-file-list/ba-p/290487)
   by using a PostgreSQL 12+ database. 

1. Set the environment variable `ALF_HOME` with the home directory of your Alfresco installation, for example:

   ```
   export ALF_HOME=/opt/alfresco
   ```

2. Clean up outdated libraries by calling:

   ```
   rm -f $ALF_HOME/tomcat/lib/postgresql-*
   rm -f $ALF_HOME/tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
   rm -f $ALF_HOME/tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
   rm -f $ALF_HOME/tomcat/webapps/alfresco/WEB-INF/lib/jackson-*
   ```

   and adding following lines into `$ALF_HOME/tomcat/bin/setenv.sh` by calling:

   ```
   tee -a $ALF_HOME/tomcat/bin/setenv.sh << EOL
   CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS"    
   CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS"
   CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS"
   CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS"
   EOL
   ```

3. Make an initial snapshot of the tomcat subdirectory by calling:

   ```
   pushd $ALF_HOME
   tar -czvf snapshot.tar.gz tomcat
   popd
   ```
 
4. Unpack the installer artifact (see [Build](#build)) into the home directory of your Alfresco installation by calling:

   ```
   tar -xzvf target/edu_sharing-community-deploy-installer-repository-<version>-bin.tar.gz -C $ALF_HOME
   ```
   
5. Deploy the Alfresco Module Packages (AMP) by calling:

   ```
   $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/alfresco/0 $ALF_HOME/tomcat/webapps/alfresco -directory -nobackup -force
   $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/alfresco/1 $ALF_HOME/tomcat/webapps/alfresco -directory -nobackup -force
   $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/edu-sharing/1 $ALF_HOME/tomcat/webapps/edu-sharing -directory -nobackup -force
   ```

6. Initialize version control for your configuration by calling:

   ```
   pushd $ALF_HOME
   git init
   git branch -m original
   git add tomcat/shared/*
   git commit -m "After install."
   git checkout -b custom
   popd
   ```
   
7. Start the server by calling:

   ```
   pushd $ALF_HOME
   ./alfresco.sh start
   popd
   ```
   
Update
------

1. Stop the server by calling:

   ```
   pushd $ALF_HOME
   ./alfresco.sh stop
   popd
   ```

2. Save your custom configuration by calling:

   ```
   pushd $ALF_HOME
   git add tomcat/shared/*
   git commit -m "Before update."
   git checkout original
   popd
   ```
   
3. Rollback the tomcat subdirectory based on your initial snapshot by calling:

   ```
   pushd $ALF_HOME
   rm -rf $ALF_HOME/tomcat
   tar -xzvf $ALF_HOME/snapshot.tar.gz -C $ALF_HOME
   popd
   ```

4. Unpack the installer artifact (see [Build](#build)) into the home directory of your Alfresco installation `$ALF_HOME`.

   ```
   tar -xzvf target/edu_sharing-community-deploy-installer-repository-<version>-bin.tar.gz -C $ALF_HOME
   ```

5. Deploy the Alfresco Module Packages (AMP) by calling:

   ```
   $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/alfresco/0 $ALF_HOME/tomcat/webapps/alfresco -directory -force
   $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/alfresco/1 $ALF_HOME/tomcat/webapps/alfresco -directory -force
   $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/edu-sharing/1 $ALF_HOME/tomcat/webapps/edu-sharing -directory -force
   ```

6. Merge changes into your custom configuration by calling:

   ```
   pushd $ALF_HOME
   git add tomcat/shared/*
   git commit -m "After update."
   git checkout custom
   git merge original -m "Merge update."
   popd
   ```
   
7. Start the server again by calling:

   ```
   pushd $ALF_HOME
   ./alfresco.sh start
   popd
   ```
      
---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
