package org.pmiops.workbench.compliance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.moodle.model.MoodleUserResponse;
import org.pmiops.workbench.moodle.model.UserBadgeResponseV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ComplianceServiceImpl implements ComplianceService {

  private MoodleApi api = new MoodleApi();
  private static final String RESPONSE_FORMAT = "json";
  private static final String GET_MOODLE_ID_SEARCH_FIELD = "email";
  private static final String DUA_BADGE_NAME = "data_use_agreement";
  private static final String RET_BADGE_NAME =
      "research_ethics_training"; // 'ret' too much like 'return'
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed";
  private CloudStorageClient cloudStorageClient;
  private Provider<WorkbenchConfig> configProvider;

  private Provider<MoodleApi> moodleApiProvider;

  private static final Logger logger = Logger.getLogger(ComplianceServiceImpl.class.getName());

  @Autowired
  public ComplianceServiceImpl(
      CloudStorageClient cloudStorageClient,
      Provider<WorkbenchConfig> configProvider,
      Provider<MoodleApi> moodleApiProvider) {
    this.cloudStorageClient = cloudStorageClient;
    this.configProvider = configProvider;
    this.moodleApiProvider = moodleApiProvider;
  }

  private String getToken() {
    return this.cloudStorageClient.getMoodleApiKey();
  }

  private boolean enableMoodleCalls() {
    return configProvider.get().moodle.enableMoodleBackend;
  }

  /**
   * Returns the Moodle ID corresponding to the given AoU user email address.
   *
   * <p>Returns null if no Moodle user ID was found.
   */
  @Override
  public Integer getMoodleId(String email) throws ApiException {
    if (!enableMoodleCalls()) {
      return null;
    }
    List<MoodleUserResponse> response =
        moodleApiProvider.get().getMoodleId(getToken(), GET_MOODLE_ID_SEARCH_FIELD, email);
    if (response.size() == 0) {
      return null;
    }
    return response.get(0).getId();
  }

  /**
   * Returns a map of Moodle badge names to Moodle badge details for the given Moodle user email.
   *
   * @param email The research-aou.org email for the user
   * @return A map of badge name to badge details.
   * @throws ApiException if the Moodle API call returns an error because the email is not yet
   *     registered in Moodle.
   */
  @Override
  public Map<String, BadgeDetailsV2> getUserBadgesByBadgeName(String email) throws ApiException {
    if (!enableMoodleCalls()) {
      return new HashMap<>();
    }

    UserBadgeResponseV2 response =
        moodleApiProvider.get().getMoodleBadgeV2(RESPONSE_FORMAT, getToken(), email);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      logger.warning(response.getMessage());
      if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      } else {
        throw new ApiException(response.getMessage());
      }
    }
    Map<String, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    if (response.getDua() != null) {
      userBadgesByName.put(DUA_BADGE_NAME, response.getDua());
    }
    if (response.getRet() != null) {
      userBadgesByName.put(RET_BADGE_NAME, response.getRet());
    }
    return userBadgesByName;
  }

  public String getResearchEthicsTrainingField() {
    return RET_BADGE_NAME;
  }
}
