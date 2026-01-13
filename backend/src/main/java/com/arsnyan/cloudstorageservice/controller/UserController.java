package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.authentication.UserDetailsResponseDto;
import com.arsnyan.cloudstorageservice.dto.authentication.UserLoginResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    @GetMapping("/me")
    @Operation(
        summary = "Get info about user",
        description = "Fetches user from database for verification of being signed in"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User is authenticated. Returns their username",
            content = @Content(schema = @Schema(implementation = UserDetailsResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User is not authenticated"
        )
    })
    public ResponseEntity<@NonNull UserDetailsResponseDto> getUserDetails(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new UserDetailsResponseDto(user.getUsername()));
    }
}
