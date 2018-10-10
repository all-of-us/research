package org.pmiops.workbench.mail;

import javax.mail.MessagingException;

import com.google.api.services.admin.directory.model.User;
import org.pmiops.workbench.model.IdVerificationStatus;

public interface MailService {

  void sendIdVerificationRequestEmail(String userName) throws MessagingException;

  void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException;

  void sendIdVerificationCompleteEmail(String contactEmail, IdVerificationStatus status, String username) throws MessagingException;
}
