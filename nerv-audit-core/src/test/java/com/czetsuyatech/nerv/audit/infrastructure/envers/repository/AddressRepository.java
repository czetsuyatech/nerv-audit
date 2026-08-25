package com.czetsuyatech.nerv.audit.infrastructure.envers.repository;

import com.czetsuyatech.nerv.audit.infrastructure.envers.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
/**
 * Defines the AddressRepository contract.
 */
public interface AddressRepository extends JpaRepository<AddressEntity, Long> {

}
