<#macro accessControlListJSON acl>
      {
         "aclId": ${acl.aclId?c},
         "entries" :
         [
            <#list acl.aces as ace>
               <@accessControlEntryJSON ace=ace/>
               <#if ace_has_next>,</#if>
            </#list>
         ]
      }
</#macro>

<#macro accessControlEntryJSON ace>
    {
        "authority": ${ace.authority},
        "permission": ${ace.permission}
    }
</#macro>


{
   "accessControlLists" :
   [
      <#list accessControlLists as acl>
         <@accessControlListJSON acl=acl/>
         <#if acl_has_next>,</#if>
      </#list>
   ]
}