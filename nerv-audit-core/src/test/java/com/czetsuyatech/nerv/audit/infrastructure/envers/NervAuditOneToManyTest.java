package com.czetsuyatech.nerv.audit.infrastructure.envers;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.audit.config.TestAuditConfiguration;
import com.czetsuyatech.nerv.audit.infrastructure.envers.entity.AddressEntity;
import com.czetsuyatech.nerv.audit.infrastructure.envers.entity.UserEntity;
import com.czetsuyatech.nerv.audit.infrastructure.envers.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("tst")
@Transactional
@Import(TestAuditConfiguration.class)
/**
 * Tests the behavior of NervAuditOneToMany.
 */
class NervAuditOneToManyTest {

  @Autowired
  private UserRepository userRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  void updateAddresses_shouldGenerateAuditEntries() {

    // 1. Create user with initial address
    UserEntity user = UserEntity.builder()
        .firstName("Address")
        .lastName("User")
        .build();

    AddressEntity address1 = AddressEntity.builder()
        .street("Street 1")
        .city("City 1")
        .country("Country 1")
        .user(user)
        .build();

    user.getAddresses().add(address1);

    user = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    // 2. Update address
    user = userRepository.findById(user.getId()).orElseThrow();
    user.getAddresses().get(0).setStreet("Updated Street 1");
    userRepository.save(user);

    entityManager.flush();
    entityManager.clear();

    TestTransaction.flagForCommit();
    TestTransaction.end();

    // 3. Check audit table for address update
    TestTransaction.start();
    @SuppressWarnings("unchecked")
    List<Object[]> addressAuditRows = entityManager.createNativeQuery(
            "SELECT id, rev, revtype, field_name, old_value, new_value FROM user_address_aud WHERE UPPER(field_name) = 'STREET'")
        .getResultList();
    TestTransaction.flagForRollback();
    TestTransaction.end();

    assertThat(addressAuditRows)
        .as("Audit entries should be generated for address street update")
        .isNotEmpty();

    // 4. Add new address
    TestTransaction.start();
    user = userRepository.findWithAddressesById(user.getId()).orElseThrow();
    AddressEntity address2 = AddressEntity.builder()
        .street("Street 2")
        .city("City 2")
        .country("Country 2")
        .user(user)
        .build();
    user.getAddresses().add(address2);
    userRepository.save(user);
    TestTransaction.flagForCommit();
    TestTransaction.end();

    // 5. Remove an address
    TestTransaction.start();
    user = userRepository.findWithAddressesById(user.getId()).orElseThrow();
    user.getAddresses().remove(0);
    userRepository.save(user);
    TestTransaction.flagForCommit();
    TestTransaction.end();

    // Check audit entries for all address operations
    TestTransaction.start();
    @SuppressWarnings("unchecked")
    List<Object[]> allAddressAuditRows = entityManager.createNativeQuery(
            "SELECT id, rev, revtype FROM user_address_aud")
        .getResultList();
    TestTransaction.flagForRollback();
    TestTransaction.end();

    assertThat(allAddressAuditRows)
        .as("Audit entries should be generated for address operations")
        .isNotEmpty();
  }
}
