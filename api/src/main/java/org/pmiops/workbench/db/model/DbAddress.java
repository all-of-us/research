package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "address")
public class DbAddress {
  private long id;
  private String streetAddress1;
  private String streetAddress2;
  private String zipCode;
  private String city;
  private String state;
  private String country;
  private DbUser user;

  public DbAddress() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "street_address_1")
  public String getStreetAddress1() {
    return streetAddress1;
  }

  public void setStreetAddress1(String streetAddress1) {
    this.streetAddress1 = streetAddress1;
  }

  @Column(name = "street_address_2")
  public String getStreetAddress2() {
    return streetAddress2;
  }

  public void setStreetAddress2(String streetAddress2) {
    this.streetAddress2 = streetAddress2;
  }

  // Most @Column annotations in our codebase don't have a length specification. This is included
  // on the zip_code field to allow test cases (where an in-memory H2 database is used instead of
  // MySQL) to trigger an exception when a user attempts to save a DbUser row with too-large field
  // payloads. See ProfileControllerTest for the test case, and the Liquibase changelogs for the
  // SQL definition of this field.
  @Column(name = "zip_code", length = 10)
  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }

  @Column(name = "city")
  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  @Column(name = "state")
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Column(name = "country")
  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public void setUser(DbUser user) {
    this.user = user;
  }
}
