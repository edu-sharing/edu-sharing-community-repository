<#macro accessControlListJSON acl indent="">
     {
     "aclId": "${acl.aclId?c}",
     "inherits": "${acl.inherits?c}",
     "aces" :
         [
        <#list acl.aces as ace>
            <@accessControlEntryJSON ace=ace indent="    "/>
            <#if ace_has_next>,</#if>
        </#list>
        ]
    }
</#macro>

<#macro accessControlEntryJSON ace indent="">
    ${indent} {
        "authority": "${ace.authority}",
        "permission": "${ace.permission}"
    ${indent} }
</#macro>


{
    "accessControlLists" :
    [
        <#list accessControlLists as acl>
            <@accessControlListJSON acl=acl indent="    "/>
            <#if acl_has_next>,</#if>
        </#list>
    ]
}