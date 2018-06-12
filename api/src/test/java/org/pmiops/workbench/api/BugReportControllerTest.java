package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.model.JupyterContents;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BugReportControllerTest {
  private static final String FC_PROJECT_ID = "fc-project";
  private static final String USER_EMAIL = "falco@lombardi.com";

  private static final JupyterContents TEST_CONTENTS =
      new JupyterContents().content("log contents");
  private List<Message> sentMessages = new ArrayList<>();

  @TestConfiguration
  @Import({BugReportController.class})
  @MockBean({JupyterApi.class})
  static class Configuration {
    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = new WorkbenchConfig();
      config.admin = new WorkbenchConfig.AdminConfig();
      config.admin.supportGroup = "support@asdf.com";
      config.admin.verifiedSendingAddress = "sender@asdf.com";
      return config;
    }

    @Bean
    User user() {
      // Allows for wiring of the initial Provider<User>; actual mocking of the
      // user is achieved via setUserProvider().
      return null;
    }
  }

  @Mock
  Provider<User> userProvider;
  @Mock
  MailService mailService;
  @Autowired
  JupyterApi jupyterApi;
  @Autowired
  BugReportController bugReportController;

  @Before
  public void setUp() throws MessagingException {
    sentMessages.clear();
    Mockito.doAnswer(
      invocation -> {
        sentMessages.add(invocation.getArgumentAt(0, Message.class));
        return null;
      }
    ).when(mailService).send(any());
    User user = new User();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName(FC_PROJECT_ID);
    user.setFreeTierBillingProjectStatus(BillingProjectStatus.READY);
    user.setDisabled(false);
    when(userProvider.get()).thenReturn(user);
    bugReportController.setUserProvider(userProvider);
    bugReportController.setMailServiceProvider(Providers.of(mailService));
  }

  @Test
  public void testSendBugReport() throws Exception {
    bugReportController.sendBugReport(
        new BugReport()
          .contactEmail(USER_EMAIL)
          .includeNotebookLogs(false)
          .reproSteps("press button")
          .shortDescription("bug"));
    verify(mailService, times(1)).send(Mockito.any());
    // The message content should have 1 part, the main body part and no attachments
    assertSentMessageParts(1);
    verify(jupyterApi, never()).getRootContents(any(), any(), any(), any(), any(), any());
  }

  public void testSendBugReport_withUnderscore() throws Exception {
    fail("nope");
  }

  public void testSendBugReport_withUnderscore2() throws Exception {
    fail("nope");
  }

  @Test
  public void testSendBugReportWithNotebooks() throws Exception {
    when(jupyterApi.getRootContents(any(), any(), any(), any(), any(), any()))
        .thenReturn(TEST_CONTENTS);
    bugReportController.sendBugReport(
        new BugReport()
          .contactEmail(USER_EMAIL)
          .includeNotebookLogs(true)
          .reproSteps("press button")
          .shortDescription("bug"));

    verify(mailService, times(1)).send(Mockito.any());
    // The message content should have 4 parts, the main body part and three attachments
    assertSentMessageParts(4);
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("delocalization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("localization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("jupyter.log"), any(), any(), any());
  }

  @Test
  public void testSendBugReportWithNotebookErrors() throws Exception {
    when(jupyterApi.getRootContents(any(), any(), any(), any(), any(), any()))
        .thenReturn(TEST_CONTENTS);
    when(jupyterApi.getRootContents(any(), any(), eq("jupyter.log"), any(), any(), any()))
        .thenThrow(new ApiException(404, "not found"));
    bugReportController.sendBugReport(
        new BugReport()
          .contactEmail(USER_EMAIL)
          .includeNotebookLogs(true)
          .reproSteps("press button")
          .shortDescription("bug"));

    verify(mailService, times(1)).send(Mockito.any());
    // The message content should have 3 parts, the main body part and two attachments
    assertSentMessageParts(3);
  }

  private void assertSentMessageParts(int count) throws Exception {
    assertThat(sentMessages).isNotEmpty();
    Message msg = sentMessages.get(0);
    Multipart multipart = (Multipart) msg.getContent();
    assertThat(multipart.getCount()).isEqualTo(count);
  }

}
