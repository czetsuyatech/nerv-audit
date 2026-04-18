package com.czetsuyatech.audit.infrastructure.envers.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

/**
 * Test entity representing a user account.
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@Entity
@Audited
@Table(name = "user_account")
@SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
public class UserEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
  private Long id;

  /**
   * User first name.
   */
  @Column(name = "first_name")
  private String firstName;

  /**
   * User last name.
   */
  @Column(name = "last_name")
  private String lastName;

  /**
   * User birth date.
   */
  @Column(name = "birth_date")
  private LocalDateTime birthDate;

  /**
   * Collection of user hobbies.
   */
  @ElementCollection
  @CollectionTable(
      name = "user_hobby",
      joinColumns = @JoinColumn(name = "user_id")
  )
  @Column(name = "hobby")
  private List<String> hobbies;
}
