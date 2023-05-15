The xml factories and parsers in this project are loaded with java service loader.
They can also be defined or overwritten with System properties.
in `tomcat/bin/setenv.sh`.
```bash

CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS"
CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=org.edu_sharing.xml.security.jaxp.DocumentBuilderFactory $CATALINA_OPTS"
CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS"

CATALINA_OPTS="-Djavax.xml.xpath.XPathFactory:http://java.sun.com/jaxp/xpath/dom=org.edu_sharing.xml.security.xpath.XPathFactory $CATALINA_OPTS"
CATALINA_OPTS="-Dorg.xml.sax.driver=org.edu_sharing.xml.security.sax.XMLReader $CATALINA_OPTS"
CATALINA_OPTS="-Djavax.xml.transform.TransformerFactory=org.edu_sharing.xml.security.transform.TransformerFactory $CATALINA_OPTS"
CATALINA_OPTS="-Djavax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema=org.edu_sharing.xml.security.validation.SchemaFactory $CATALINA_OPTS"
```