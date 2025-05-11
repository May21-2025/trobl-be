package com.may21.trobl.user.domain;

import jakarta.persistence.*;

import java.util.*;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "app_users")
public class User implements UserDetails, OAuth2User{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  private String nickname;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role")
  private List<String> roles;


  private int failedLoginAttempts;

  private boolean accountNonExpired = true;
  private boolean accountNonLocked = true;
  private boolean credentialsNonExpired = true;
  private boolean enabled = true;


  public User(Long userId, String subject, String s, Collection<GrantedAuthority> authorities) {
    this.id = userId;
    this.username = subject;
    this.password = s;
    this.roles =
        authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replace("ROLE_", ""))
            .toList();
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
    this.enabled = true;
  }
  @Override
  public String getPassword() {
    return this.password;
  }
  @Override
  public Map<String, Object> getAttributes() {
    return Map.of("id", id, "username", username, "nickname", nickname);
  }

  @Override
  public String getName() {
    return this.username;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
  }
  @Override
  public boolean isAccountNonExpired() {
    return this.accountNonExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return this.accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return this.credentialsNonExpired;
  }
  @Override
  public boolean isEnabled() {
    return enabled;
  }


  @Builder
  public User(String username, String password, String nickname, List<String> roles){
    this.username = username;
    this.password = password;
    this.nickname = nickname;
    this.roles = roles;
  }
}
