package com.czetsuyatech.audit.infrastructure.envers.repository;

import com.czetsuyatech.audit.infrastructure.envers.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

}
