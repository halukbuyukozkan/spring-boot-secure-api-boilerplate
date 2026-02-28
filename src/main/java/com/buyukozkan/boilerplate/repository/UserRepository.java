package com.buyukozkan.boilerplate.repository;

import com.buyukozkan.boilerplate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.email = :email
            """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    boolean existsByEmail(String email);
}
