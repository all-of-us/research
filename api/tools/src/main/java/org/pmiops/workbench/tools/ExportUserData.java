package org.pmiops.workbench.tools;

import com.opencsv.bean.BeanField;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A tool that takes a Workspace namespace / Firecloud Project ID and returns details for any
 * workspaces found.
 *
 * <p>Details currently include... - Name - Creator Email - Collaborator Emails and Access Levels
 */
@SpringBootApplication
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
@EntityScan("org.pmiops.workbench.db.model")
public class ExportUserData {

  private static final Logger log = Logger.getLogger(ExportUserData.class.getName());

  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();

  private static Options options = new Options().addOption(fcBaseUrlOpt);

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      FileWriter writer = new FileWriter("export.csv");

      List<WorkspaceExportRow> rows = new ArrayList<>();

      WorkspacesApi workspacesApi =
          (new ServiceAccountAPIClientFactory(opts.getOptionValue(fcBaseUrlOpt.getLongOpt())))
              .workspacesApi();

      int n = 0;
      for (DbWorkspace workspace : workspaceDao.findAll()) {
        WorkspaceExportRow row = new WorkspaceExportRow();
        row.setCreatorContactEmail(workspace.getCreator().getContactEmail());
        row.setCreatorUsername(workspace.getCreator().getEmail());

        n++;
        if (n == 10) {
          break;
        }

        rows.add(row);
      }

      final CustomMappingStrategy mappingStrategy = new CustomMappingStrategy();
      mappingStrategy.setType(WorkspaceExportRow.class);

      StatefulBeanToCsvBuilder<WorkspaceExportRow> builder =
          new StatefulBeanToCsvBuilder(writer);
      StatefulBeanToCsv beanWriter =
          builder.withMappingStrategy(mappingStrategy).build();

      beanWriter.write(rows);

      writer.close();
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(ExportUserData.class).web(false).run(args);
  }
}

class CustomMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {
  @Override
  public String[] generateHeader(T bean) {

    super.setColumnMapping(new String[FieldUtils.getAllFields(bean.getClass()).length]);
    final int numColumns = findMaxFieldIndex();

    String[] header = new String[numColumns + 1];

    BeanField<T> beanField;
    for (int i = 0; i <= numColumns; i++) {
      beanField = findField(i);
      String columnHeaderName = extractHeaderName(beanField);
      header[i] = columnHeaderName;
    }
    return header;
  }

  private String extractHeaderName(final BeanField<T> beanField) {
    return beanField.getField()
        .getDeclaredAnnotationsByType(CsvBindByName.class)[0].column();
  }
}