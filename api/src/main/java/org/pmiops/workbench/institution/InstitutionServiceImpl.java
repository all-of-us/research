package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.InstitutionUserInstructionsDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private static final Logger log = Logger.getLogger(InstitutionServiceImpl.class.getName());

  private final InstitutionDao institutionDao;
  private final InstitutionUserInstructionsDao institutionUserInstructionsDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final InstitutionMapper institutionMapper;
  private final InstitutionUserInstructionsMapper institutionUserInstructionsMapper;
  private final PublicInstitutionDetailsMapper publicInstitutionDetailsMapper;
  private final String OPERATIONAL_USER_INSTITUTION_SHORT_NAME = "AouOps";

  @Autowired
  InstitutionServiceImpl(
      InstitutionDao institutionDao,
      InstitutionUserInstructionsDao institutionUserInstructionsDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      InstitutionMapper institutionMapper,
      InstitutionUserInstructionsMapper institutionUserInstructionsMapper,
      PublicInstitutionDetailsMapper publicInstitutionDetailsMapper) {
    this.institutionDao = institutionDao;
    this.institutionUserInstructionsDao = institutionUserInstructionsDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.institutionMapper = institutionMapper;
    this.institutionUserInstructionsMapper = institutionUserInstructionsMapper;
    this.publicInstitutionDetailsMapper = publicInstitutionDetailsMapper;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(institutionMapper::dbToModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<PublicInstitutionDetails> getPublicInstitutionDetails() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(publicInstitutionDetailsMapper::dbToModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Institution> getInstitution(final String shortName) {
    return getDbInstitution(shortName).map(institutionMapper::dbToModel);
  }

  @Override
  public DbInstitution getDbInstitutionOrThrow(final String shortName) {
    return getDbInstitution(shortName)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Could not find Institution '%s'", shortName)));
  }

  private Optional<DbInstitution> getDbInstitution(final String shortName) {
    return institutionDao.findOneByShortName(shortName);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return institutionMapper.dbToModel(
        institutionDao.save(institutionMapper.modelToDb(institutionToCreate)));
  }

  @Override
  public void deleteInstitution(final String shortName) {
    final DbInstitution institution = getDbInstitutionOrThrow(shortName);
    if (verifiedInstitutionalAffiliationDao.findAllByInstitution(institution).isEmpty()) {
      // no verified user affiliations: safe to delete
      institutionDao.delete(institution);
    } else {
      throw new ConflictException(
          String.format(
              "Could not delete Institution '%s' because it has verified user affiliations",
              shortName));
    }
  }

  @Override
  @Transactional
  public Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate) {
    final DbInstitution updateSource = institutionMapper.modelToDb(institutionToUpdate);
    return getDbInstitution(shortName)
        .map(
            dbInstitution -> {
              System.out.println("dbInstitution pre set");

              // TODO mapper!
              dbInstitution
                  .setShortName(updateSource.getShortName())
                  .setDisplayName(updateSource.getDisplayName())
                  .setOrganizationTypeEnum(updateSource.getOrganizationTypeEnum())
                  .setOrganizationTypeOtherText(updateSource.getOrganizationTypeOtherText())
                  .setDuaTypeEnum(updateSource.getDuaTypeEnum())
                  .setEmailDomains(updateSource.getEmailDomains())
                  .setEmailAddresses(updateSource.getEmailAddresses());

              System.out.println("dbInstitution post set, pre save");

              dbInstitution = institutionDao.save(dbInstitution);

              System.out.println("dbInstitution post save");

              return institutionMapper.dbToModel(dbInstitution);
            });
  }

  @Override
  public boolean validateAffiliation(
      @Nullable DbVerifiedInstitutionalAffiliation dbAffiliation, String contactEmail) {
    if (dbAffiliation == null) {
      return false;
    }
    return validateInstitutionalEmail(
        institutionMapper.dbToModel(dbAffiliation.getInstitution()), contactEmail);
  }

  @Override
  public boolean validateInstitutionalEmail(Institution institution, String contactEmail) {
    try {
      // TODO RW-4489: UserService should handle initial email validation
      new InternetAddress(contactEmail).validate();
    } catch (AddressException e) {
      log.info(
          String.format(
              "Contact email '%s' validation threw an AddressException: %s",
              contactEmail, e.getMessage()));
      return false;
    } catch (NullPointerException e) {
      log.info(
          String.format(
              "Contact email '%s' validation threw a NullPointerException", contactEmail));
      return false;
    }

    // If the Institution has DUA Agreement that is restricted just to few researchers
    // Confirm if the email address is in the allowed email list
    if (institution.getDuaTypeEnum() != null
        && institution.getDuaTypeEnum().equals(DuaType.RESTRICTED)) {
      final boolean validated = institution.getEmailAddresses().contains(contactEmail);
      log.info(
          String.format(
              "Contact email '%s' validated against RESTRICTED-DUA institution '%s': address %s",
              contactEmail, institution.getShortName(), validated ? "MATCHED" : "DID NOT MATCH"));
      return validated;
    }

    // If Agreement Type is NULL assume DUA Agreement is MASTER
    // If Institution agreement type is master confirm if the contact email has valid/allowed domain
    final String contactEmailDomain = contactEmail.substring(contactEmail.indexOf("@") + 1);
    final boolean validated = institution.getEmailDomains().contains(contactEmailDomain);
    log.info(
        String.format(
            "Contact email '%s' validated against MASTER-DUA institution '%s': domain %s %s",
            contactEmail,
            institution.getShortName(),
            contactEmailDomain,
            validated ? "MATCHED" : "DID NOT MATCH"));
    return validated;
  }

  @Override
  public Optional<String> getInstitutionUserInstructions(final String shortName) {
    return institutionUserInstructionsDao
        .getByInstitutionId(getDbInstitutionOrThrow(shortName).getInstitutionId())
        .map(DbInstitutionUserInstructions::getUserInstructions);
  }

  @Override
  public void setInstitutionUserInstructions(final InstitutionUserInstructions userInstructions) {

    final DbInstitutionUserInstructions dbInstructions =
        institutionUserInstructionsMapper.modelToDb(userInstructions, this);

    // if a DbInstitutionUserInstructions entry already exists for this Institution, retrieve its ID
    // so the call to save() replaces it

    institutionUserInstructionsDao
        .getByInstitutionId(dbInstructions.getInstitutionId())
        .ifPresent(
            existingDbEntry ->
                dbInstructions.setInstitutionUserInstructionsId(
                    existingDbEntry.getInstitutionUserInstructionsId()));

    institutionUserInstructionsDao.save(dbInstructions);
  }

  @Override
  @Transactional // TODO: understand why this is necessary
  public boolean deleteInstitutionUserInstructions(final String shortName) {
    final DbInstitution institution = getDbInstitutionOrThrow(shortName);
    return institutionUserInstructionsDao.deleteByInstitutionId(institution.getInstitutionId()) > 0;
  }

  @Override
  public boolean validateOperationalUser(DbInstitution institution) {
    return institution != null
        && institution.getShortName().equals(OPERATIONAL_USER_INSTITUTION_SHORT_NAME);
  }
}
