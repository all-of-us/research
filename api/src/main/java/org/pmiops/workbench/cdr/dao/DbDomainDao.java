package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DbDomainDao extends CrudRepository<DbDomain, Long> {


}
