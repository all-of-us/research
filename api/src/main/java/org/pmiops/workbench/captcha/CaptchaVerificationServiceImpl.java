package org.pmiops.workbench.captcha;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.captcha.api.CaptchaApi;
import org.pmiops.workbench.captcha.model.CaptchaVerificationResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service to verify Captcha */
@Service
public class CaptchaVerificationServiceImpl implements CaptchaVerificationService {

  final String urlPattern = "https://%s/login";

  final String googleTestHost = "testkey.google.com";

  private CloudStorageService cloudStorageService;
  final Provider<WorkbenchConfig> configProvider;
  private Provider<CaptchaApi> captchaApiProvider;

  private static final Logger log =
      Logger.getLogger(CaptchaVerificationServiceImpl.class.getName());

  @Autowired
  public CaptchaVerificationServiceImpl(
      CloudStorageService cloudStorageService,
      javax.inject.Provider<WorkbenchConfig> configProvider,
      javax.inject.Provider<CaptchaApi> captchaApiProvider) {
    this.cloudStorageService = cloudStorageService;
    this.configProvider = configProvider;
    this.captchaApiProvider = captchaApiProvider;
  }

  @VisibleForTesting
  public void mockLoginUrl(String loginUrl) {
    WorkbenchConfig.AdminConfig adminConfig = new WorkbenchConfig.AdminConfig();
    adminConfig.loginUrl = loginUrl;
    configProvider.get().admin = adminConfig;
  }

  @VisibleForTesting
  public void mockUseTestCaptcha(boolean useTestCaptcha) {
    WorkbenchConfig.CaptchaConfig captchaConfig = new WorkbenchConfig.CaptchaConfig();
    captchaConfig.useTestCaptcha = useTestCaptcha;

    configProvider.get().captcha = captchaConfig;
  }


  /**
   * Calls google api to verify Captcha Response by sending the captcha Server key associated with
   * host and the token generated by the user response on front end. Returns true if valid else
   * false
   *
   * @param responseToken
   * @return if Captcha is valid
   */
  @Override
  public boolean verifyCaptcha(String responseToken) throws ApiException {
    CaptchaVerificationResponse response =
        captchaApiProvider.get().verify(cloudStorageService.getCaptchaServerKey(), responseToken);
    if (!response.getSuccess()) {
      log.log(
          Level.WARNING,
          String.format(
              "Exception while verifying captcha%s",
              response.getErrorCodes().stream()
                  .map(errorCodes -> errorCodes.getValue())
                  .collect(Collectors.joining(","))));
      return false;
    }
    String captchaHostname = response.getHostname();
    String uiUrl = configProvider.get().admin.loginUrl;
    boolean usingTestCaptcha = configProvider.get().captcha.useTestCaptcha;
    boolean captchaHostNameMatchUI = false;

    // Production/Stable should  not use google test Key and the domainName should match with the
    // one return from Captcha Response
    if (!usingTestCaptcha) {
      // check if the UI URL has the host as send by Captcha Response
      captchaHostNameMatchUI = String.format(urlPattern, captchaHostname).equals(uiUrl);
    } else {
      captchaHostNameMatchUI =
          captchaHostname.equals(googleTestHost);
    }
    if (!captchaHostNameMatchUI) {
      log.log(
          Level.SEVERE, String.format("Captcha Host Name %s does not match UI", captchaHostname));
    }
    return response.getSuccess() && captchaHostNameMatchUI;
  }
}
