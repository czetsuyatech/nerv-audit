package com.czetsuyatech.nerv.audit.infrastructure.envers;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.audit.config.TestAuditConfiguration;
import com.czetsuyatech.nerv.audit.infrastructure.envers.entity.UserEntity;
import com.czetsuyatech.nerv.audit.infrastructure.envers.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
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
@Import(TestAuditConfiguration.class)
/**
 * Tests the behavior of NervAuditCollection.
 */
class NervAuditCollectionTest {

  @Autowired
  private UserRepository userRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  @Transactional
  void updateHobbies_shouldGenerateAuditEntries() {

    // 1. Create user with initial hobbies
    UserEntity user = UserEntity.builder()
        .firstName("Hobby")
        .lastName("User")
        .hobbies(new ArrayList<>(List.of("reading")))
        .build();

    user = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    // 2. Update hobbies (this should trigger recreation/update)
    user = userRepository.findById(user.getId()).orElseThrow();
    user.getHobbies().clear();
    user.getHobbies().add("coding");
    user.getHobbies().add("gaming");
    userRepository.save(user);

    entityManager.flush();
    entityManager.clear();

    TestTransaction.flagForCommit();
    TestTransaction.end();

    // 3. Check audit table using native query
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager.createNativeQuery(
            """
                        SELECT id, rev, revtype, updated_by, updated, field_name, old_value, new_value 
                        FROM user_hobby_aud 
                        WHERE UPPER(field_name) = 'ELEMENT'
                """)
        .getResultList();

    // If it's recreation, we expect ADD entries for new hobbies
    // and potentially DEL entries for old ones if it was an update.
    assertThat(rows)
        .as("Audit entries should be generated for hobbies update")
        .isNotEmpty();
  }

  @Test
  @Transactional
  void removeHobbies_shouldGenerateAuditEntries() {

    // 1. Create user with initial hobbies
    UserEntity user = UserEntity.builder()
        .firstName("Remove")
        .lastName("Hobby")
        .hobbies(new ArrayList<>(List.of("swimming", "running")))
        .build();

    user = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    // 2. Remove hobbies (clear collection)
    user = userRepository.findById(user.getId()).orElseThrow();
    user.getHobbies().clear();
    userRepository.save(user);

    entityManager.flush();
    entityManager.clear();

    TestTransaction.flagForCommit();
    TestTransaction.end(); // 🔥 THIS triggers Envers

    // 3. Check audit table using native query
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager.createNativeQuery(
            """
                        SELECT id, rev, revtype, updated_by, updated, field_name, old_value, new_value 
                        FROM user_hobby_aud 
                        WHERE UPPER(field_name) = 'ELEMENT'
                """)
        .getResultList();

    assertThat(rows)
        .as("Audit entries should be generated for hobbies removal")
        .isNotEmpty();
  }
}
