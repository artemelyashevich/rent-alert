package com.rentalert.common.security;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


@UtilityClass
public class InternalAuth {

    public static final String HEADER = "X-Internal-Token";

    public static final String URL_PATTERN = "/*/internal/**";

    public static final String AUTHORITY = "INTERNAL";

    public static boolean isInternalCall() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> AUTHORITY.equals(authority.getAuthority()));
    }

    public static Authentication withInternalAuthority(Authentication userAuthentication) {
        List<GrantedAuthority> authorities = new ArrayList<>(userAuthentication.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(AUTHORITY));
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(userAuthentication.getPrincipal(), userAuthentication.getCredentials(), authorities);
        authentication.setDetails(userAuthentication.getDetails());
        return authentication;
    }
}
