alfresco 3.4e revision 26676

VersionServiceImpl.java:
- Versionierung von children gefixt


#############################
Fix Versionierung von IO's:##
#############################

Ursprungszustand
############################################
public Collection<Version> createVersion(Collection<NodeRef> nodeRefs, Map<String, Serializable> versionProperties)
public Version createVersion(NodeRef nodeRef, Map<String, Serializable> versionProperties) throws ReservedVersionNameException, AspectMissingException
- bei diesen Methoden werden in der Orginalfassung Kindobjekte eingefroren, d.h. es werden für die orginal subobjekte auch objekte in der Versionshistorie angelegt
  allerdings nicht mit den derzeitigen properties gefüllt sondern es wird sich nur eine referenz auf das orginalobjekt gemerkt

folgende Methode ist nur in dem alten VersionServiceImpl enthalten und verhält sich anders:
public Collection<Version> createVersion(NodeRef nodeRef, Map<String, Serializable> versionProperties, boolean versionChildren) throws ReservedVersionNameException, AspectMissingException
- bei dieser Methode werden (wenn versionChildren==true) auch die Kindobjekte angelegt, allerdings mit den properties des Hauptobjektes(versionProperties) gefüllt (was natürlich den Inhalt zerstört)


methode revert:
 die Implementierung des revert zielt darauf ab gelöschte, oder verschobene Objekte wiederherzustellen, aber nicht geänderte Properties der Objekte zu berücksichtigen
 außerdem gibt es einen bug:
 
 if (children.contains(versionedChild) == false)
 dies ergibt immer false Childrefs verglichen werden die zwar die gleiche id haben können, aber einen andern parent haben(Versionsparent)
 dies führt dazu das die aktuellen Kindobjekte alle entfernt werden:
   for (ChildAssociationRef ref : children)
   {
   	this.nodeService.removeChild(nodeRef, ref.getChildRef());
   }
        

Fix
#############################################################
der fix zieht bei den Methoden des Version2ServiceImpl
- public Collection<Version> createVersion(Collection<NodeRef> nodeRefs, Map<String, Serializable> versionProperties)
- public Version createVersion(NodeRef nodeRef, Map<String, Serializable> versionProperties) throws ReservedVersionNameException, AspectMissingException
nicht bei den des alten VersionServiceImpl:
 public Collection<Version> createVersion(NodeRef nodeRef, Map<String, Serializable> versionProperties, boolean versionChildren) throws ReservedVersionNameException, AspectMissingException

- Versionierung der subtypen (Educational,Contributer,Classification etc.):
	- über policy IOAfterCreateVersionPolicy wird 
		- verhindert, das das default AfterCreateVersion versioning zieht
		- die subobjekte werden versioniert und die hierarchie wird über childassociations nachempfunden
		- um zu verhindern, das für die IO SubObjekte das default OnCreateVersion handling zieht welches dazu führt,
		  daß die sub Knoten mit der referenz auf das orginalobjekt angelegt werden, werden für alle sub node Typen von 
		  edu-sharing ein eigene OnCreatePolicy erstellt: IOSubOnCreateVersionPolicy 
		  Hinweis: es scheint als ob  if (classDefinition.isContainer(), in der default implementierung VersionServiceImpl.defaultOnCreateVersion) 
		  für IO's nicht zieht aber für classification und taxon path(vielleicht weil io von cm:content abgeleitet ist und classification und taxon path von cm:object
- anpassung revert:
	- Version2ServiceImpl wird überschrieben
	- nur wenn es sich um ein IO oder ein subtyp handelt, wird das spezielle handling ausgeführt, ansonsten wird die Standard implementierung über super aufgerufen
	
	
Sonderbarkeiten:
#############################################################
wird über die nodeService Bean ein versionStore Referenz gerufen scheint spring automatisch den org.alfresco.repo.version.Node2ServiceImpl aufzurufen
welcher folgende Besonderheiten hat:
- keine Änderungs und Erstellungs Methoden sind implementiert
- wird getChildAssocs aufgerufen bekommt man
  - eine Liste (List<ChildAssociationRef>) zurück die aus ChildAssociationRef besteht die für getChildRef
    eine Referenz auf den SpacesStore zurückliefert aber einen Parent aus dem VersionStore hat
  - die Liste besteht nicht aus allen ChildAssocs im Version Store sondern nur aus denen die noch im SpacesStore existieren

das VersionObjekt:  
- Version.getFrozenStateNodeRef;: liefert eine Referenz der Form version://Version2Store/12kryptischeid23 zurück, wenn man damit über den dbNodeService gehen will
  muss man das erst in die Form workspace://version2Store/12kryptischeid23 umwandeln, entweder per Hand oder mit dem VersionUtil.convertNodeRef
- die Objekte im workspace://version2Store/ haben widerum ein Property:
  - {http://www.alfresco.org/model/versionstore/2.0}frozenNodeRef 
  - dies enthält jedoch eine Referenz auf das Orginal Objekt z.B.:	workspace://SpacesStore/037febdd-4346-42c5-bd5f-edcff847a019
- sollen subobjekte (wie contributer) nach dem Löschen auch wieder hergestellt werden können, muss im contentmodel der typ folgende Einstellung bekommen:
  <archive>true</archive> wenn nicht werden die Versionen beim löschen mit entfernt
  
  
revert und versionlabel problem
#############################################################
Beim revert merken wir uns im LOM Versionsfeld 
{http://www.campuscontent.de/model/lom/1.0}version 
zu welcher Version reverted wird, da das alfresco Versionsfeld
{http://www.alfresco.org/model/content/1.0}versionLabel
nicht zurückgesetzt wird(wird von alfresco verwendet um die nächste Version zu bestimmen).

Der renderer jedoch muss wissen welche version er aktuell vor sich hat.

Auch beim Erstellen einer neuen Version(speichern) wird das LOM Versionsfeld gesetzt.

revert und Usage
#############################################################
Die Versionierung von Usage Datensätzen macht keinen Sinn. 
Z.B. wenn ein revert ausgeführt wird zu einer Version die noch keine Usage Datensätze enthält, geht die usage Information für das LMS verloren.
D.h. es kann nicht mehr bestimmen wie es gerendert werden soll. Auch auf repository Seite wird beim Löschen nicht mehr gewarnt, dass das Objekt noch in 
einem LMS verwendet wird.

Daher wird Versionierung und revert für Usages ausgestellt.
IOAfterCreateVersionPolicy.createSubVersions -> if(isUsage) return;
IOSubOnCreateVersionPolicy String[] ioSubTypes -> usage raus
Version2ServiceImpl.revert:
- toRevertList Schleife if(isUsage) continue; usage Objekte haben immer den aktuellsten Zustand ->kein revert;
- toRestore    Schleife if(isUsage) continue; dies sollte nicht auftreten da Usage Objekte nicht mehr Versioniert werden, aber zur Sicherheit;
- toRemove     Schleife if(isUsage) continue; dies ist die wichtigste Stelle die dafür sorgt, das usage Objekte nicht gelöscht werden (wenn sie nicht in der Versionshistorie existieren).


release_1.6 Bugfix
#############################################################
- Bug1: wird der content überschrieben scheitert die Versionierung weil die autom. Erstellung der Vorschaubildes knallt(weil versionlabel fehlt)
- Bug1fix: versionlabel wird in Policy gesetzt bevor Version erstellt wird
- Bug2: wird ein subobject entfernt wird auch seine Entsprechung im Versionstore entfernt, da es sich um die primäre child 
  association handelt die entfernt wird
- Bug2fix: aktivieren der Archivierung(funktioniert jetzt) siehe Kommentar custom-core-services-context.xml 
	