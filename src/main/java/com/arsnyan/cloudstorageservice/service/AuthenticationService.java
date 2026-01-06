package com.arsnyan.cloudstorageservice.service;

import com.arsnyan.cloudstorageservice.dto.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.UserRegisterResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationService {
    void login(String username, String password, HttpServletRequest request, HttpServletResponse response);

    UserRegisterResponseDto register(UserRegisterRequestDto dto, HttpServletRequest request, HttpServletResponse response);

    void logout(UserDetails user, HttpServletRequest request);
}
