package org.pmiops.workbench.compliance;

import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.moodle.ApiClient;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.pmiops.workbench.moodle.model.MoodleUserResponse;
import org.pmiops.workbench.moodle.model.UserBadgeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.pmiops.workbench.config.WorkbenchConfig;
import javax.inject.Provider;
import java.util.List;

@Service
public class ComplianceServiceImpl implements ComplianceService {

  private MoodleApi api = new MoodleApi();
  private static final String RESPONSE_FORMAT = "json";
  private static final String GET_MOODLE_ID_SEARCH_FIELD = "email";
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed";
  private CloudStorageService cloudStorageService;
  private Provider<WorkbenchConfig> configProvider;


  @Autowired
  public ComplianceServiceImpl(CloudStorageService cloudStorageService,
      Provider<WorkbenchConfig> configProvider) {
    this.cloudStorageService = cloudStorageService;
    this.configProvider = configProvider;
  }

  private String getToken() {
    return this.cloudStorageService.getMoodleApiKey();
  }

  private boolean enableMoodleCalls() {
    return configProvider.get().moodle.enableMoodleBackend;
  }

  private void setApiHost() {
    ApiClient apiClient = api.getApiClient();
    apiClient.setBasePath("https://" + configProvider.get().moodle.host +"/webservice/rest");
  }

  @Override
  public Integer getMoodleId(String email) throws ApiException {
    if (!enableMoodleCalls())
      return null;
    setApiHost();
    api.getApiClient().setDebugging(true);
    List<MoodleUserResponse> response = api.getMoodleId(getToken(), GET_MOODLE_ID_SEARCH_FIELD, email);
    if (response.size() == 0) {
      return null;
    }
    return response.get(0).getId();
  }

  @Override
  public List<BadgeDetails>  getUserBadge(int userMoodleId) throws ApiException {
    if (!enableMoodleCalls())
      return null;
    setApiHost();
    api.getApiClient().setDebugging(true);

    UserBadgeResponse response = api.getMoodleBadge(RESPONSE_FORMAT, getToken(), userMoodleId);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      }
      else {
        throw new ApiException(response.getMessage());
      }
    }
    return response.getBadges();
  }
}
