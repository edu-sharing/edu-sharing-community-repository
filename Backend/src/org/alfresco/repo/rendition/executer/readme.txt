- alfresco fix:
	- the rendition service overwrites all properties of an thumbnail when it's updated (IO Helper Thumbnail action is called)
	-> so also the version label will be removed
	- together with ChildAssociation Versioning fix in org.edu_sharing.alfresco.fixes 
	  this leads to an Exception when the fix tries to version the child thumbnail cause there is a version
	  which has no corresponding Node in workspace://SpacesStore
	- the fix is to remember the versionlabel before it's overwritten and to write it back after the update is done
	- the fix is in:  AbstractRenderingEngine.createOrUpdateRendition
	- the class is placed in alfresco-repository-3.4.e.jar cause there was no possibility to things like policies or 
	  spring beans to make an "plug in" fix