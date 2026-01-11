package com.arsnyan.cloudstorageservice.repository;

import com.arsnyan.cloudstorageservice.model.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<@NonNull User, @NonNull Long> {
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> getUserByUsername(String username);

    @Query("SELECT u.id FROM User u")
    Optional<Long> getUserIdByUsername(String username);

    boolean existsByUsername(String username);
}
