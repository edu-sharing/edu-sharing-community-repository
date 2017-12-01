allow ReplicationHandler.inform(SolrCore core) and SnapPuller.doCommit() to use AlfrescoUpdateHandler 
cause it's nearly the same as the DirectUpdateHandler2 that is needed normaly

SnapShooter is only because it uses the ReplactionHandler and we want itr to use the overwritten one

call in bin dir:
jar -cf edu-sharing-solr.jar .
deploy to alf_data/solr/lib -> the jars will be loaded to solr webapp classloader

use in solrconfig.xml:

<!--<requestHandler name="/replication" class="solr.ReplicationHandler" >-->
   <requestHandler name="/replication" class="org.edu_sharing.solr.ReplicationHandler" >



it was better to create own package to not overwrite the oriignal classes in solr-core.jar
cause then we got ClassNotFoundException for AlfrescoUpdateHandler and depending classes
-> cause original ReplicationHanlder is loaded before alfresco-solr libs are loaded