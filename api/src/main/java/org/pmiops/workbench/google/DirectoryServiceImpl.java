package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.gax.rpc.ApiException;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserEmail;
import com.google.api.services.admin.directory.model.UserName;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.MandrillService;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

@Service
public class DirectoryServiceImpl implements DirectoryService {

  static final String APPLICATION_NAME = "All of Us Researcher Workbench";
  private static final String ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
  private static SecureRandom rnd = new SecureRandom();

  // This list must exactly match the scopes allowed via the GSuite Domain Admin page here:
  // https://admin.google.com/AdminHome?chromeless=1#OGX:ManageOauthClients
  // For example, ADMIN_DIRECTORY_USER does not encapsulate ADMIN_DIRECTORY_USER_READONLY — it must
  // be explicit.
  // The "Client Name" field in that form must be the cient ID of the service account. The field
  // will accept the email address of the service account and lookup the correct client ID giving
  // the impression that the email address is an acceptable substitute, but testing shows that this
  // doesn't actually work.
  static final List<String> SCOPES = Arrays.asList(
      DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
  );

  private final Provider<GoogleCredential> googleCredentialProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final HttpTransport httpTransport;
  private final GoogleRetryHandler retryHandler;
  private final MandrillService mandrillService;

  @Autowired
  public DirectoryServiceImpl(Provider<GoogleCredential> googleCredentialProvider,
      Provider<WorkbenchConfig> configProvider, MandrillService mandrillService,
      HttpTransport httpTransport, GoogleRetryHandler retryHandler) {
    this.googleCredentialProvider = googleCredentialProvider;
    this.configProvider = configProvider;
    this.httpTransport = httpTransport;
    this.retryHandler = retryHandler;
    this.mandrillService = mandrillService;
  }

  private GoogleCredential createCredentialWithImpersonation() {
    GoogleCredential googleCredential = googleCredentialProvider.get();
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    return new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(getDefaultJsonFactory())
        // Must be an admin user in the GSuite domain.
        .setServiceAccountUser("directory-service@"+gSuiteDomain)
        .setServiceAccountId(googleCredential.getServiceAccountId())
        .setServiceAccountScopes(SCOPES)
        .setServiceAccountPrivateKey(googleCredential.getServiceAccountPrivateKey())
        .setServiceAccountPrivateKeyId(googleCredential.getServiceAccountPrivateKeyId())
        .setTokenServerEncodedUrl(googleCredential.getTokenServerEncodedUrl())
        .build();
  }

  private Directory getGoogleDirectoryService() {
    return new Directory.Builder(httpTransport, getDefaultJsonFactory(),
          createCredentialWithImpersonation())
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  @Override
  public User getUser(String email) {
    try {
      return retryHandler.runAndThrowChecked((context) ->
          getGoogleDirectoryService().users().get(email).execute());
    } catch (GoogleJsonResponseException e) {
      // Handle the special case where we're looking for a not found user by returning
      // null.
      if (e.getDetails().getCode() == HttpStatus.NOT_FOUND.value()) {
        return null;
      }
      throw ExceptionUtils.convertGoogleIOException(e);
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
  }

  @Override
  public boolean isUsernameTaken(String username) {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    return getUser(username + "@" + gSuiteDomain) != null;
  }

  @Override
  public User createUser(String givenName, String familyName, String username, String contactEmail) {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    String password = randomString();
    User user = new User()
      .setPrimaryEmail(username+"@"+gSuiteDomain)
      .setPassword(password)
      .setName(new UserName().setGivenName(givenName).setFamilyName(familyName))
      .setEmails(new UserEmail().setType("custom").setAddress(contactEmail).setCustomType("contact"))
      .setChangePasswordAtNextLogin(true);
    retryHandler.run((context) -> getGoogleDirectoryService().users().insert(user).execute());
    //This is a temporary solution until Mandrill is implemented.
    //This way if one merge happens and the other is delayed for any reason,
    //we won't break in a major way.
    sendPasswordEmail(contactEmail, password, user);
    return user;
  }
  @Override
  public void deleteUser(String username) {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    try {
      retryHandler.runAndThrowChecked((context)-> getGoogleDirectoryService().users()
          .delete(username + "@" + gSuiteDomain).execute());
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == HttpStatus.NOT_FOUND.value()) {
        // Deleting a user that doesn't exist will have no effect.
        return;
      }
      throw ExceptionUtils.convertGoogleIOException(e);
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
  }

  private String randomString(){
    return IntStream.range(0, 17).boxed().
      map(x -> ALLOWED.charAt(rnd.nextInt(ALLOWED.length()))).
      map(Object::toString).
      collect(Collectors.joining(""));
  }

  protected void sendPasswordEmail(String contactEmail, String password, User user) {
    MandrillMessage message = new MandrillMessage();
    RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(contactEmail);
    message.setTo(Arrays.asList(toAddress));
    String messageBody = "Your new account is: " + user.getPrimaryEmail() +
      "\nThe password for your new account is: " + password;
    message.setHtml(messageBody);
    message.setSubject("Your new All of Us Account");
    // using this address as a placeholder right now.
    // do not think it is right since I'm getting a
    // unsigned rejection right now, but the api should work
    // once we have the domains/from Email straightened out
    message.setFromEmail("donotreply@allofus.org");
    try {
      mandrillService.sendEmail(message);
    } catch (ApiException e) {
      throw new EmailException("Error sending initial email", e);
    }
  }
}
