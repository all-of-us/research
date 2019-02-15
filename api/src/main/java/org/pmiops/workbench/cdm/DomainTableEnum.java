package org.pmiops.workbench.cdm;

/**
 * This enum maps different domain types to table name and concept id.
 */
public enum DomainTableEnum {
    CONDITION("Condition",
      "condition_occurrence",
      "person_condition",
      "condition_concept_id",
      "condition_source_concept_id",
      "condition_start_date"),
    PROCEDURE("Procedure",
      "procedure_occurrence",
      "person_procedure",
      "procedure_concept_id",
      "procedure_source_concept_id",
      "procedure_date"),
    OBSERVATION("Observation",
      "observation",
      "person_observation",
      "observation_concept_id",
      "observation_source_concept_id",
      "observation_date"),
    MEASUREMENT("Measurement",
      "measurement",
      "person_measurement",
      "measurement_concept_id",
      "measurement_source_concept_id",
      "measurement_date"),
    DRUG("Drug",
      "drug_exposure",
      "person_drug",
      "drug_concept_id",
      "drug_source_concept_id",
      "drug_exposure_start_date"),
    DEVICE("Device",
      "device_exposure",
      "person_device",
      "device_concept_id",
      "device_source_concept_id",
      "device_exposure_start_date"),
    VISIT("Visit",
      "visit_occurrence",
      "person_visit",
      "visit_concept_id",
      "visit_source_concept_id",
      "visit_start_date");

    private String domainId;
    private String tableName;
    private String denormalizedTableName;
    private String sourceConceptId;
    private String conceptId;
    private String entryDate;

    private DomainTableEnum(String domainId,
                            String tableName,
                            String denormalizedTableName,
                            String conceptId,
                            String sourceConceptId,
                            String entryDate) {
        this.domainId = domainId;
        this.tableName = tableName;
        this.denormalizedTableName = denormalizedTableName;
        this.conceptId = conceptId;
        this.sourceConceptId = sourceConceptId;
        this.entryDate = entryDate;
    }

    public static String getTableName(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equalsIgnoreCase(domainId)) {
                return item.tableName;
            }
        }
        return null;
    }

    public static String getDenormalizedTableName(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equalsIgnoreCase(domainId)) {
                return item.denormalizedTableName;
            }
        }
        return null;
    }

    public static String getConceptId(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equalsIgnoreCase(domainId)) {
                return item.conceptId;
            }
        }
        return null;
    }

    public static String getSourceConceptId(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equalsIgnoreCase(domainId)) {
                return item.sourceConceptId;
            }
        }
        return null;
    }

    public static String getEntryDate(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equalsIgnoreCase(domainId)) {
                return item.entryDate;
            }
        }
        return null;
    }
}
