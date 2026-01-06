package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.dto.UserRegisterResponseDto;
import com.arsnyan.cloudstorageservice.exception.UserAlreadyExistsException;
import com.arsnyan.cloudstorageservice.model.User;
import com.arsnyan.cloudstorageservice.model.UserRole;
import com.arsnyan.cloudstorageservice.model.UserRoleId;
import com.arsnyan.cloudstorageservice.repository.UserRepository;
import com.arsnyan.cloudstorageservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserRegisterResponseDto registerAccount(UserRegisterRequestDto dto) {
        if (userRepository.getUserByUsername(dto.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username already taken");
        }
        
        var encryptedPassword = passwordEncoder.encode(dto.password());
        var user = new User();
        user.setUsername(dto.username());
        user.setPassword(encryptedPassword);

        var defaultRoleId = new UserRoleId(dto.username(), "DEFAULT");
        var defaultRole = new UserRole(defaultRoleId, user);

        user.setRoles(Set.of(defaultRole));

        userRepository.save(user);

        return new UserRegisterResponseDto(dto.username());
    }
}
