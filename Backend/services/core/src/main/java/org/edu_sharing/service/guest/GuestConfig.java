//package org.edu_sharing.service.guest;
//
//import lombok.Data;
//import org.edu_sharing.lightbend.ParameterizedConfigurationProperties;
//
//import java.util.List;
//
//@Data
//@ParameterizedConfigurationProperties(prefix =  /* language=SpEL */ "#{T(org.apache.commons.lang3.StringUtils).isNotBlank(#root?.contextId)?'repository.context.'+#root?.contextId+'.':''}repository.guest")
//public class GuestConfig {
//    boolean enabled;
//    String username;
//    String password;
//    List<String> groups;
//}
