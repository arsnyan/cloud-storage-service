package com.arsnyan.cloudstorageservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_is_disabled", columnList = "is_disabled DESC"),
        @Index(name = "idx_user_is_expired", columnList = "is_expired DESC"),
        @Index(name = "idx_user_is_locked", columnList = "is_locked DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "username", unique = true, nullable = false, columnDefinition = "varchar")
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_disabled", nullable = false)
    private Boolean isDisabled = false;

    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired = false;

    @Column(name = "credentials_expired", nullable = false)
    private Boolean isCredentialsExpired = false;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserRole> roles;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}
