package com.arsnyan.cloudstorageservice.service.impl;

import com.arsnyan.cloudstorageservice.dto.authentication.UserRegisterRequestDto;
import com.arsnyan.cloudstorageservice.exception.EntityAlreadyExistsException;
import com.arsnyan.cloudstorageservice.repository.UserRepository;
import com.arsnyan.cloudstorageservice.service.UserService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import com.arsnyan.cloudstorageservice.TestcontainersConfiguration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserServiceImplTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void registerAccount_registersIfTheUserDoesNotExist() {
        UserRegisterRequestDto dto = new UserRegisterRequestDto("username", "password");

        var registeredAccount = userService.registerAccount(dto);
        var dbUser = userRepository.getUserByUsername(dto.username());

        assertNotNull(registeredAccount);
        assertThat(dbUser).isPresent();

        assertThat(registeredAccount.username()).isEqualTo(dto.username());
        assertThat(dbUser.get().getPassword()).isNotEqualTo(dto.password());

        assertThat(dbUser.get().getRoles()).isNotEmpty();
        assertThat(dbUser.get().getRoles()).hasSize(1);

        assertThat(dbUser.get().getIsExpired()).isFalse();
        assertThat(dbUser.get().getIsLocked()).isFalse();
        assertThat(dbUser.get().getIsDisabled()).isFalse();
        assertThat(dbUser.get().getIsCredentialsExpired()).isFalse();
    }

    @Test
    void registerAccount_doesNotRegisterIfTheUserExists() {
        UserRegisterRequestDto dto = new UserRegisterRequestDto("username", "password");
        userService.registerAccount(dto);

        assertThrows(EntityAlreadyExistsException.class, () -> userService.registerAccount(dto));
    }

    @RepeatedTest(value = 3)
    void registerAccount_doesNotAllowRaceConditionsForUserExistanceCheck() {
        UserRegisterRequestDto dto = new UserRegisterRequestDto("user_" + UUID.randomUUID().toString(), "password");

        var firstTask = CompletableFuture.runAsync(() -> userService.registerAccount(dto));
        var secondTask = CompletableFuture.runAsync(() -> userService.registerAccount(dto));
        var thirdTask = CompletableFuture.runAsync(() -> userService.registerAccount(dto));

        var combined = CompletableFuture.allOf(firstTask, secondTask, thirdTask);

        assertThrows(Exception.class, combined::join);
    }
}