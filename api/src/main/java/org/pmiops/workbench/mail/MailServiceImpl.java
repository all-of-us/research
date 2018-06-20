package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.User;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

@Service
public class MailServiceImpl implements MailService {

    private final Provider<MandrillApi> mandrillApiProvider;
    private final Provider<CloudStorageService> cloudStorageServiceProvider;
    private Provider<WorkbenchConfig> workbenchConfigProvider;
    private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());
    enum Status {REJECTED, API_ERROR, SUCCESSFUL}

    @Autowired
    public MailServiceImpl(Provider<MandrillApi> mandrillApiProvider,
                           Provider<CloudStorageService> cloudStorageServiceProvider,
                           Provider<WorkbenchConfig> workbenchConfigProvider) {
        this.mandrillApiProvider = mandrillApiProvider;
        this.cloudStorageServiceProvider = cloudStorageServiceProvider;
        this.workbenchConfigProvider = workbenchConfigProvider;
    }

    public void send(Message msg) throws MessagingException {
        Transport.send(msg);
    }

    public void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException {
        String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey();
        int retries = workbenchConfigProvider.get().mandrill.sendRetries;
        MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
        keyAndMessage.setKey(apiKey);
        MandrillMessage msg = buildWelcomeMessage(contactEmail, password, user);
        keyAndMessage.setMessage(msg);
        retry: do {
            retries--;
            log.log(Level.INFO, "retries: " + retries);
            ImmutablePair attempt = trySend(keyAndMessage);
            Status status = Status.valueOf(attempt.getLeft().toString());
            switch (status) {
                case API_ERROR:
                    log.log(Level.WARNING, String.format(
                      "ApiException: Welcome Email to '%s' for user '%s' not sent: %s", contactEmail, user.getName(), attempt.getRight().toString()));
                    if (retries == 0) {
                        log.log(Level.SEVERE, String.format(
                          "ApiException: On Last Attempt! Welcome Email to '%s' for user '%s' not sent: %s", contactEmail, user.getName(), attempt.getRight().toString()));
                        throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
                    }

                case REJECTED:
                    log.log(Level.SEVERE, String.format(
                      "Messaging Exception: Welcome Email to '%s' for user '%s' not sent: %s", contactEmail, user.getName(), attempt.getRight().toString()));
                    throw new MessagingException("Sending email failed: " + attempt.getRight().toString());

                case SUCCESSFUL:
                    log.log(Level.INFO, String.format(
                      "Welcome Email to '%s' for user '%s' was sent.", contactEmail, user.getName()));
                    break retry;
            }
        } while (retries > 0);
    }

    private MandrillMessage buildWelcomeMessage(String contactEmail, String password, User user) {
        MandrillMessage msg = new MandrillMessage();
        RecipientAddress toAddress = new RecipientAddress();
        toAddress.setEmail(contactEmail);
        msg.setTo(Collections.singletonList(toAddress));
        String msgBody = "Your new account is: " + user.getPrimaryEmail() +
          "\nThe password for your new account is: " + password;
        msg.setHtml(msgBody);
        msg.setSubject("Your new All of Us Account");
        msg.setFromEmail(workbenchConfigProvider.get().mandrill.fromEmail);
        return msg;
    }

    private ImmutablePair<String, String> trySend(MandrillApiKeyAndMessage keyAndMessage) {
        try {
            log.log(Level.INFO, "trying send");
            MandrillMessageStatuses msgStatuses = mandrillApiProvider.get().send(keyAndMessage);
            for (MandrillMessageStatus msgStatus : msgStatuses) {
                if (msgStatus.getRejectReason() != null) {
                    return new ImmutablePair<String, String>("REJECTED", msgStatus.getRejectReason());
                }
            }
        } catch (Exception e) {
            log.log(Level.INFO, "in catch");
            return new ImmutablePair<>("API_ERROR", e.toString());
        }
        return new ImmutablePair<>("SUCCESSFUL", "");
    }

}
