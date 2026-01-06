package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.UserLoginRequestDto;
import com.arsnyan.cloudstorageservice.dto.UserLoginResponseDto;
import com.arsnyan.cloudstorageservice.dto.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.UserRegisterResponseDto;
import com.arsnyan.cloudstorageservice.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<@NonNull UserLoginResponseDto> login(
        @RequestBody @Valid UserLoginRequestDto dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        authenticationService.login(dto.username(), dto.password(), request, response);
        return ResponseEntity.ok(new UserLoginResponseDto(dto.username()));
    }

    @PostMapping("/register")
    public ResponseEntity<@NonNull UserRegisterResponseDto> register(
        @RequestBody @Valid UserRegisterRequestDto dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        var registeredUser = authenticationService.register(dto, request, response);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/logout")
    public ResponseEntity<@NonNull Void> logout(@AuthenticationPrincipal UserDetails user, HttpServletRequest request) {
        authenticationService.logout(user, request);
        return ResponseEntity.noContent().build();
    }
}
