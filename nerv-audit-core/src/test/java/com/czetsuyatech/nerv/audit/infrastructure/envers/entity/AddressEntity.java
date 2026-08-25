package com.czetsuyatech.nerv.audit.infrastructure.envers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;

/**
 * Test entity representing a user address.
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@Entity
@Audited
@Table(name = "user_address")
@SequenceGenerator(name = "address_seq", sequenceName = "address_seq", allocationSize = 1)
public class AddressEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_seq")
  private Long id;

  /**
   * Street name.
   */
  @Column(name = "street")
  private String street;

  /**
   * City name.
   */
  @Column(name = "city")
  private String city;

  /**
   * Country name.
   */
  @Column(name = "country")
  private String country;

  /**
   * User owner of the address.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private UserEntity user;
}
