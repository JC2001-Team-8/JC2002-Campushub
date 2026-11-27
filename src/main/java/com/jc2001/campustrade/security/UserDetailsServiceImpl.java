package com.jc2001.campustrade.security;

import com.jc2001.campustrade.entity.UserEntity;
import com.jc2001.campustrade.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository users;
    public UserDetailsServiceImpl(UserRepository users) { this.users = users; }

    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        String until = user.getRestrictedUntil() == null ? null : user.getRestrictedUntil().toString();
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getStatus(), until,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }
}
