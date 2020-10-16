package org.pmiops.workbench.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.rdr.RdrExportService;
import org.pmiops.workbench.rdr.RdrExportServiceImpl;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

public class DeleteFromRdrExport {
  private static final Logger log = Logger.getLogger(DeleteWorkspaces.class.getName());

  private static Option workspaceListFilename =
      Option.builder()
          .longOpt("workspace-list-filename")
          .desc("File containing list of workspaces Ids")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();

  private static Options options =
      new Options().addOption(workspaceListFilename).addOption(dryRunOpt);

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(DeleteFromRdrExport.class, args);
  }

  @Bean
  public RdrExportService rdrExportService(
      Clock clock,
      Provider<RdrApi> rdrApiProvider,
      RdrExportDao rdrExportDao,
      WorkspaceDao workspaceDao,
      UserDao userDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    return new RdrExportServiceImpl(
        clock,
        rdrApiProvider,
        rdrExportDao,
        null,
        workspaceDao,
        null,
        null,
        userDao,
        verifiedInstitutionalAffiliationDao);
  }

  @Bean
  public CommandLineRunner run(RdrExportService rdrExportService) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());
      try (BufferedReader reader =
          new BufferedReader(
              new FileReader(opts.getOptionValue(workspaceListFilename.getLongOpt())))) {
        reader
            .lines()
            .forEach(
                line -> {
                  List<Long> workspaceIds =
                      Stream.of(line.split(",")).map(Long::parseLong).collect(Collectors.toList());
                  if (!dryRun) {
                    rdrExportService.deleteWorkspaceExportEntries(workspaceIds);
                  } else {
                    System.out.println(
                        "Dry RUN TRUE: Deleting following workspace Ids from rdr_export");
                    workspaceIds.forEach(System.out::println);
                  }
                });
      }
    };
  }
}
