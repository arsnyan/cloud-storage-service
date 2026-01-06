package com.arsnyan.cloudstorageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequestDto(
    @NotBlank
    @Size(min = 4, max = 24)
    String username,

    @NotBlank
    @Size(min = 6, max = 32)
    String password
) {}
