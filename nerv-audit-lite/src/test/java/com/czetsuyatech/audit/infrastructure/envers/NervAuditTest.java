package com.czetsuyatech.audit.infrastructure.envers;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.application.query.AuditQuery;
import com.czetsuyatech.audit.config.TestAuditConfiguration;
import com.czetsuyatech.audit.infrastructure.envers.entity.UserEntity;
import com.czetsuyatech.audit.infrastructure.envers.repository.UserRepository;
import com.czetsuyatech.audit.service.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the NERV audit infrastructure.
 *
 * <p>Uses @SpringBootTest to load the full application context from TestApplication,
 * avoiding @AutoConfigureTestDatabase interference so our H2 URL settings apply.</p>
 */
@SpringBootTest
@ActiveProfiles("tst")
@Transactional
@Import(TestAuditConfiguration.class)
class NervAuditTest {

  @Autowired
  private UserRepository userRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private AuditService auditService;

  // ---- CRUD sanity tests ----

  @Test
  void save_persistsUserEntity() {

    UserEntity user = UserEntity.builder()
        .firstName("John")
        .lastName("Doe")
        .birthDate(LocalDateTime.of(1990, 1, 15, 0, 0))
        .build();

    UserEntity saved = userRepository.save(user);
    entityManager.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(userRepository.findById(saved.getId())).isPresent();
  }

  @Test
  void findById_returnsPersistedEntity() {

    UserEntity user = UserEntity.builder()
        .firstName("Alice")
        .lastName("Wonder")
        .build();
    user = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    assertThat(userRepository.findById(user.getId()))
        .isPresent()
        .get()
        .satisfies(u -> {
          assertThat(u.getFirstName()).isEqualTo("Alice");
          assertThat(u.getLastName()).isEqualTo("Wonder");
        });
  }

  @Test
  void update_changesEntityFields() {

    UserEntity user = UserEntity.builder()
        .firstName("Bob")
        .lastName("Builder")
        .build();
    user = userRepository.save(user);
    entityManager.flush();

    user.setFirstName("Robert");
    userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    assertThat(userRepository.findById(user.getId()))
        .isPresent()
        .get()
        .satisfies(u -> assertThat(u.getFirstName()).isEqualTo("Robert"));
  }

  @Test
  void delete_removesEntityFromDatabase() {

    UserEntity user = UserEntity.builder()
        .firstName("Charlie")
        .lastName("Chaplin")
        .build();
    user = userRepository.save(user);
    entityManager.flush();
    Long id = user.getId();

    userRepository.deleteById(id);
    entityManager.flush();

    assertThat(userRepository.findById(id)).isEmpty();
  }

  @Test
  void saveWithHobbies_persistsElementCollection() {

    UserEntity user = UserEntity.builder()
        .firstName("Dave")
        .lastName("Explorer")
        .hobbies(List.of("hiking", "reading", "coding"))
        .build();

    user = userRepository.save(user);
    entityManager.flush();
    entityManager.clear();

    assertThat(userRepository.findById(user.getId()))
        .isPresent()
        .get()
        .satisfies(u -> assertThat(u.getHobbies())
            .containsExactlyInAnyOrder("hiking", "reading", "coding"));
  }

  @Test
  void findAll_returnsAllPersistedEntities() {

    userRepository.save(UserEntity.builder().firstName("User1").lastName("Last1").build());
    userRepository.save(UserEntity.builder().firstName("User2").lastName("Last2").build());
    entityManager.flush();

    assertThat(userRepository.findAll())
        .hasSizeGreaterThanOrEqualTo(2);
  }

  // ---- Audit query tests (against pre-populated audit table) ----

  @Test
  void auditService_searchWithNoEntities_returnsEmptyResult() {

    AuditQuery query = AuditQuery.builder().build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isNotNull();
  }

  @Test
  void auditService_searchEntity_withPrePopulatedData_returnsAuditRecords() {

    entityManager.createNativeQuery(
        "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
            + "VALUES (1, 1, 1, 'testUser', CURRENT_TIMESTAMP, 'FIRSTNAME', 'John', 'Jane')"
    ).executeUpdate();
    entityManager.flush();

    AuditQuery query = AuditQuery.builder()
        .entities(List.of("UserEntity"))
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFieldName())
        .isEqualTo("FIRSTNAME");
    assertThat(result.getContent().get(0).getOldValue())
        .isEqualTo("John");
    assertThat(result.getContent().get(0).getNewValue())
        .isEqualTo("Jane");
    assertThat(result.getContent().get(0).getUpdatedBy())
        .isEqualTo("testUser");
  }

  @Test
  void auditService_searchWithFieldNameFilter_returnsMatchingRecords() {

    entityManager.createNativeQuery(
        "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
            + "VALUES (20, 20, 1, 'admin', CURRENT_TIMESTAMP, 'LASTNAME', 'Doe', 'Smith')"
    ).executeUpdate();
    entityManager.createNativeQuery(
        "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
            + "VALUES (20, 21, 1, 'admin', CURRENT_TIMESTAMP, 'FIRSTNAME', 'John', 'Jane')"
    ).executeUpdate();
    entityManager.flush();

    AuditQuery query = AuditQuery.builder()
        .entities(List.of("UserEntity"))
        .fieldName("LASTNAME")
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFieldName())
        .isEqualTo("LASTNAME");
  }

  @Test
  void auditService_searchWithUpdatedByFilter_returnsMatchingRecords() {

    entityManager.createNativeQuery(
        "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
            + "VALUES (30, 30, 1, 'alice', CURRENT_TIMESTAMP, 'FIRSTNAME', 'Old', 'New')"
    ).executeUpdate();
    entityManager.createNativeQuery(
        "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
            + "VALUES (31, 31, 1, 'bob', CURRENT_TIMESTAMP, 'FIRSTNAME', 'Old2', 'New2')"
    ).executeUpdate();
    entityManager.flush();

    AuditQuery query = AuditQuery.builder()
        .entities(List.of("UserEntity"))
        .updatedBy("alice")
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUpdatedBy())
        .isEqualTo("alice");
  }

  @Test
  void auditService_searchWithPagination_respectsLimitAndOffset() {

    for (int i = 100; i < 105; i++) {
      entityManager.createNativeQuery(
          "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
              + "VALUES (" + i + ", " + i + ", 1, 'system', CURRENT_TIMESTAMP, 'FIELD" + i + "', 'old', 'new')"
      ).executeUpdate();
    }
    entityManager.flush();

    AuditQuery query = AuditQuery.builder()
        .entities(List.of("UserEntity"))
        .offset(0)
        .limit(2)
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    assertThat(result.getContent()).hasSizeLessThanOrEqualTo(2);
    assertThat(result.getTotal()).isGreaterThanOrEqualTo(5);
    assertThat(result.getLimit()).isEqualTo(2);
  }

  @Test
  void auditService_searchWithNewValueFilter_returnsMatchingRecords() {

    entityManager.createNativeQuery(
        "INSERT INTO user_account_aud (id, rev, revtype, updated_by, updated, field_name, old_value, new_value) "
            + "VALUES (50, 50, 1, 'user', CURRENT_TIMESTAMP, 'STATUS', 'INACTIVE', 'ACTIVE')"
    ).executeUpdate();
    entityManager.flush();

    AuditQuery query = AuditQuery.builder()
        .entities(List.of("UserEntity"))
        .newValue("ACTIVE")
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getNewValue())
        .isEqualTo("ACTIVE");
    assertThat(result.getContent().get(0).getOldValue())
        .isEqualTo("INACTIVE");
  }
}
