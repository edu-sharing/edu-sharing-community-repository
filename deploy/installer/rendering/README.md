# edu-sharing community - deploy installer rendering

Prerequisites
-------------

- Apache2 Webserver
- PHP Framework 7.1+
- MySQL Community Server / MariaDB Server
- Apache Maven 3.6.3+

Download
--------

1. Download the installer artifact:

   * [releases](https://artifacts.edu-sharing.com/#browse/browse:community-releases:org%2Fedu_sharing%2Fedu_sharing-community-deploy-installer-rendering)
   * [snapshots](https://artifacts.edu-sharing.com/#browse/browse:community-snapshots:org%2Fedu_sharing%2Fedu_sharing-community-deploy-installer-rendering)

Build
-----

1. Check out the [rendering-project](https://scm.edu-sharing.com/edu-sharing/rendering-service) outside of this project.
 
2. Build installer artifact by calling: 
                                                    
   ```
   ./deploy.sh build <rendering-project>
   ```

   After that you can find the installer artifact inside the `target` subdirectory.

Installation
------------

1. Open a bash shell and go to the document root of your website.

2. Unpack the [downloaded](#download) or [builted](#build) installer artifact.

3. Edit the configuration of your website like:

   ```
   <Directory <docroot>/esrender>  
     Options -Indexes +FollowSymLinks
     AllowOverride All  
     Require all granted
   </Directory>
   ```

4. Create a new directory for caching files. 
   Make sure the user under which the webserver is running has read/write access in this directory.

   > This cache directory should not be inside the document root. 

5. Start the installation wizard by opening `http://yourdomain.de/esrender/admin/`.
  
Update
------

1. Open a bash shell and go to the document root of your website.

2. Unpack the [downloaded](#download) or [builted](#build) installer artifact.

3. Start the update wizard by opening `http://yourdomain.de/esrender/admin/`.
    
---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
