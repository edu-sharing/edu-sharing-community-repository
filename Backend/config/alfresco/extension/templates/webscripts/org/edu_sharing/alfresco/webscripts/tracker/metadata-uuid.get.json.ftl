<#-- https://github.com/Alfresco/alfresco-remote-api/tree/master/src/main/resources/alfresco/templates/webscripts/org/alfresco/repository/solr -->

<#macro json_string string>${string?js_string?replace("\\'", "\'")?replace("\\>", ">")}</#macro>

<#macro nodeMetaDataJSON nodeMetaData filter>
      {
         "id": ${nodeMetaData.nodeId?c}
         <#if nodeMetaData.tenantDomain??>, "tenantDomain": "${nodeMetaData.tenantDomain}"</#if>
         <#if filter.includeNodeRef??><#if nodeMetaData.nodeRef??>, "nodeRef": "${nodeMetaData.nodeRef.toString()}"</#if></#if>
         <#if filter.includeType??><#if nodeMetaData.nodeType??>, "type": <@qNameJSON qName=nodeMetaData.nodeType/></#if></#if>
         <#if filter.includeAclId??><#if nodeMetaData.aclId??>, "aclId": ${nodeMetaData.aclId?c}</#if></#if>
         <#if filter.includeTxnId??><#if nodeMetaData.txnId??>, "txnId": ${nodeMetaData.txnId?c}</#if></#if>
         <#if filter.includeProperties??>
         <#if nodeMetaData.properties??>
         , "properties": {
           <#list nodeMetaData.properties?keys as propName>
               "${propName}": ${nodeMetaData.properties[propName]}<#if propName_has_next>,</#if>
           </#list>
         }
         </#if>
         </#if>
         <#if filter.includeAspects??>
         <#if nodeMetaData.aspects??>
         , "aspects": [
           <#list nodeMetaData.aspects as aspectQName>
               <@nodeAspectJSON aspectQName=aspectQName indent=""/><#if aspectQName_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         <#if filter.includePaths??>
         <#if nodeMetaData.paths??>
         , "paths": [
           <#list nodeMetaData.paths as path>
           ${path}<#if path_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         <#if filter.includePaths??>
         <#if nodeMetaData.ancestors??>
         <#if (nodeMetaData.ancestors?size > 0)>
         , "ancestors": [
           <#list nodeMetaData.ancestors as ancestor>
           "${ancestor}"<#if ancestor_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         </#if>
         <#if filter.includePaths??>
         <#if nodeMetaData.namePaths??>
         , "namePaths": [
           <#list nodeMetaData.namePaths as namePath>
           ${namePath}<#if namePath_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         <#if filter.includeParentAssociations??>
         <#if nodeMetaData.parentAssocs??>
         <#if (nodeMetaData.parentAssocs?size > 0)>
         , "parentAssocs": [
           <#list nodeMetaData.parentAssocs as pa>
           "<@json_string "${pa}"/>"<#if pa_has_next>,</#if>
           </#list>
         ]
         ,"parentAssocsCrc": <#if nodeMetaData.parentAssocsCrc??>${nodeMetaData.parentAssocsCrc?c}<#else>null</#if>
         </#if>
         </#if>
         </#if>
         <#if filter.includeChildAssociations??>
         <#if nodeMetaData.childAssocs??>
         <#if (nodeMetaData.childAssocs?size > 0)>
         , "childAssocs": [
           <#list nodeMetaData.childAssocs as ca>
           "<@json_string "${ca}"/>"<#if ca_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         </#if>
         <#if filter.includeChildIds??>
         <#if nodeMetaData.childIds??>
         <#if (nodeMetaData.childIds?size > 0)>
         , "childIds": [
           <#list nodeMetaData.childIds as ci>
           ${ci?c}<#if ci_has_next>,</#if>
           </#list>
         ]
         </#if>
         </#if>
         </#if>
         <#if filter.includeOwner??>
         <#if nodeMetaData.owner??>
         , "owner": "${nodeMetaData.owner}"
         </#if>
         </#if>
      }
</#macro>

<#macro qNameJSON qName indent="">
${indent}"${jsonUtils.encodeJSONString(shortQName(qName))}"
</#macro>

<#macro nodeAspectJSON aspectQName indent="">
${indent}<@qNameJSON qName=aspectQName/>
</#macro>

{
   "node" : <@nodeMetaDataJSON nodeMetaData=nodeMetaData filter=filter/>
}