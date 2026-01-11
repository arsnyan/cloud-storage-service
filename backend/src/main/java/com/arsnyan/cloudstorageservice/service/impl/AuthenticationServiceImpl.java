package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterResponseDto;
import com.arsnyan.cloudstorageservice.service.AuthenticationService;
import com.arsnyan.cloudstorageservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;

    @Override
    public void login(String username, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authRequest = UsernamePasswordAuthenticationToken.unauthenticated(username , password);

        Authentication authResponse = authenticationManager.authenticate(authRequest);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authResponse);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    @Override
    public UserRegisterResponseDto register(UserRegisterRequestDto dto, HttpServletRequest request, HttpServletResponse response) {
        var registeredUser = userService.registerAccount(dto);

        login(dto.username(), dto.password(), request, response);

        return registeredUser;
    }

    @Override
    public void logout(UserDetails user, HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
    }
}
