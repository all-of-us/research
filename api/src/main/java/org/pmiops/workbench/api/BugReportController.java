package org.pmiops.workbench.api;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.model.BugReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BugReportController implements BugsApiDelegate {

  private static final Logger log = Logger.getLogger(BugReportController.class.getName());



  @Override
  public ResponseEntity<BugReport> sendBug(BugReport bugReport) throws MessagingException, AddressException, UnsupportedEncodingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    // try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress("all-of-us-research-tools@googlegroups.com"));
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress("brubenst@broadinstitute.org", "My Email"));
      msg.setSubject("[AofU Bug Report]: " + bugReport.getShortDescription());
      msg.setText(bugReport.getReproSteps());
      Transport.send(msg);
      System.out.println("[AofU Bug Report]: " + bugReport.getShortDescription());
      System.out.println(bugReport.getReproSteps());
    // } catch MessagingException {
    //
    // } catch AddressException {
    //
    // } catch UnsupportedEncodingException {
    //
    // }
    return ResponseEntity.ok(bugReport);
  }
}
