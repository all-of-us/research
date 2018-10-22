package org.pmiops.workbench.api;

import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DomainLookupService {

  private static final List<String> TYPES =
    Arrays.asList(TreeType.ICD9.name(),
    TreeType.ICD10.name(),
    TreeType.CONDITION.name(),
    TreeType.PROCEDURE.name());

    private CriteriaDao criteriaDao;

    @Autowired
    public DomainLookupService(CriteriaDao criteriaDao) {
        this.criteriaDao = criteriaDao;
    }

    /**
     * Find all domain ids for {@link SearchGroup}s in the following groups:
     * ICD9, ICD10, CONDITION and PROCEDURE.
     *
     * @param searchGroups
     */
    public void findCodesForEmptyDomains(List<SearchGroup> searchGroups) {
      searchGroups.stream()
        .flatMap(searchGroup -> searchGroup.getItems().stream())
        .filter(item -> item.getType().matches(String.join("|", TYPES)))
        .forEach(item -> {
          List<SearchParameter> paramsWithDomains = new ArrayList<>();
          for (SearchParameter parameter : item.getSearchParameters()) {
            if (parameter.getGroup() &&
              (parameter.getDomainId() == null || parameter.getDomainId().isEmpty())) {
              List<String> domainLookups =
                criteriaDao.findCriteriaByTypeAndSubtypeAndCode(
                  parameter.getType(),
                  parameter.getSubtype(),
                  parameter.getValue());
              if (domainLookups.isEmpty()) {
                throw new NotFoundException("Not Found: No domain found for criteria type: " +
                  parameter.getType() + ", subtype: " + parameter.getSubtype() + " and code: " +
                  parameter.getValue());
              }

              for (String row : domainLookups) {
                paramsWithDomains.add(new SearchParameter()
                  .domainId(row)
                  .value(parameter.getValue())
                  .type(parameter.getType())
                  .subtype(parameter.getSubtype())
                  .group(parameter.getGroup())
                  .conceptId(parameter.getConceptId()));
              }
            } else {
              paramsWithDomains.add(parameter);
            }
          }
          item.setSearchParameters(paramsWithDomains);
        });
    }
}
