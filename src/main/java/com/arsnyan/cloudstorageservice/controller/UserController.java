package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.UserDetailsResponseDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    @GetMapping("/me")
    public ResponseEntity<@NonNull UserDetailsResponseDto> getUserDetails(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new UserDetailsResponseDto(user.getUsername()));
    }
}
