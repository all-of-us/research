package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.Institution;

public interface InstitutionService {
  List<Institution> getInstitutions();

  Optional<Institution> getInstitution(final String shortName);

  Optional<DbInstitution> getDbInstitution(final String shortName);

  Institution createInstitution(final Institution institutionToCreate);

  enum DeletionResult {
    SUCCESS,
    NOT_FOUND,
    HAS_VERIFIED_AFFILIATIONS
  }

  DeletionResult deleteInstitution(final String shortName);

  Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate);

  /**
   * Validates that the user's institutional affiliation is valid, by pattern-matching the user's
   * contact email against the institution's set of whitelisted email domains or addresses.
   *
   * @param verifiedInstitutionalAffiliation the user's declared affiliation
   * @param contactEmail the contact email to verify
   * @return boolean - does the affiliation pass validation?
   */
  boolean validate(
      DbVerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation, String contactEmail);
}
