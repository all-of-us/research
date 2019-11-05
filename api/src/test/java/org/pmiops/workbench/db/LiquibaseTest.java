package org.pmiops.workbench.db;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LiquibaseTest {

  List<String> whitelist = Arrays.asList("db.changelog-master.xml");

  @Test
  public void allChangelogFilesAreInMaster() throws Exception {
    List<String> indexedChangeLogs = getChangeLogIndexes();
    for (File f : new File("db/changelog").listFiles()) {
      if (!whitelist.contains(f.getName()) && !indexedChangeLogs.contains(f.getName())) {
        fail(f.getName() + " is in the db/changelog directory but not in db.changelog-master.xml");
      }
    }
  }

  private List<String> getChangeLogIndexes() throws Exception {
    File fXmlFile = new File("db/changelog/db.changelog-master.xml");
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(fXmlFile);
    doc.getDocumentElement().normalize();

    NodeList changeLogs = doc.getElementsByTagName("include");

    List<String> changeLogFilenames = new ArrayList<>();
    for (int i = 0; i < changeLogs.getLength(); i++) {
      changeLogFilenames.add(((Element) changeLogs.item(i)).getAttribute("file").substring(10));
    }

    return changeLogFilenames;
  }

}
