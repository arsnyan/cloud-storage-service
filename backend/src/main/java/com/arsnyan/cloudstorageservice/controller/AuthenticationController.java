package com.arsnyan.cloudstorageservice.controller;

import com.arsnyan.cloudstorageservice.dto.authentication.UserLoginRequestDto;
import com.arsnyan.cloudstorageservice.dto.authentication.UserLoginResponseDto;
import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterResponseDto;
import com.arsnyan.cloudstorageservice.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/sign-in")
    @Operation(
        summary = "User login",
        description = "Signs in a user account"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Sign in successful. Returns username and makes a session token",
            content = @Content(schema = @Schema(implementation = UserLoginResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Incorrect user credentials or the user doesn't exist"
        )
    })
    public ResponseEntity<@NonNull UserLoginResponseDto> login(
        @RequestBody @Valid UserLoginRequestDto dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        authenticationService.login(dto.username(), dto.password(), request, response);
        return ResponseEntity.ok(new UserLoginResponseDto(dto.username()));
    }

    @PostMapping("/sign-up")
    @Operation(
        summary = "User registration",
        description = "Registers a new user account and makes a session token. User immediately logins after that"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Registration successful. Returns username and makes a session token",
            content = @Content(schema = @Schema(implementation = UserRegisterResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Username is already taken"
        )
    })
    public ResponseEntity<@NonNull UserRegisterResponseDto> register(
        @RequestBody @Valid UserRegisterRequestDto dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        var registeredUser = authenticationService.register(dto, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/sign-out")
    @Operation(
        summary = "User logout",
        description = "Logs out the user and removes session cookie from their browser"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Logout successful. User no longer holds session tokens"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User is not authenticated"
        )
    })
    public ResponseEntity<@NonNull Void> logout(@AuthenticationPrincipal UserDetails user, HttpServletRequest request) {
        authenticationService.logout(user, request);
        return ResponseEntity.noContent().build();
    }
}
