package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @NonNull
    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        var loadedUser = userRepository.getUserByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User %s not found".formatted(username)));

        var authorities = loadedUser.getRoles().stream()
            .map(userRole -> new SimpleGrantedAuthority(userRole.getId().getRole()))
            .toArray(SimpleGrantedAuthority[]::new);

        return User.builder()
            .username(loadedUser.getUsername())
            .password(loadedUser.getPassword())
            .accountLocked(loadedUser.getIsLocked())
            .accountExpired(loadedUser.getIsExpired())
            .credentialsExpired(loadedUser.getIsCredentialsExpired())
            .disabled(loadedUser.getIsDisabled())
            .authorities(authorities)
            .build();
    }
}
