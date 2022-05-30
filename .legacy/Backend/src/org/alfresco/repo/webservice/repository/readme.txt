- alfresco bug fix for org.alfresco.repo.webservice.repository.AssociatedQuerySession.java
  when calling
  repServStub.queryAssociated(ref, assoc)
  
  AssociatedQuerySession.getNextResultsBatch(...)
  
  
- wurde entfernt 2010_01_19 weil bei alfresco3Stable funktioniert diese implemntierung nicht mehre
	vermute das das problem gelöst wurde. ansonsten im svn nachschauen  