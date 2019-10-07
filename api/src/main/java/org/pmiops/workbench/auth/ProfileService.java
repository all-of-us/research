package org.pmiops.workbench.auth;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DemographicSurveyEnum;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private static final Function<
          org.pmiops.workbench.db.model.InstitutionalAffiliation, InstitutionalAffiliation>
      TO_CLIENT_INSTITUTIONAL_AFFILIATION =
          new Function<
              org.pmiops.workbench.db.model.InstitutionalAffiliation, InstitutionalAffiliation>() {
            @Override
            public InstitutionalAffiliation apply(
                org.pmiops.workbench.db.model.InstitutionalAffiliation institutionalAffiliation) {
              InstitutionalAffiliation result = new InstitutionalAffiliation();
              if (institutionalAffiliation != null) {
                result.setRole(institutionalAffiliation.getRole());
                result.setInstitution(institutionalAffiliation.getInstitution());
                result.setNonAcademicAffiliation(institutionalAffiliation.getNonAcademicAffiliationEnum());
                result.setOther(institutionalAffiliation.getOther());
              }
              return result;
            }
          };

  private static final Function<org.pmiops.workbench.db.model.PageVisit, PageVisit>
      TO_CLIENT_PAGE_VISIT =
          new Function<org.pmiops.workbench.db.model.PageVisit, PageVisit>() {
            @Override
            public PageVisit apply(org.pmiops.workbench.db.model.PageVisit pageVisit) {
              PageVisit result = new PageVisit();
              result.setPage(pageVisit.getPageId());
              result.setFirstVisit(pageVisit.getFirstVisit().getTime());
              return result;
            }
          };

  private static final Function<org.pmiops.workbench.db.model.DemographicSurvey, DemographicSurvey>
      TO_CLIENT_DEMOGRAPHIC_SURVEY =
          new Function<org.pmiops.workbench.db.model.DemographicSurvey, DemographicSurvey>() {
            @Override
            public DemographicSurvey apply(
                org.pmiops.workbench.db.model.DemographicSurvey demographicSurvey) {
              DemographicSurvey result = new DemographicSurvey();
              if (result.getDisability() != null)
                result.setDisability(demographicSurvey.getDisabilityEnum().equals(Disability.TRUE));
              result.setEducation(demographicSurvey.getEducationEnum());
              result.setEthnicity(demographicSurvey.getEthnicityEnum());
              result.setGender(demographicSurvey.getGender().stream().map
                  ((gender) -> DemographicSurveyEnum.genderFromStorage(gender)).collect(Collectors.toList()));
              result.setRace(demographicSurvey.getRace().stream()
                  .map((race) -> DemographicSurveyEnum.raceFromStorage(race)).collect(Collectors.toList()));
              result.setYearOfBirth(BigDecimal.valueOf(demographicSurvey.getYear_of_birth()));

              return result;
            }
          };

  private static final Function<org.pmiops.workbench.db.model.Address, Address>
      TO_CLIENT_ADDRESS_SURVEY =
          new Function<org.pmiops.workbench.db.model.Address, Address>() {
            @Override
            public Address apply(org.pmiops.workbench.db.model.Address address) {
              Address result = new Address();
              if (address != null) {
                result.setStreetAddress1(address.getStreetAddress1());
                result.setStreetAddress2(address.getStreetAddress2());
                result.setCity(address.getCity());
                result.setState(address.getState());
                result.setCountry(address.getCountry());
                result.setZipCode(address.getZipCode());
                return result;
              }
              return result;
            }
          };

  private final UserDao userDao;

  @Autowired
  public ProfileService(UserDao userDao) {
    this.userDao = userDao;
  }

  public Profile getProfile(User user) {
    // Fetch the user's authorities, since they aren't loaded during normal request interception.
    User userWithAuthoritiesAndPageVisits =
        userDao.findUserWithAuthoritiesAndPageVisits(user.getUserId());
    if (userWithAuthoritiesAndPageVisits != null) {
      // If the user is already written to the database, use it and whatever authorities and page
      // visits are there.
      user = userWithAuthoritiesAndPageVisits;
    }

    Profile profile = new Profile();
    profile.setUserId(user.getUserId());
    profile.setUsername(user.getEmail());
    if (user.getCreationNonce() != null) {
      profile.setCreationNonce(user.getCreationNonce().toString());
    }
    profile.setFamilyName(user.getFamilyName());
    profile.setGivenName(user.getGivenName());
    profile.setOrganization(user.getOrganization());
    profile.setCurrentPosition(user.getCurrentPosition());
    profile.setContactEmail(user.getContactEmail());
    profile.setPhoneNumber(user.getPhoneNumber());
    profile.setAboutYou(user.getAboutYou());
    profile.setAreaOfResearch(user.getAreaOfResearch());
    profile.setDisabled(user.getDisabled());
    profile.setEraCommonsLinkedNihUsername(user.getEraCommonsLinkedNihUsername());

    if (user.getComplianceTrainingCompletionTime() != null) {
      profile.setComplianceTrainingCompletionTime(
          user.getComplianceTrainingCompletionTime().getTime());
    }
    if (user.getComplianceTrainingBypassTime() != null) {
      profile.setComplianceTrainingBypassTime(user.getComplianceTrainingBypassTime().getTime());
    }
    if (user.getEraCommonsLinkExpireTime() != null) {
      profile.setEraCommonsLinkExpireTime(user.getEraCommonsLinkExpireTime().getTime());
    }
    if (user.getEraCommonsCompletionTime() != null) {
      profile.setEraCommonsCompletionTime(user.getEraCommonsCompletionTime().getTime());
    }
    if (user.getEraCommonsBypassTime() != null) {
      profile.setEraCommonsBypassTime(user.getEraCommonsBypassTime().getTime());
    }
    if (user.getDemographicSurveyCompletionTime() != null) {
      profile.setDemographicSurveyCompletionTime(
          user.getDemographicSurveyCompletionTime().getTime());
    }
    if (user.getFirstSignInTime() != null) {
      profile.setFirstSignInTime(user.getFirstSignInTime().getTime());
    }
    if (user.getIdVerificationBypassTime() != null) {
      profile.setIdVerificationBypassTime(user.getIdVerificationBypassTime().getTime());
    }
    if (user.getIdVerificationCompletionTime() != null) {
      profile.setIdVerificationCompletionTime(user.getIdVerificationCompletionTime().getTime());
    }
    if (user.getDataAccessLevelEnum() != null) {
      profile.setDataAccessLevel(user.getDataAccessLevelEnum());
    }
    if (user.getBetaAccessBypassTime() != null) {
      profile.setBetaAccessBypassTime(user.getBetaAccessBypassTime().getTime());
    }
    if (user.getBetaAccessRequestTime() != null) {
      profile.setBetaAccessRequestTime(user.getBetaAccessRequestTime().getTime());
    }
    if (user.getEmailVerificationCompletionTime() != null) {
      profile.setEmailVerificationCompletionTime(
          user.getEmailVerificationCompletionTime().getTime());
    }
    if (user.getEmailVerificationBypassTime() != null) {
      profile.setEmailVerificationBypassTime(user.getEmailVerificationBypassTime().getTime());
    }
    if (user.getDataUseAgreementCompletionTime() != null) {
      profile.setDataUseAgreementCompletionTime(user.getDataUseAgreementCompletionTime().getTime());
    }
    if (user.getDataUseAgreementBypassTime() != null) {
      profile.setDataUseAgreementBypassTime(user.getDataUseAgreementBypassTime().getTime());
    }
    if (user.getDataUseAgreementSignedVersion() != null) {
      profile.setDataUseAgreementSignedVersion(user.getDataUseAgreementSignedVersion());
    }
    if (user.getTwoFactorAuthCompletionTime() != null) {
      profile.setTwoFactorAuthCompletionTime(user.getTwoFactorAuthCompletionTime().getTime());
    }
    if (user.getTwoFactorAuthBypassTime() != null) {
      profile.setTwoFactorAuthBypassTime(user.getTwoFactorAuthBypassTime().getTime());
    }
    if (user.getAuthoritiesEnum() != null) {
      profile.setAuthorities(new ArrayList<>(user.getAuthoritiesEnum()));
    }
    if (user.getPageVisits() != null && !user.getPageVisits().isEmpty()) {
      profile.setPageVisits(
          user.getPageVisits().stream().map(TO_CLIENT_PAGE_VISIT).collect(Collectors.toList()));
    }
    if (user.getDemographicSurvey() != null) {
      profile.setDemographicSurvey(TO_CLIENT_DEMOGRAPHIC_SURVEY.apply(user.getDemographicSurvey()));
    } else {
      profile.setDemographicSurvey(new DemographicSurvey());
    }
    if (user.getAddress() != null) {
      profile.setAddress(TO_CLIENT_ADDRESS_SURVEY.apply(user.getAddress()));
    }
    profile.setInstitutionalAffiliations(
        user.getInstitutionalAffiliations().stream()
            .map(TO_CLIENT_INSTITUTIONAL_AFFILIATION)
            .collect(Collectors.toList()));
    profile.setEmailVerificationStatus(user.getEmailVerificationStatusEnum());

    return profile;
  }
}
