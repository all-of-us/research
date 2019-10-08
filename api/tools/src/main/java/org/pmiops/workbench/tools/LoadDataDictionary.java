package org.pmiops.workbench.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.logging.Logger;
import org.pmiops.workbench.dataset.DataDictionaryEntryDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench")
@EntityScan("org.pmiops.workbench")
public class LoadDataDictionary {

  private static final Logger logger = Logger.getLogger(LoadDataDictionary.class.getName());

  @Bean
  public CommandLineRunner run(Clock clock,
      CdrVersionDao cdrVersionDao,
      DataDictionaryEntryDao dataDictionaryEntryDao) {
    return (args) -> {

      CdrVersion defaultCdrVersion = cdrVersionDao.findByIsDefault(true);

      ClassLoader cl = this.getClass().getClassLoader();
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
      Resource[] resources = resolver.getResources("classpath*:/data_dictionary_exports/*.txt") ;
      for (Resource resource : resources) {
        InputStream is = resource.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
          String[] fields = line.split(":");

          String relevantOmopTable = fields[0];
          String fieldName = fields[1];

          DataDictionaryEntry entry = dataDictionaryEntryDao.findByRelevantOmopTableAndFieldNameAndCdrVersion(
              relevantOmopTable, fieldName, defaultCdrVersion);

          if (entry == null) {
            entry = new DataDictionaryEntry();
            entry.setRelevantOmopTable(relevantOmopTable);
            entry.setFieldName(fieldName);
            entry.setCdrVersion((defaultCdrVersion));
          }

          entry.setDefinedTime(new Timestamp(clock.instant().toEpochMilli()));
          entry.setOmopCdmStandardOrCustomField(fields[2]);
          entry.setDescription(fields[3]);
          entry.setFieldType(fields[4]);
          entry.setDataProvenance(fields[5]);
          entry.setSourcePpiModule(fields[6]);
          entry.setTransformedByRegisteredTierPrivacyMethods("true".equals(fields[7]));

          dataDictionaryEntryDao.save(entry);
        }
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(LoadDataDictionary.class).web(false).run(args);
  }
}
