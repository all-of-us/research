#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export DRY_RUN=$3     # dry run

if [ "$DRY_RUN" == true ]
then
  test=$(bq ls "$BQ_PROJECT:$BQ_DATASET")
  exit 0
fi

# Test that datset exists
test=$(bq show "$BQ_PROJECT:$BQ_DATASET")

################################################
# CREATE LINKING TABLE
################################################
echo "CREATE TABLE - ds_linking"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`
(
    DENORMALIZED_NAME               STRING,
    OMOP_SQL                        STRING,
    JOIN_VALUE                      STRING,
    DOMAIN                          STRING
)"

################################################
# INSERT DATA
################################################
echo "ds_linking - inserting condition data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    # We add the core table for domain row to ensure we have a single place to make certain we load in the base table.
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('PERSON_ID', 'c_occurrence.PERSON_ID', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_CONCEPT_ID', 'c_occurrence.CONDITION_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('STANDARD_CONCEPT_NAME', 'c_standard_concept.concept_name as STANDARD_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` c_standard_concept on c_occurrence.CONDITION_CONCEPT_ID = c_standard_concept.CONCEPT_ID', 'Condition'),
    ('STANDARD_CONCEPT_CODE', 'c_standard_concept.concept_code as STANDARD_CONCEPT_CODE', 'left join \`\${projectId}.\${datasetId}.concept\` c_standard_concept on c_occurrence.CONDITION_CONCEPT_ID = c_standard_concept.CONCEPT_ID', 'Condition'),
    ('STANDARD_VOCABULARY', 'c_standard_concept.vocabulary_id as STANDARD_VOCABULARY', 'left join \`\${projectId}.\${datasetId}.concept\` c_standard_concept on c_occurrence.CONDITION_CONCEPT_ID = c_standard_concept.CONCEPT_ID', 'Condition'),
    ('CONDITION_START_DATETIME', 'c_occurrence.CONDITION_START_DATETIME', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_END_DATETIME', 'c_occurrence.CONDITION_END_DATETIME', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_TYPE_CONCEPT_ID', 'c_occurrence.CONDITION_TYPE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_TYPE_CONCEPT_NAME', 'c_type.concept_name as CONDITION_TYPE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` c_type on c_occurrence.CONDITION_TYPE_CONCEPT_ID = c_type.CONCEPT_ID', 'Condition'),
    ('STOP_REASON', 'c_occurrence.STOP_REASON', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('VISIT_OCCURRENCE_ID', 'c_occurrence.VISIT_OCCURRENCE_ID', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'visit.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.visit_occurrence\` v on c_occurrence.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID left join \`\${projectId}.\${datasetId}.concept\` visit on v.visit_concept_id = visit.concept_id', 'Condition'),
    ('CONDITION_SOURCE_VALUE', 'c_occurrence.CONDITION_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_SOURCE_CONCEPT_ID', 'c_occurrence.CONDITION_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('SOURCE_CONCEPT_NAME', 'c_source_concept.concept_name as SOURCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` c_source_concept on c_occurrence.CONDITION_SOURCE_CONCEPT_ID = c_source_concept.CONCEPT_ID', 'Condition'),
    ('SOURCE_CONCEPT_CODE', 'c_source_concept.concept_code as SOURCE_CONCEPT_CODE', 'left join \`\${projectId}.\${datasetId}.concept\` c_source_concept on c_occurrence.CONDITION_SOURCE_CONCEPT_ID = c_source_concept.CONCEPT_ID', 'Condition'),
    ('SOURCE_VOCABULARY', 'c_source_concept.vocabulary_id as SOURCE_VOCABULARY', 'left join \`\${projectId}.\${datasetId}.concept\` c_source_concept on c_occurrence.CONDITION_SOURCE_CONCEPT_ID = c_source_concept.CONCEPT_ID', 'Condition'),
    ('CONDITION_STATUS_SOURCE_VALUE', 'c_occurrence.CONDITION_STATUS_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_STATUS_CONCEPT_ID', 'c_occurrence.CONDITION_STATUS_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ('CONDITION_STATUS_CONCEPT_NAME', 'c_status.concept_name as CONDITION_STATUS_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` c_status on c_occurrence.CONDITION_STATUS_CONCEPT_ID = c_status.CONCEPT_ID', 'Condition')"

echo "ds_linking - inserting drug exposure data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('PERSON_ID', 'd_exposure.PERSON_ID', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DRUG_CONCEPT_ID', 'd_exposure.DRUG_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('STANDARD_CONCEPT_NAME', 'd_standard_concept.concept_name as STANDARD_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` d_standard_concept on d_exposure.DRUG_CONCEPT_ID = d_standard_concept.CONCEPT_ID', 'Drug'),
    ('STANDARD_CONCEPT_CODE', 'd_standard_concept.concept_code as STANDARD_CONCEPT_CODE', 'left join \`\${projectId}.\${datasetId}.concept\` d_standard_concept on d_exposure.DRUG_CONCEPT_ID = d_standard_concept.CONCEPT_ID', 'Drug'),
    ('STANDARD_VOCABULARY', 'd_standard_concept.vocabulary_id as STANDARD_VOCABULARY', 'left join \`\${projectId}.\${datasetId}.concept\` d_standard_concept on d_exposure.DRUG_CONCEPT_ID = d_standard_concept.CONCEPT_ID', 'Drug'),
    ('DRUG_EXPOSURE_START_DATETIME', 'd_exposure.DRUG_EXPOSURE_START_DATETIME', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DRUG_EXPOSURE_END_DATETIME', 'd_exposure.DRUG_EXPOSURE_END_DATETIME', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('VERBATIM_END_DATE', 'd_exposure.VERBATIM_END_DATE', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DRUG_TYPE_CONCEPT_ID', 'd_exposure.DRUG_TYPE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DRUG_TYPE_CONCEPT_NAME', 'd_type.concept_name as DRUG_TYPE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` d_type on d_exposure.drug_type_concept_id = d_type.CONCEPT_ID', 'Drug'),
    ('STOP_REASON', 'd_exposure.STOP_REASON', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('REFILLS', 'd_exposure.REFILLS', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('QUANTITY', 'd_exposure.QUANTITY', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DAYS_SUPPLY', 'd_exposure.DAYS_SUPPLY', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('SIG', 'd_exposure.SIG', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('ROUTE_CONCEPT_ID', 'd_exposure.ROUTE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('ROUTE_CONCEPT_NAME', 'd_route.concept_name as ROUTE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` d_route on d_exposure.ROUTE_CONCEPT_ID = d_route.CONCEPT_ID', 'Drug'),
    ('LOT_NUMBER', 'd_exposure.LOT_NUMBER', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('VISIT_OCCURRENCE_ID', 'd_exposure.VISIT_OCCURRENCE_ID', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'd_visit.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.visit_occurrence\` v on d_exposure.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID LEFT JOIN \`\${projectId}.\${datasetId}.concept\` d_visit on v.VISIT_CONCEPT_ID = d_visit.CONCEPT_ID', 'Drug'),
    ('DRUG_SOURCE_VALUE', 'd_exposure.DRUG_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DRUG_SOURCE_CONCEPT_ID', 'd_exposure.DRUG_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('SOURCE_CONCEPT_NAME', 'd_source_concept.concept_name as SOURCE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` d_source_concept on d_exposure.DRUG_SOURCE_CONCEPT_ID = d_source_concept.CONCEPT_ID', 'Drug'),
    ('SOURCE_CONCEPT_CODE', 'd_source_concept.concept_code as SOURCE_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` d_source_concept on d_exposure.DRUG_SOURCE_CONCEPT_ID = d_source_concept.CONCEPT_ID', 'Drug'),
    ('SOURCE_VOCABULARY', 'd_source_concept.vocabulary_id as SOURCE_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` d_source_concept on d_exposure.DRUG_SOURCE_CONCEPT_ID = d_source_concept.CONCEPT_ID', 'Drug'),
    ('ROUTE_SOURCE_VALUE', 'd_exposure.ROUTE_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug'),
    ('DOSE_UNIT_SOURCE_VALUE', 'd_exposure.DOSE_UNIT_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.drug_exposure\` d_exposure', 'Drug')"

echo "ds_linking - inserting measurement data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('PERSON_ID', 'measurement.PERSON_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('MEASUREMENT_CONCEPT_ID', 'measurement.MEASUREMENT_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('STANDARD_CONCEPT_NAME', 'm_standard_concept.concept_name as STANDARD_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` m_standard_concept on measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    ('STANDARD_CONCEPT_CODE', 'm_standard_concept.concept_code as STANDARD_CONCEPT_CODE', 'left join \`\${projectId}.\${datasetId}.concept\` m_standard_concept on measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    ('STANDARD_VOCABULARY', 'm_standard_concept.vocabulary_id as STANDARD_VOCABULARY', 'left join \`\${projectId}.\${datasetId}.concept\` m_standard_concept on measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    ('MEASUREMENT_DATETIME', 'measurement.MEASUREMENT_DATETIME', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('MEASUREMENT_TYPE_CONCEPT_ID', 'measurement.MEASUREMENT_TYPE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('MEASUREMENT_TYPE_CONCEPT_NAME', 'm_type.concept_name as MEASUREMENT_TYPE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` m_type on measurement.measurement_type_concept_id = m_type.concept_id', 'Measurement'),
    ('OPERATOR_CONCEPT_ID', 'measurement.OPERATOR_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('OPERATOR_CONCEPT_NAME', 'm_operator.concept_name as OPERATOR_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` m_operator on measurement.operator_concept_id = m_operator.concept_id', 'Measurement'),
    ('VALUE_AS_NUMBER', 'measurement.VALUE_AS_NUMBER', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('VALUE_AS_CONCEPT_ID', 'measurement.VALUE_AS_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('VALUE_AS_CONCEPT_NAME', 'm_value.concept_name as VALUE_AS_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` m_value on measurement.value_as_concept_id = m_value.concept_id', 'Measurement'),
    ('UNIT_CONCEPT_ID', 'measurement.UNIT_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('UNIT_CONCEPT_NAME', 'm_unit.concept_name as UNIT_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` m_unit on measurement.unit_concept_id = m_unit.concept_id', 'Measurement'),
    ('RANGE_LOW', 'measurement.RANGE_LOW', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('RANGE_HIGH', 'measurement.RANGE_HIGH', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('VISIT_OCCURRENCE_ID', 'measurement.VISIT_OCCURRENCE_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'm_visit.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.visit_occurrence\` v on measurement.visit_occurrence_id = v.visit_occurrence_id left join \`\${projectId}.\${datasetId}.concept\` m_visit on v.visit_concept_id = m_visit.concept_id', 'Measurement'),
    ('MEASUREMENT_SOURCE_VALUE', 'measurement.MEASUREMENT_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('MEASUREMENT_SOURCE_CONCEPT_ID', 'measurement.MEASUREMENT_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('SOURCE_CONCEPT_NAME', 'm_source_concept.concept_name as SOURCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` m_source_concept on measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    ('SOURCE_CONCEPT_CODE', 'm_source_concept.concept_code as SOURCE_CONCEPT_CODE', 'left join \`\${projectId}.\${datasetId}.concept\` m_source_concept on measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    ('SOURCE_VOCABULARY', 'm_source_concept.vocabulary_id as SOURCE_VOCABULARY', 'left join \`\${projectId}.\${datasetId}.concept\` m_source_concept on measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    ('UNIT_SOURCE_VALUE', 'measurement.UNIT_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement'),
    ('VALUE_SOURCE_VALUE', 'measurement.VALUE_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.measurement\` measurement', 'Measurement')"

echo "ds_linking - inserting observation data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('PERSON_ID', 'observation.PERSON_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('OBSERVATION_CONCEPT_ID', 'observation.OBSERVATION_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('STANDARD_CONCEPT_NAME', 'o_standard_concept.concept_name as STANDARD_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_standard_concept on observation.OBSERVATION_CONCEPT_ID = o_standard_concept.CONCEPT_ID', 'Observation'),
    ('STANDARD_CONCEPT_CODE', 'o_standard_concept.concept_code as STANDARD_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_standard_concept on observation.OBSERVATION_CONCEPT_ID = o_standard_concept.CONCEPT_ID', 'Observation'),
    ('STANDARD_VOCABULARY', 'o_standard_concept.vocabulary_id as STANDARD_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_standard_concept on observation.OBSERVATION_CONCEPT_ID = o_standard_concept.CONCEPT_ID', 'Observation'),
    ('OBSERVATION_DATETIME', 'observation.OBSERVATION_DATETIME', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('OBSERVATION_TYPE_CONCEPT_ID', 'observation.OBSERVATION_TYPE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('OBSERVATION_TYPE_CONCEPT_NAME', 'o_type.concept_name as OBSERVATION_TYPE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_type on observation.OBSERVATION_TYPE_CONCEPT_ID = o_type.CONCEPT_ID', 'Observation'),
    ('VALUE_AS_NUMBER', 'observation.VALUE_AS_NUMBER', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('VALUE_AS_STRING', 'observation.VALUE_AS_STRING', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('VALUE_AS_CONCEPT_ID', 'observation.VALUE_AS_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('VALUE_AS_CONCEPT_NAME', 'o_value.concept_name as VALUE_AS_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_value on observation.value_as_concept_id = o_value.CONCEPT_ID', 'Observation'),
    ('QUALIFIER_CONCEPT_ID', 'observation.QUALIFIER_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('QUALIFIER_CONCEPT_NAME', 'o_qualifier.concept_name as QUALIFIER_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_qualifier on observation.qualifier_concept_id = o_qualifier.CONCEPT_ID', 'Observation'),
    ('UNIT_CONCEPT_ID', 'observation.UNIT_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('UNIT_CONCEPT_NAME', 'o_unit.concept_name as UNIT_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_unit on observation.unit_concept_id = o_unit.CONCEPT_ID', 'Observation'),
    ('VISIT_OCCURRENCE_ID', 'observation.VISIT_OCCURRENCE_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'o_visit.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.visit_occurrence\` v on observation.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID left join \`\${projectId}.\${datasetId}.concept\` o_visit on v.visit_concept_id = o_visit.concept_id', 'Observation'),
    ('OBSERVATION_SOURCE_VALUE', 'observation.OBSERVATION_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('OBSERVATION_SOURCE_CONCEPT_ID', 'observation.OBSERVATION_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('SOURCE_CONCEPT_NAME', 'o_source_concept.concept_name as SOURCE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_source_concept on observation.OBSERVATION_SOURCE_CONCEPT_ID = o_source_concept.CONCEPT_ID', 'Observation'),
    ('SOURCE_CONCEPT_CODE', 'o_source_concept.concept_code as SOURCE_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_source_concept on observation.OBSERVATION_SOURCE_CONCEPT_ID = o_source_concept.CONCEPT_ID', 'Observation'),
    ('SOURCE_VOCABULARY', 'o_source_concept.vocabulary_id as SOURCE_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` o_source_concept on observation.OBSERVATION_SOURCE_CONCEPT_ID = o_source_concept.CONCEPT_ID', 'Observation'),
    ('UNIT_SOURCE_VALUE', 'observation.UNIT_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('QUALIFIER_SOURCE_VALUE', 'observation.QUALIFIER_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('value_source_concept_id', 'observation.value_source_concept_id', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('value_source_value', 'observation.value_source_value', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation'),
    ('questionnaire_response_id', 'observation.questionnaire_response_id', 'from \`\${projectId}.\${datasetId}.ds_observation\` observation', 'Observation')"


echo "ds_linking - inserting person data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('PERSON_ID', 'person.PERSON_ID', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('GENDER_CONCEPT_ID', 'person.GENDER_CONCEPT_ID', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('GENDER', 'p_gender_concept.concept_name as GENDER', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_gender_concept on person.gender_concept_id = p_gender_concept.CONCEPT_ID', 'Person'),
    ('DATE_OF_BIRTH', 'person.BIRTH_DATETIME as DATE_OF_BIRTH', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('RACE_CONCEPT_ID', 'person.RACE_CONCEPT_ID', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('RACE', 'p_race_concept.concept_name as RACE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_race_concept on person.race_concept_id = p_race_concept.CONCEPT_ID', 'Person'),
    ('ETHNICITY_CONCEPT_ID', 'person.ETHNICITY_CONCEPT_ID', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('ETHNICITY', 'p_ethnicity_concept.concept_name as ETHNICITY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_ethnicity_concept on person.ethnicity_concept_id = p_ethnicity_concept.CONCEPT_ID', 'Person'),
    ('SEX_AT_BIRTH_CONCEPT_ID', 'person.SEX_AT_BIRTH_CONCEPT_ID', 'FROM \`\${projectId}.\${datasetId}.person\` person', 'Person'),
    ('SEX_AT_BIRTH', 'p_sex_at_birth_concept.concept_name as SEX_AT_BIRTH', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_sex_at_birth_concept on person.sex_at_birth_concept_id = p_sex_at_birth_concept.CONCEPT_ID', 'Person')"

echo "ds_linking - inserting procedure data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('PERSON_ID', 'procedure.PERSON_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('PROCEDURE_CONCEPT_ID', 'procedure.PROCEDURE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('STANDARD_CONCEPT_NAME', 'p_standard_concept.concept_name as STANDARD_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_standard_concept on procedure.PROCEDURE_CONCEPT_ID = p_standard_concept.CONCEPT_ID', 'Procedure'),
    ('STANDARD_CONCEPT_CODE', 'p_standard_concept.concept_code as STANDARD_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_standard_concept on procedure.PROCEDURE_CONCEPT_ID = p_standard_concept.CONCEPT_ID', 'Procedure'),
    ('STANDARD_VOCABULARY', 'p_standard_concept.vocabulary_id as STANDARD_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_standard_concept on procedure.PROCEDURE_CONCEPT_ID = p_standard_concept.CONCEPT_ID', 'Procedure'),
    ('PROCEDURE_DATETIME', 'procedure.PROCEDURE_DATETIME', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('PROCEDURE_TYPE_CONCEPT_ID', 'procedure.PROCEDURE_TYPE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('PROCEDURE_TYPE_CONCEPT_NAME', 'p_type.concept_name as PROCEDURE_TYPE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_type on procedure.PROCEDURE_TYPE_CONCEPT_ID = p_type.CONCEPT_ID', 'Procedure'),
    ('MODIFIER_CONCEPT_ID', 'procedure.MODIFIER_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('MODIFIER_CONCEPT_NAME', 'p_modifier.concept_name as MODIFIER_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_modifier on procedure.MODIFIER_CONCEPT_ID = p_modifier.CONCEPT_ID', 'Procedure'),
    ('QUANTITY', 'procedure.QUANTITY', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('VISIT_OCCURRENCE_ID', 'procedure.VISIT_OCCURRENCE_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'p_visit.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.visit_occurrence\` v on procedure.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID left join \`\${projectId}.\${datasetId}.concept\` p_visit on v.visit_concept_id = p_visit.concept_id', 'Procedure'),
    ('PROCEDURE_SOURCE_VALUE', 'procedure.PROCEDURE_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('PROCEDURE_SOURCE_CONCEPT_ID', 'procedure.PROCEDURE_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure'),
    ('SOURCE_CONCEPT_NAME', 'p_source_concept.concept_name as SOURCE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_source_concept on procedure.PROCEDURE_SOURCE_CONCEPT_ID = p_source_concept.CONCEPT_ID', 'Procedure'),
    ('SOURCE_CONCEPT_CODE', 'p_source_concept.concept_code as SOURCE_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_source_concept on procedure.PROCEDURE_SOURCE_CONCEPT_ID = p_source_concept.CONCEPT_ID', 'Procedure'),
    ('SOURCE_VOCABULARY', 'p_source_concept.vocabulary_id as SOURCE_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` p_source_concept on procedure.PROCEDURE_SOURCE_CONCEPT_ID = p_source_concept.CONCEPT_ID', 'Procedure'),
    ('QUALIFIER_SOURCE_VALUE', 'procedure.QUALIFIER_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` procedure', 'Procedure')"

echo "ds_linking - inserting survey data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', ' FROM \`\${projectId}.\${datasetId}.ds_survey\` answer', 'Survey'),
    ('PERSON_ID', 'answer.person_id', ' ', 'Survey'),
    ('SURVEY_DATETIME', 'answer.survey_datetime', ' ', 'Survey'),
    ('SURVEY', 'answer.survey', ' ', 'Survey'),
    ('QUESTION_CONCEPT_ID', 'answer.question_concept_id', ' ', 'Survey'),
    ('QUESTION', 'answer.question', ' ', 'Survey'),
    ('ANSWER_CONCEPT_ID', 'answer.answer_concept_id', ' ', 'Survey'),
    ('ANSWER', 'answer.answer', ' ', 'Survey'),
    ('SURVEY_VERSION_CONCEPT_ID', 'answer.survey_version_concept_id', ' ', 'Survey'),
    ('SURVEY_VERSION_NAME', 'answer.survey_version_name', ' ', 'Survey')"

echo "ds_linking - inserting visit data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ('CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('PERSON_ID', 'visit.PERSON_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('VISIT_CONCEPT_ID', 'visit.VISIT_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('STANDARD_CONCEPT_NAME', 'v_standard_concept.concept_name as STANDARD_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_standard_concept on visit.VISIT_CONCEPT_ID = v_standard_concept.CONCEPT_ID', 'Visit'),
    ('STANDARD_CONCEPT_CODE', 'v_standard_concept.concept_code as STANDARD_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_standard_concept on visit.VISIT_CONCEPT_ID = v_standard_concept.CONCEPT_ID', 'Visit'),
    ('STANDARD_VOCABULARY', 'v_standard_concept.vocabulary_id as STANDARD_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_standard_concept on visit.VISIT_CONCEPT_ID = v_standard_concept.CONCEPT_ID', 'Visit'),
    ('VISIT_START_DATETIME', 'visit.VISIT_START_DATETIME', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('VISIT_END_DATETIME', 'visit.VISIT_END_DATETIME', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('VISIT_TYPE_CONCEPT_ID', 'visit.VISIT_TYPE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('VISIT_TYPE_CONCEPT_NAME', 'v_type.concept_name as VISIT_TYPE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_type on visit.VISIT_TYPE_CONCEPT_ID = v_type.CONCEPT_ID', 'Visit'),
    ('VISIT_SOURCE_VALUE', 'visit.VISIT_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('VISIT_SOURCE_CONCEPT_ID', 'visit.VISIT_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('SOURCE_CONCEPT_NAME', 'v_source_concept.concept_name as SOURCE_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_source_concept on visit.VISIT_SOURCE_CONCEPT_ID = v_source_concept.CONCEPT_ID', 'Visit'),
    ('SOURCE_CONCEPT_CODE', 'v_source_concept.concept_code as SOURCE_CONCEPT_CODE', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_source_concept on visit.VISIT_SOURCE_CONCEPT_ID = v_source_concept.CONCEPT_ID', 'Visit'),
    ('SOURCE_VOCABULARY', 'v_source_concept.vocabulary_id as SOURCE_VOCABULARY', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_source_concept on visit.VISIT_SOURCE_CONCEPT_ID = v_source_concept.CONCEPT_ID', 'Visit'),
    ('ADMITTING_SOURCE_CONCEPT_ID', 'visit.ADMITTING_SOURCE_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('ADMITTING_SOURCE_CONCEPT_NAME', 'v_admitting_source_concept.concept_name as ADMITTING_SOURCE_CONCEPT_NAME', 'left join \`\${projectId}.\${datasetId}.concept\` v_admitting_source_concept on visit.ADMITTING_SOURCE_CONCEPT_ID = v_admitting_source_concept.CONCEPT_ID', 'Visit'),
    ('ADMITTING_SOURCE_VALUE', 'visit.ADMITTING_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('DISCHARGE_TO_CONCEPT_ID', 'visit.DISCHARGE_TO_CONCEPT_ID', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit'),
    ('DISCHARGE_TO_CONCEPT_NAME', 'v_discharge.concept_name as DISCHARGE_TO_CONCEPT_NAME', 'LEFT JOIN \`\${projectId}.\${datasetId}.concept\` v_discharge on visit.DISCHARGE_TO_CONCEPT_ID = v_discharge.CONCEPT_ID', 'Visit'),
    ('DISCHARGE_TO_SOURCE_VALUE', 'visit.DISCHARGE_TO_SOURCE_VALUE', 'from \`\${projectId}.\${datasetId}.procedure_occurrence\` visit', 'Visit')"
