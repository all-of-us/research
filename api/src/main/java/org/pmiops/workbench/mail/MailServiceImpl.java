package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.User;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.pmiops.workbench.model.IdVerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.api.services.admin.directory.model.User;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Service
public class MailServiceImpl implements MailService {
  private String ID_VERIFICATION_TEXT = "A new user has requested manual ID verification: ";

  private final Provider<MandrillApi> mandrillApiProvider;
  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());
  private static final String WELCOME_RESOURCE = "emails/welcomeemail/content.html";
  private static final String ID_VERIFICATION_RESOURCE = "emails/idverificationemail/content.html";

  enum Status {REJECTED, API_ERROR, SUCCESSFUL}

  @Autowired
  public MailServiceImpl(Provider<MandrillApi> mandrillApiProvider,
                         Provider<CloudStorageService> cloudStorageServiceProvider,
                         Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.mandrillApiProvider = mandrillApiProvider;
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void sendIdVerificationRequestEmail(String userName) throws MessagingException {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();

    MandrillMessage msg = new MandrillMessage();
    RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(workbenchConfig.admin.adminIdVerification);
    msg.setTo(Collections.singletonList(toAddress));
    msg.setSubject("[Id Verification Request]: " + userName);
    msg.setHtml(ID_VERIFICATION_TEXT + userName);
    msg.setFromEmail(workbenchConfig.mandrill.fromEmail);

    sendWithRetries(msg, "IDV submit notification");
  }

  @Override
  public void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException {
    try {
      InternetAddress email = new InternetAddress(contactEmail);
      email.validate();
    } catch (AddressException e) {
      throw new MessagingException("Email: " + contactEmail + " is invalid.");
    }
    MandrillMessage msg = buildWelcomeMessage(contactEmail, password, user);
    sendWithRetries(msg, String.format("Welcome for %s", user.getName()));
  }

  @Override
  public void sendIdVerificationCompleteEmail(String contactEmail, IdVerificationStatus status, String username) throws MessagingException {
    try {
      InternetAddress email = new InternetAddress(contactEmail);
      email.validate();
    } catch (AddressException e) {
      throw new MessagingException("Email: " + contactEmail + " is invalid.");
    }
    MandrillMessage msg = buildIdVerificationCompleteMessage(contactEmail, status, username);
    sendWithRetries(msg, String.format("ID Verification Complete for %s", contactEmail));
  }

  private void sendWithRetries(MandrillMessage msg, String description) throws MessagingException {
    String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey();
    int retries = workbenchConfigProvider.get().mandrill.sendRetries;
    MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
    keyAndMessage.setKey(apiKey);
    keyAndMessage.setMessage(msg);
    do {
      retries--;
      ImmutablePair<Status, String> attempt = trySend(keyAndMessage);
      Status status = Status.valueOf(attempt.getLeft().toString());
      switch (status) {
        case API_ERROR:
          log.log(Level.WARNING, String.format(
              "ApiException: Email '%s' not sent: %s", description, attempt.getRight().toString()));
          if (retries == 0) {
            log.log(Level.SEVERE, String.format(
              "ApiException: On Last Attempt! Email '%s' not sent: %s",
              description, attempt.getRight().toString()));
            throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
          }
          break;

        case REJECTED:
          log.log(Level.SEVERE, String.format(
            "Messaging Exception: Email '%s' not sent: %s",
            description, attempt.getRight().toString()));
          throw new MessagingException("Sending email failed: " + attempt.getRight().toString());

        case SUCCESSFUL:
          log.log(Level.INFO, String.format("Email '%s' was sent.", description));
          return;

        default:
          if (retries == 0) {
            log.log(Level.SEVERE, String.format(
              "Email '%s' was not sent. Default case.", description));
            throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
          }
      }
    } while (retries > 0);
  }

  private MandrillMessage buildWelcomeMessage(String contactEmail, String password, User user) throws MessagingException {
    MandrillMessage msg = new MandrillMessage();
    RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(contactEmail);
    msg.setTo(Collections.singletonList(toAddress));
    try {
      String msgHtml = buildWelcomeEmailHtml(password, user);
      msg.setHtml(msgHtml);
      msg.setSubject("Your new All of Us Account");
      msg.setFromEmail(workbenchConfigProvider.get().mandrill.fromEmail);
      return msg;
    } catch (IOException e) {
      throw new MessagingException("Error reading in email");
    }
  }


  private String buildWelcomeEmailHtml(String password, User user) throws IOException {
    CloudStorageService cloudStorageService = cloudStorageServiceProvider.get();
    StringBuilder contentBuilder = new StringBuilder();
    URL emailContent = Resources.getResource(WELCOME_RESOURCE);
    Resources
      .readLines(emailContent, StandardCharsets.UTF_8)
      .forEach(s -> contentBuilder.append(s).append("\n"));
    String string = contentBuilder.toString();
    ImmutableMap<String, String> replaceMap = new ImmutableMap.Builder<String, String>()
      .put("USERNAME", user.getPrimaryEmail())
      .put("PASSWORD", password)
      .put("URL", workbenchConfigProvider.get().admin.loginUrl)
      .put("HEADER_IMG", cloudStorageService.getImageUrl("all_of_us_logo.png"))
      .put("BULLET_1", cloudStorageService.getImageUrl("bullet_1.png"))
      .put("BULLET_2", cloudStorageService.getImageUrl("bullet_2.png"))
      .build();
    return new StrSubstitutor(replaceMap).replace(string);
  }

  private MandrillMessage buildIdVerificationCompleteMessage(String contactEmail, IdVerificationStatus status, String username) throws MessagingException {
    MandrillMessage msg = new MandrillMessage();
    RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(contactEmail);
    msg.setTo(Collections.singletonList(toAddress));
    try {
      String msgHtml = buildIdVerificationCompleteHtml(status, username);
      msg.setHtml(msgHtml);
      msg.setSubject("All of Us ID Verification Complete");
      msg.setFromEmail(workbenchConfigProvider.get().mandrill.fromEmail);
      return msg;
    } catch (IOException e) {
      throw new MessagingException("Error reading in email");
    }
  }

  private String buildIdVerificationCompleteHtml(IdVerificationStatus status, String username) throws IOException {
    CloudStorageService cloudStorageService = cloudStorageServiceProvider.get();
    StringBuilder contentBuilder = new StringBuilder();
    URL emailContent = Resources.getResource(ID_VERIFICATION_RESOURCE);
    Resources
        .readLines(emailContent, StandardCharsets.UTF_8)
        .forEach(s -> contentBuilder.append(s).append("\n"));
    String string = contentBuilder.toString();
    String idVerificationReport;
    String action;
    if (status.equals(IdVerificationStatus.REJECTED)) {
      idVerificationReport = "rejected";
      action = "contact our support team at support@researchallofus.org for further information";
    } else {
      idVerificationReport = "approved for use";
      action = "login to the workbench via <a class=\"link\">" + workbenchConfigProvider.get().admin.loginUrl + "</a>";
    }
    ImmutableMap<String, String> replaceMap = new ImmutableMap.Builder<String, String>()
        .put("ACTION", action)
        .put("ID_VERIFICATION_REPORT", idVerificationReport)
        .put("HEADER_IMG", cloudStorageService.getImageUrl("all_of_us_logo.png"))
        .put("USERNAME", username)
        .build();
    return new StrSubstitutor(replaceMap).replace(string);
  }

  private ImmutablePair<Status, String> trySend(MandrillApiKeyAndMessage keyAndMessage) {
    try {
      MandrillMessageStatuses msgStatuses = mandrillApiProvider.get().send(keyAndMessage);
      for (MandrillMessageStatus msgStatus : msgStatuses) {
        if (msgStatus.getRejectReason() != null) {
          return new ImmutablePair<>(Status.REJECTED, msgStatus.getRejectReason());
        }
      }
    } catch (Exception e) {
      return new ImmutablePair<>(Status.API_ERROR, e.toString());
    }
    return new ImmutablePair<>(Status.SUCCESSFUL, "");
  }

}
