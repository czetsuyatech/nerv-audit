package com.czetsuyatech.nerv.audit.infrastructure.envers.repository;

import com.czetsuyatech.nerv.audit.infrastructure.envers.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
/**
 * Defines the UserRepository contract.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.addresses WHERE u.id = :id")
  Optional<UserEntity> findWithAddressesById(@Param("id") Long id);
}
