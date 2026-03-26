package org.edu_sharing.spring.security.basic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.alfresco.repo.security.authentication.RepositoryAuthenticatedUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import net.sf.acegisecurity.providers.dao.User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Getter
public class EduSharingPrincipal implements UserDetails {

    private  String username;
    private  String password;
    private  boolean enabled;
    private  boolean accountNonExpired;
    private  boolean credentialsNonExpired;
    private  boolean accountNonLocked;

    private  Collection<? extends GrantedAuthority> authorities;

    private  List<String> hashIndicator;
    private  Serializable salt;

    public EduSharingPrincipal() {

    }

    @JsonCreator
    public EduSharingPrincipal(
            @JsonProperty("username") String username,
            @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
            @JsonProperty("hashIndicator") List<String> hashIndicator,
            @JsonProperty("salt") Serializable salt
    ) {
        this.username = username;
        this.authorities = authorities;
        this.hashIndicator = hashIndicator;
        this.salt = salt;
    }

    public EduSharingPrincipal(User repo) {

        this.username = repo.getUsername();
        this.password = repo.getPassword();
        this.enabled = repo.isEnabled();
        this.accountNonExpired = repo.isAccountNonExpired();
        this.credentialsNonExpired = repo.isCredentialsNonExpired();
        this.accountNonLocked = repo.isAccountNonLocked();
        this.authorities = map(repo.getAuthorities());

        if(repo instanceof RepositoryAuthenticatedUser){
            this.hashIndicator =  ((RepositoryAuthenticatedUser)repo).getHashIndicator();
            this.salt = ((RepositoryAuthenticatedUser)repo).getSalt();
        }

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities ;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return this.username ;
    }

    public static Collection<GrantedAuthority> map(net.sf.acegisecurity.GrantedAuthority[] acegiAuthorities) {

        if (acegiAuthorities == null) {
            return List.of();
        }

        return List.of(acegiAuthorities).stream()
                .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                .collect(Collectors.toList());
    }
}
