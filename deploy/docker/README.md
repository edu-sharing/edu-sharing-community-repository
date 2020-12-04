
# edu-sharing community - deploy docker

Prerequisites
-------------

- Docker Engine 19.03.0+
- Docker Compose 1.27.4+ 

Download
--------

1. Download the docker artifact:

   * [releases](https://artifacts.edu-sharing.com/#browse/browse:community-releases:org%2Fedu_sharing%2Fedu_sharing-community-deploy-docker-deploy)
   * [snapshots](https://artifacts.edu-sharing.com/#browse/browse:community-snapshots:org%2Fedu_sharing%2Fedu_sharing-community-deploy-docker-deploy)

Installation
------------

1. Unpack the [downloaded](#download) docker artifact.
 
2. Start up an instance by calling:

   ```
   ./deploy.sh start
   ```
  
3. Shut down the instance by calling:

   ```
   ./deploy.sh stop
   ```
  
4. Save all data volumes on disk by calling:

   ```
   ./deploy.sh backup <backup-directory>
   ```
   
   > Later on you can restore all data valumes from disk by calling:
   > ```
   > ./deploy.sh restore <backup-directory>
   > ```
   
5. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```

---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.
