alf5.2
-----------
Alle Klassen außer Webdavlockservice wurden kopiert von alfresco sourcen.
Edu_SharingPutMethod erstmal nicht mehr verwendet evt. ist der bug von alfresco gefixt
WebdavMethod wie beim vorgänger (keine Änderungen)

alf5.0.d
------------
Alle Klassen außer Edu_Sharing*.java wurden kopiert von alfresco sourcen. Der Gund ist:
Probleme gab es beim WebDavHelper dess Konstruktor protected ist. d.h. wenn man das gleiche package hat welches aber unter einem anderen classpath liegt
gibt es eine IllegalAccessException.

Anpassungen:
Edu_SharingWebDAVServlet,EduSharingBeanProxyFilter
- die Beans werden nicht aus dem servletcontext gezogen sondern aus dem spring Application Context

WebDAVMethod: runas admin cause of groupfolder permission problem
Edu_SharingPropFindMethod: run as admin, hide favorite folders

Probleme:
- bei kopie gehen metadaten(userdefinierte) verloren
- Windows7 webdav client: beim kopieren: put anstatt copy


in alf4.2.f
-------------------------------------------
Edu_SharingBeanProxyFilter (wahrscheinlich einfach übernehmen -> ApplicationContext)

Edu_SharingPropFindMethod: 
	- favoriten rausfiltern
	- runAsAdmin when all parents of groupfolder will be traversed

!!!!!! scheint shcon gefixt zu sein testen!!! Edu_SharingPutMethod:
- mimetyp setzten über guess für clientes die quatsch mitschicken

Edu_SharingUnlockMethod:
- preview on unlock (ms office)

Edu_SharingPutMethod fix 4.2.f :
- hier wird ein nocontent aspect gesetzt der dazu führt, das die onContentUpdatePolicy nicht zieht
  und somit keine Version erstellt wird
  --> ist content da remove den nocontent aspect

Edu_SharingWebDAVServlet:
- eigene Methods registrieren
- WebdavHelper in es webapp deployen weil er auf webdav Klassen zugreifen die zum Teil unter es deployed sind
  daher nicht als bean verwenden sondern händisch erstellen und initialiseren
  
WebDAVMethod: 
runas admin cause of groupfolder permission problem
!!!TODO can cause perfomance Problems when many groupfolder exsist

alf4.2.f
Auch hier müssen die Klassen wieder kopiert werden sonst z.B.:

org.alfresco.repo.webdav.OptionsMethod cannot be cast to org.alfresco.repo.webdav.WebDAVMethod

alles reinkopiert aus package org.alfresco.repo.webdav


Linkage errror:
- WebDAVLockService/Impl -> wieder entfernt da über alf spring initialisiert


in 5.0.d
-------------------------------------------
WebDAVMethod -> (dort wird wgen velinkten objekten mit admin losgerannt -> neue version anpassen
		     -> determineSiteId (als admin) 23. Juni
WebDAVHelper (wird von webdavmethod genutzt)

alles reinkopiert aus package org.alfresco.repo.webdav

Edu_SharingPropFindMethod 
- aktuelle Version reinkopiert und es anpassungen hinzugefügt

Edu_SharingUnlockMethod(hat sich eigentlich nicht geändert):
- preview on unlock (ms office)

Edu_SharingPutMethod:
- hier wird ein nocontent aspect gesetzt der dazu führt, das die onContentUpdatePolicy nicht zieht
  und somit keine Version erstellt wird
  --> ist content da remove den nocontent aspect
  
Edu_SharingWebDAVServlet:
- eigene Methods registrieren
- WebdavHelper in es webapp deployen weil er auf webdav Klassen zugreifen die zum Teil unter es deployed sind
  daher nicht als bean verwenden sondern händisch erstellen und initialiseren
  
webDAVLockService: 
- webDAVLockService kommt von alf (als bean) daher muss auch das .class file von alf webapp stammen
  -> webDAVLockService als kopie in es webapp entfernt
