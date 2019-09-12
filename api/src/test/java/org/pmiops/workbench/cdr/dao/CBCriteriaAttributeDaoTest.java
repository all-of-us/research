package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.CBCriteriaAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CBCriteriaAttributeDaoTest {

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Test
  public void findCriteriaAttributeByConceptId() throws Exception {
    CBCriteriaAttribute attribute =
        new CBCriteriaAttribute()
            .conceptId(1L)
            .conceptName("test")
            .estCount("10")
            .type("type")
            .valueAsConceptId(12345678L);
    cbCriteriaAttributeDao.save(attribute);
    List<CBCriteriaAttribute> attributes =
        cbCriteriaAttributeDao.findCriteriaAttributeByConceptId(1L);
    assertEquals(1, attributes.size());
    assertEquals(attribute, attributes.get(0));
  }
}
