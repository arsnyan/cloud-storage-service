package com.arsnyan.cloudstorageservice.repository;

import com.arsnyan.cloudstorageservice.model.Resource;
import com.arsnyan.cloudstorageservice.model.ResourceType;
import com.arsnyan.cloudstorageservice.model.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<@NonNull Resource, @NonNull UUID> {
    List<Resource> findByOwnerAndParentIsNull(User owner);

    List<Resource> findByOwnerAndParent(User owner, Resource parent);

    Optional<Resource> findByOwnerAndParentAndNameAndType(User owner, Resource parent, String name, ResourceType type);

    Optional<Resource> findByOwnerAndParentIsNullAndNameAndType(User owner, String name, ResourceType type);

    Optional<Resource> findByOwnerAndParentAndName(User owner, Resource parent, String name);

    Optional<Resource> findByOwnerAndParentIsNullAndName(User owner, String name);

    boolean existsByOwnerAndParentAndNameAndType(User owner, Resource parent, String name, ResourceType type);

    boolean existsByOwnerAndParentIsNullAndNameAndType(User owner, String name, ResourceType type);

    @NativeQuery("""
        with recursive descendants as (                    
            select r.* from resources r where r.parent_id = :parentId
            
            union all
            
            select r.* from resources r
            inner join descendants d on r.parent_id = d.id
        )
        select * from descendants
        """)
    List<Resource> findAllDescendants(@Param("parentId") UUID parentId);

    @NativeQuery("""
        with recursive descendants as (
            select r.* from resources r where r.parent_id = :parentId
            union all
            select r.* from resources r
            inner join descendants d on r.parent_id = d.id
        )
        select * from descendants where type = 'FILE'
        """)
    List<Resource> findAllDescendantFiles(@Param("parentId") UUID parentId);

    @NativeQuery("""
        with recursive path_parts as (
            select r.id, r.parent_id, r.name, r.type, 1 as depth
            from resources r
            where r.id = :resourceId
            
            union all
            
            select r.id, r.parent_id, r.name, r.type, pp.depth + 1
            from resources r
            inner join path_parts pp on r.id = pp.parent_id
        )
        select string_agg(
            case when type = 'DIRECTORY' then name || '/' else name end,
            '' order by depth desc
        ) as full_path
        from path_parts
        """)
    String computeFullPath(@Param("resourceId") UUID resourceId);

    @Query("select r from Resource r where r.owner = :owner and lower(r.name) like lower(concat('%', :searchTerm, '%'))")
    List<Resource> searchByName(@Param("owner") User owner, @Param("searchTerm") String searchTerm);

    long countByOwner(User owner);

    @Query("select sum(r.size) from Resource r where r.owner = :owner and r.type = 'FILE'")
    Long sumFileSizeByOwner(@Param("owner") User owner);
}
