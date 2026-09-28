package com.rentalert.identity_ms.security;

import com.bsu.renalert.identity.application.service.AuthenticationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private final AuthenticationService authenticationService;

    @Override
    protected void additionalAuthenticationChecks(
            @NonNull UserDetails userDetails,
            @NonNull UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    }

    @NonNull
    @Override
    protected UserDetails retrieveUser(
            @NonNull String username,
            @NonNull UsernamePasswordAuthenticationToken authentication
    ) throws AuthenticationException {
        return authenticationService.findUserByToken(String.valueOf(authentication.getCredentials()));
    }
}