package com.arsnyan.cloudstorageservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Resource parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Resource> children = new ArrayList<>();

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ResourceType type;

    @Column(name = "size")
    private Long size;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isFile() { return type == ResourceType.FILE; }

    public boolean isDirectory() { return type == ResourceType.DIRECTORY; }

    public boolean isRoot() { return parent == null; }

    public static Resource directory(String name, User owner, Resource parent) {
        return Resource.builder()
            .name(name)
            .owner(owner)
            .parent(parent)
            .type(ResourceType.DIRECTORY)
            .build();
    }

    public static Resource file(String name, User owner, Resource parent, Long size) {
        return Resource.builder()
            .name(name)
            .owner(owner)
            .parent(parent)
            .type(ResourceType.FILE)
            .size(size)
            .build();
    }
}
