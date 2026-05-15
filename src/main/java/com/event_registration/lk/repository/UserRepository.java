package com.event_registration.lk.repository;

import com.event_registration.lk.entity.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity,Long> {
    public Boolean existsUserEntityByEmailContainingIgnoreCase(String email);
    public boolean existsByUserId(Long userId);
    boolean deleteByUserId(Long userId);

    Optional<UserEntity> findUserEntityByEmailContainingIgnoreCase(String email);
    UserEntity findUserEntityByEmailIgnoreCase(String email);
    Optional<UserEntity> findByUsernameIgnoreCase(String username);
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailContainingIgnoreCase(String email);

    Optional<UserEntity> findByVerificationToken(String verificationToken);

}
