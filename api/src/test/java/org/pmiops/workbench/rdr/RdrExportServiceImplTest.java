package org.pmiops.workbench.rdr;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RdrExportServiceImplTest {
  @Autowired private RdrExportService rdrExportService;

  @MockBean private ApiClient mockApiClient;
  @MockBean private RdrApi mockRdrApi;
  @MockBean private RdrExportDao rdrExportDao;
  @MockBean private UserDao mockUserDao;

  private static final Instant NOW = Instant.now();
  private static final Timestamp NOW_TIMESTAMP = Timestamp.from(NOW);
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  private DbUser dbUserWithEmail;
  private DbUser dbUserWithoutEmail;

  @TestConfiguration
  @Import({RdrExportServiceImpl.class})
  @MockBean({
      RdrApi.class,
      RdrExportDao.class,
      WorkspaceDao.class,
      WorkspaceService.class,
      UserDao.class
  })
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    rdrExportService = spy(rdrExportService);
    when(mockRdrApi.getApiClient()).thenReturn(mockApiClient);
    when(mockApiClient.setDebugging(true)).thenReturn(null);

    dbUserWithEmail = new DbUser();
    dbUserWithEmail.setUserId(1L);
    dbUserWithEmail.setCreationTime(NOW_TIMESTAMP);
    dbUserWithEmail.setLastModifiedTime(NOW_TIMESTAMP);
    dbUserWithEmail.setGivenName("icanhas");
    dbUserWithEmail.setFamilyName("email");
    dbUserWithEmail.setContactEmail("i.can.has.email@gmail.com");
    dbUserWithEmail.setDegreesEnum(Collections.singletonList(Degree.NONE));

    when(mockUserDao.findUserByUserId(1L)).thenReturn(dbUserWithEmail);

    dbUserWithoutEmail = new DbUser();
    dbUserWithoutEmail.setUserId(2L);
    dbUserWithoutEmail.setCreationTime(NOW_TIMESTAMP);
    dbUserWithoutEmail.setLastModifiedTime(NOW_TIMESTAMP);
    dbUserWithoutEmail.setGivenName("icannothas");
    dbUserWithoutEmail.setFamilyName("email");
    dbUserWithoutEmail.setDegreesEnum(Collections.singletonList(Degree.NONE));

    when(mockUserDao.findUserByUserId(2L)).thenReturn(dbUserWithoutEmail);

    when(rdrExportDao.findByEntityTypeAndEntityId(anyShort(), anyLong())).thenReturn(null);
  }

  @Test
  public void exportUsers_successful() throws ApiException {
    doNothing().when(mockRdrApi).exportResearchers(anyList());

    List<Long> userIds = new ArrayList<>();
    userIds.add(dbUserWithEmail.getUserId());
    userIds.add(dbUserWithoutEmail.getUserId());
    rdrExportService.exportUsers(userIds);

    verify(rdrExportService, times(1)).updateDBRdrExport(any(), anyList());
  }

  @Test
  public void exportUsers_unsuccessful_no_persist() throws ApiException {
    doThrow(new ApiException()).when(mockRdrApi).exportResearchers(anyList());

    List<Long> userIds = new ArrayList<>();
    userIds.add(dbUserWithEmail.getUserId());
    userIds.add(dbUserWithoutEmail.getUserId());
    rdrExportService.exportUsers(userIds);

    verify(rdrExportService, times(0)).updateDBRdrExport(any(), anyList());
  }
}
