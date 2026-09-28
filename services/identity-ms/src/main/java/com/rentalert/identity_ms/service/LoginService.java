package com.rentalert.identity_ms.service;

import com.rentalert.dto.auth.LoginResponse;

public interface LoginService<T> {
    
    LoginResponse auth(T user);
}
