package org.pmiops.workbench.db.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "page_visit")
public class PageVisit {

  private long pageVisitId;
  private String pageId;
  private Timestamp firstVisit;
  private Timestamp lastVisit;
  private User user;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "page_visit_id")
  public long getPageVisitId() {
    return pageVisitId;
  }

  public void setPageVisitId(long pageVisitId) {
    this.pageVisitId = pageVisitId;
  }

  @ManyToOne
  @JoinColumn(name="user_id")
  public User getUser() { return user; }

  public void setUser(User user) { this.user = user; }

  @Column(name = "page_id")
  public String getPageId() { return pageId; }

  public void setPageId(String pageId) { this.pageId = pageId; }

  @Column(name = "first_visit")
  public Timestamp getFirstVisit() { return firstVisit; }

  public void setFirstVisit(Timestamp firstVisit) { this.firstVisit = firstVisit; }

  @Column(name = "last_visit")
  public Timestamp getLastVisit() { return lastVisit; }

  public void setLastVisit(Timestamp lastVisit) { this.lastVisit = lastVisit; }
}
