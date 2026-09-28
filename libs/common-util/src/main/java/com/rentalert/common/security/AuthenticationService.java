package com.rentalert.common.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationService {

    UserDetails findUserByToken(String token);
}
