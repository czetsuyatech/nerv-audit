package com.czetsuyatech.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.czetsuyatech.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.application.query.AuditQuery;
import com.czetsuyatech.audit.persistence.repository.AuditRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

  @Mock
  private AuditRepository auditRepository;

  @Mock
  private EntityManager entityManager;

  private AuditServiceImpl auditService;

  @BeforeEach
  void setUp() {
    auditService = new AuditServiceImpl(auditRepository, entityManager);
  }

  @Test
  void getVerticalAuditsNameOnQueryAndDelegatesToRepository() {

    PageResult<VerticalAuditDTO> expected = new PageResult<>(List.of(), 0, 0, 10);
    when(auditRepository.findAuditsByQuery(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    AuditQuery query = AuditQuery.builder().build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits("UserEntity", query);

    ArgumentCaptor<AuditQuery> captor = forClass(AuditQuery.class);
    verify(auditRepository).findAuditsByQuery(captor.capture());
    assertThat(captor.getValue().getEntities())
        .containsExactly("UserEntity");
    assertThat(result)
        .isSameAs(expected);
  }

  @Test
  void getVerticalAudits() {

    PageResult<VerticalAuditDTO> expected = new PageResult<>(List.of(), 0, 0, 10);
    when(auditRepository.findAuditsByQuery(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    AuditQuery query = AuditQuery.builder()
        .entities(List.of("OtherEntity", "AnotherEntity"))
        .build();
    auditService.getVerticalAudits("TargetEntity", query);

    ArgumentCaptor<AuditQuery> captor = forClass(AuditQuery.class);
    verify(auditRepository).findAuditsByQuery(captor.capture());
    assertThat(captor.getValue().getEntities())
        .hasSize(1)
        .containsExactly("TargetEntity");
  }

  @Test
  void getVerticalAudits_delegatesDirectlyToRepository() {

    PageResult<VerticalAuditDTO> expected = new PageResult<>(List.of(), 5, 0, 20);
    when(auditRepository.findAuditsByQuery(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    AuditQuery query = AuditQuery.builder()
        .updatedBy("user1")
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    verify(auditRepository).findAuditsByQuery(query);
    assertThat(result)
        .isSameAs(expected);
  }

  @Test
  void getVerticalAudits_withPopulatedResults_returnsAllResults() {

    List<VerticalAuditDTO> dtos = List.of(
        VerticalAuditDTO.builder()
            .id(1L)
            .fieldName("firstName")
            .newValue("John")
            .build(),
        VerticalAuditDTO.builder()
            .id(2L)
            .fieldName("lastName")
            .newValue("Doe")
            .build()
    );
    PageResult<VerticalAuditDTO> expected = new PageResult<>(dtos, 2, 0, 10);
    when(auditRepository.findAuditsByQuery(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(AuditQuery.builder().build());

    assertThat(result.getContent())
        .hasSize(2);
    assertThat(result.getTotal())
        .isEqualTo(2);
  }

  @Test
  void getVerticalAuditsName() {

    AuditQuery query = AuditQuery.builder().build();
    PageResult<VerticalAuditDTO> expected = new PageResult<>(List.of(), 0, 0, 10);
    when(auditRepository.findAuditsByQuery(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    auditService.getVerticalAudits("SomeEntity", query);

    ArgumentCaptor<AuditQuery> captor = forClass(AuditQuery.class);
    verify(auditRepository).findAuditsByQuery(captor.capture());
    assertThat(captor.getValue().getEntities())
        .containsExactly("SomeEntity");
  }

  @Test
  void getVerticalAudits_withPagination_passesQueryUnmodified() {

    PageResult<VerticalAuditDTO> expected = new PageResult<>(List.of(), 100, 10, 20);
    when(auditRepository.findAuditsByQuery(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

    AuditQuery query = AuditQuery.builder()
        .offset(10)
        .limit(20)
        .build();
    PageResult<VerticalAuditDTO> result = auditService.getVerticalAudits(query);

    verify(auditRepository).findAuditsByQuery(query);
    assertThat(result.getOffset())
        .isEqualTo(10);
    assertThat(result.getLimit())
        .isEqualTo(20);
    assertThat(result.getTotal())
        .isEqualTo(100);
  }

  @Test
  void getHorizontalAudits_unknownEntity_throwsIllegalArgument() {

    Metamodel metamodel = mock(Metamodel.class);
    when(entityManager.getMetamodel()).thenReturn(metamodel);
    when(metamodel.getEntities()).thenReturn(Set.of());

    assertThatThrownBy(() -> auditService.getHorizontalAudits("Unknown", AuditQuery.builder().build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown");
  }

  @Test
  @SuppressWarnings("unchecked")
  void getHorizontalAudits_nullLimit_usesDefaultOfTen() {

    setupMetamodelWithEntity(String.class);

    try (MockedStatic<AuditReaderFactory> factory = mockStatic(AuditReaderFactory.class)) {
      org.hibernate.envers.query.AuditQuery enversQuery = stubAuditReaderChain(factory, List.of());

      auditService.getHorizontalAudits("String", AuditQuery.builder().build());

      verify(enversQuery).setMaxResults(10);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void getHorizontalAudits_configuredLimit_usesProvidedLimit() {

    setupMetamodelWithEntity(String.class);

    try (MockedStatic<AuditReaderFactory> factory = mockStatic(AuditReaderFactory.class)) {
      org.hibernate.envers.query.AuditQuery enversQuery = stubAuditReaderChain(factory, List.of());

      auditService.getHorizontalAudits("String", AuditQuery.builder().limit(5).build());

      verify(enversQuery).setMaxResults(5);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void getHorizontalAudits_mapsRevisionDataToDTOs() {

    setupMetamodelWithEntity(String.class);

    String entity = "testValue";
    Instant revisionDate = Instant.parse("2024-01-15T10:00:00Z");

    DefaultRevisionEntity revEntity = mock(DefaultRevisionEntity.class);
    when(revEntity.getId()).thenReturn(42);
    when(revEntity.getRevisionDate()).thenReturn(Date.from(revisionDate));

    List<Object[]> rows = List.<Object[]>of(new Object[]{entity, revEntity});

    try (MockedStatic<AuditReaderFactory> factory = mockStatic(AuditReaderFactory.class)) {
      stubAuditReaderChain(factory, rows);

      List<HorizontalAuditDTO<String>> result =
          auditService.getHorizontalAudits("String", AuditQuery.builder().build());

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getEntity()).isEqualTo(entity);
      assertThat(result.get(0).getRevision()).isEqualTo(42);
      assertThat(result.get(0).getRevisionDate()).isEqualTo(revisionDate);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void setupMetamodelWithEntity(Class<T> entityClass) {

    EntityType<T> entityType = mock(EntityType.class);
    Metamodel metamodel = mock(Metamodel.class);
    when(entityManager.getMetamodel()).thenReturn(metamodel);
    when(metamodel.getEntities()).thenReturn((Set) Set.of(entityType));
    when(entityType.getJavaType()).thenReturn(entityClass);
  }

  private org.hibernate.envers.query.AuditQuery stubAuditReaderChain(
      MockedStatic<AuditReaderFactory> factory, List<Object[]> results) {

    AuditReader reader = mock(AuditReader.class);
    AuditQueryCreator creator = mock(AuditQueryCreator.class);
    org.hibernate.envers.query.AuditQuery enversQuery = mock(org.hibernate.envers.query.AuditQuery.class);

    factory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(reader);
    when(reader.createQuery()).thenReturn(creator);
    when(creator.forRevisionsOfEntity(any(), eq(false), eq(true))).thenReturn(enversQuery);
    when(enversQuery.setMaxResults(anyInt())).thenReturn(enversQuery);
    when(enversQuery.addOrder(any())).thenReturn(enversQuery);
    when(enversQuery.getResultList()).thenReturn((List) results);

    return enversQuery;
  }
}
