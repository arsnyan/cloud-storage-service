package com.arsnyan.cloudstorageservice.service;

import com.arsnyan.cloudstorageservice.dto.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.UserRegisterResponseDto;

public interface UserService {
    UserRegisterResponseDto registerAccount(UserRegisterRequestDto dto);
}
