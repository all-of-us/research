package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class NotebooksServiceImplTest {

  @TestConfiguration
  @Import({NotebooksServiceImpl.class})
  @MockBean({
    CloudStorageService.class,
    FireCloudService.class,
    WorkspaceService.class,
    UserRecentResourceService.class
  })
  static class Configuration {

    @Bean
    Clock clock() {
      return new FakeClock();
    }

    @Bean
    User user() {
      return null;
    }
  }

  @Autowired private NotebooksService notebooksService;
  @Autowired private FireCloudService firecloudService;
  @Autowired private CloudStorageService cloudStorageService;

  @Test
  public void testGetReadOnlyHtml_basicContent() throws Exception {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(new WorkspaceResponse().workspace(new Workspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><body><div>asdf</div></body></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("div");
    assertThat(html).contains("asdf");
  }

  @Test
  public void testGetReadOnlyHtml_scriptSanitization() throws Exception {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(new WorkspaceResponse().workspace(new Workspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><script>window.alert('hacked');</script></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("script");
    assertThat(html).doesNotContain("alert");
  }

  @Test
  public void testGetReadOnlyHtml_styleSanitization() throws Exception {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(new WorkspaceResponse().workspace(new Workspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn(
            "<STYLE type=\"text/css\">BODY{background:url(\"javascript:alert('XSS')\")} div {color: 'red'}</STYLE>\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("style");
    assertThat(html).contains("color");
    // This behavior is not desired, but this test is in place to enshrine current expected
    // behavior. Style tags can introduce vulnerabilities as demonstrated in the test case - we
    // expect that the only style tags produced in the preview are produced by nbconvert, and are
    // therefore safe. Ideally we would keep the style tag, but sanitize the contents.
    assertThat(html).contains("XSS");
  }
}
