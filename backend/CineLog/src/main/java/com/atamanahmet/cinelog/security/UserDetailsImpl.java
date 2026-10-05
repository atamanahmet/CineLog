package com.atamanahmet.cinelog.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailsImpl implements UserDetails {

    private final Integer id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Holds only the authentication fields needed by Spring Security.
     */
    public UserDetailsImpl(Integer id, String username, String password,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities == null ? List.of() : List.copyOf(authorities);
    }

    /**
     * Returns the authenticated user id.
     */
    public Integer getUserId() {
        return id;
    }

    /**
     * Returns the granted authorities. This project has no roles.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Returns the password hash.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Returns true. Accounts do not expire.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Returns true. Accounts are not locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Returns true. Credentials do not expire.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Returns true. Accounts stay enabled.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
