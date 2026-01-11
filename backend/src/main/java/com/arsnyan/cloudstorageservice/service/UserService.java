package com.arsnyan.cloudstorageservice.service;

import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterResponseDto;

public interface UserService {
    UserRegisterResponseDto registerAccount(UserRegisterRequestDto dto);
}
