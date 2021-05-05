package org.pmiops.workbench;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@Import({BaseTestConfiguration.class})
public class SpringTest {
}
