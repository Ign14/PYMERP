package com.datakomerz.pymes.security;

import com.datakomerz.pymes.auth.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

  private final UserAccountRepository repository;

  public AppUserDetailsService(UserAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return repository.findByEmailIgnoreCase(username)
      .map(AppUserDetails::new)
      .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}
