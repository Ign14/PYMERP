package com.datakomerz.pymes.security;

import com.datakomerz.pymes.auth.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {

  private final UserAccount account;
  private final List<GrantedAuthority> authorities;

  public AppUserDetails(UserAccount account) {
    this.account = account;
    this.authorities = List.of(account.getRoles().split(","))
      .stream()
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .map(SimpleGrantedAuthority::new)
      .collect(Collectors.toList());
  }

  public UUID getCompanyId() {
    return account.getCompanyId();
  }

  public String getDisplayName() {
    return account.getName();
  }

  public UserAccount getAccount() {
    return account;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return account.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return account.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return "active".equalsIgnoreCase(account.getStatus());
  }
}
