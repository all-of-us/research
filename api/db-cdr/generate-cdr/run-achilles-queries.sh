#!/bin/bash

# This generates cloudsql  database from cdr dump
# note  the account must be authorized to perform gcloud and bq operations

set -xeuo pipefail
IFS=$'\n\t'

# Init vars
ACCOUNT=""
BQ_PROJECT=""
WORKBENCH_PROJECT=""
BQ_DATASET=""
WORKBENCH_DATASET=""

USAGE="./generate-clousql-cdr/run-achilles-queries.sh --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --account <ACCOUNT> --cdr-version=YYYYMMDD"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
#TODO THESE FAIL WITHOUT PRINTING USAGE IF ARG ISN'T PASSED IN
# I TRY TO PREVENT IT UP TOP BY INITING VARS BUT NO WORK
if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


# TODO Next Populate achilles_results
echo "Running achilles queries..."

echo "Getting person count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, count_value) select 0 as id, 1 as analysis_id,  COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`"


# Gender count
echo "Getting gender count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value)
select 0, 2 as analysis_id,  cast (gender_concept_id as STRING) as stratum_1, COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by GENDER_CONCEPT_ID"

# Age count
# 3	Number of persons by year of birth
echo "Getting age count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value)
select 0, 3 as analysis_id,  CAST(year_of_birth AS STRING) as stratum_1, COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by YEAR_OF_BIRTH"


#  4	Number of persons by race
echo "Getting race count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value)
select 0, 4 as analysis_id,  CAST(RACE_CONCEPT_ID AS STRING) as stratum_1, COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by RACE_CONCEPT_ID"

# 5	Number of persons by ethnicity
echo "Getting ethnicity count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value)
select 0, 5 as analysis_id,  CAST(ETHNICITY_CONCEPT_ID AS STRING) as stratum_1, COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by ETHNICITY_CONCEPT_ID"

# 10	Number of all persons by year of birth and by gender
echo "Getting year of birth , gender count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 10 as analysis_id,  CAST(year_of_birth AS STRING) as stratum_1,
  CAST(gender_concept_id AS STRING) as stratum_2,
  COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by YEAR_OF_BIRTH, gender_concept_id"

# 12	Number of persons by race and ethnicity
echo "Getting race, ethnicity count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 12 as analysis_id, CAST(RACE_CONCEPT_ID AS STRING) as stratum_1, CAST(ETHNICITY_CONCEPT_ID AS STRING) as stratum_2, COUNT(distinct person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by RACE_CONCEPT_ID,ETHNICITY_CONCEPT_ID"

# 200	(3000 ) Number of persons with at least one visit occurrence, by visit_concept_id
echo "Getting visit count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select 0, 3000 as analysis_id,
	CAST(vo1.visit_concept_id AS STRING) as stratum_1,
	COUNT(distinct vo1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo1
group by vo1.visit_concept_id"

# Condition gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.condition_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
group by co1.condition_concept_id, p1.gender_concept_id"

# 400 (3000)	Number of persons with at least one condition occurrence, by condition_concept_id
echo "Querying condition_occurrence ..."
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select 0, 3000 as analysis_id,
	CAST(co1.condition_CONCEPT_ID AS STRING) as stratum_1,
	COUNT(distinct co1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
group by co1.condition_CONCEPT_ID"

# Condition gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.condition_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
group by co1.condition_concept_id, p1.gender_concept_id"

# (400 age ) 3102 Number of persons with at least one condition occurrence, by condition_concept_id by age decile
# Age Deciles : They will be 18 - 29, 30 - 39, 40 - 49, 50 - 59, 60 - 69, 70 - 79, 80-89, 90+
#  children are 0-17 and we don't have children for now . Want all adults in a bucket thus 18 - 29 .
#Ex yob = 2000  , start date : 2017 -- , sd - yob = 17  / 10 = 1.7 floor(1.7 ) = 1
# 30 - 39 , 2017 - 1980 = 37 / 10 = 3


# Get the 30-39, 40 - 49 , ... groups
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.condition_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from condition_start_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
  count(distinct p1.person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
where floor((extract(year from condition_start_date) - p1.year_of_birth)/10) >=3
group by co1.condition_concept_id, stratum_2"

#Get conditions by age decile id 3102 for the 18-29 group labeled as 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.condition_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,
  count(distinct p1.person_id) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
where (extract(year from condition_start_date) - p1.year_of_birth) > 18 and (extract(year from condition_start_date) - p1.year_of_birth) < 30
group by co1.condition_concept_id, stratum_2"


# 500	(3000) Number of persons with death, by cause_concept_id
echo "Querying death ..."
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select 0, 3000 as analysis_id,
	CAST(d1.cause_concept_id AS STRING) as stratum_1,
	COUNT(distinct d1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.death\` d1
group by d1.cause_concept_id"

# Death (3101)	Number of persons with a death by death cause concept id by  gender concept id
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.cause_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.death\` co1
on p1.person_id = co1.person_id
group by co1.cause_concept_id,
	p1.gender_concept_id"

# Death (3102)	Number of persons with a death by death cause concept id by  age decile  30+ yr old deciles */
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.cause_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.death_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.death\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.death_date) - p1.year_of_birth)/10) >=3
group by co1.cause_concept_id,
	stratum_2"

# Death (3102)	Number of persons with a death by death cause concept id by  age decile  18-29 yr old decile 2 */
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id, CAST(co1.cause_concept_id AS STRING) as stratum_1, '2' as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.death\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.death_date) - p1.year_of_birth) >= 18 and (extract(year from co1.death_date) - p1.year_of_birth) < 30
group by co1.cause_concept_id,
	stratum_2"

# 600	Number of persons with at least one procedure occurrence, by procedure_concept_id
echo "Querying procedure_occurrence"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select 0, 3000 as analysis_id,
	CAST(po1.procedure_CONCEPT_ID AS STRING) as stratum_1,
	COUNT(distinct po1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` po1
group by po1.procedure_CONCEPT_ID"

#  600 Gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.procedure_CONCEPT_ID AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
group by co1.procedure_concept_id,
	p1.gender_concept_id"


# 600 age
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.procedure_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.procedure_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.procedure_date) - p1.year_of_birth)/10) >=3
group by co1.procedure_concept_id, stratum_2"

# 600 age 18 to 29
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.procedure_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.procedure_date) - p1.year_of_birth) >= 18 and
(extract(year from co1.procedure_date) - p1.year_of_birth) < 30
group by co1.procedure_concept_id, stratum_2"

# Drugs
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select 0, 3000 as analysis_id,
	CAST(de1.drug_CONCEPT_ID AS STRING) as stratum_1,
	COUNT(distinct de1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` de1
group by de1.drug_CONCEPT_ID"

# Drug gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.drug_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
group by co1.drug_concept_id,
	p1.gender_concept_id"

# Drug age
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.drug_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.drug_exposure_start_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.drug_exposure_start_date) - p1.year_of_birth)/10) >=3
group by co1.drug_concept_id, stratum_2"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.drug_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.drug_exposure_start_date) - p1.year_of_birth) >= 18 and
(extract(year from co1.drug_exposure_start_date) - p1.year_of_birth) < 30
group by co1.drug_concept_id, stratum_2"

# 800	(3000) Number of persons with at least one observation occurrence, by observation_concept_id
echo "Querying observation"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select 0, 3000 as analysis_id,
	CAST(co1.observation_CONCEPT_ID AS STRING) as stratum_1,
	COUNT(distinct co1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
where co1.observation_concept_id > 0
group by co1.observation_CONCEPT_ID"

# Observation 3101 concept by gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.observation_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0
group by co1.observation_concept_id, p1.gender_concept_id"

# Observation (3102)	Number of persons with   concept id by  age decile  30+ yr old deciles
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.observation_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.observation_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0 and floor((extract(year from co1.observation_date) - p1.year_of_birth)/10) >=3
group by co1.observation_concept_id, stratum_2"

# Observation (3102)	Number of persons with concept id by  age decile  18-29 yr old decile 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.observation_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0 and (extract(year from co1.observation_date) - p1.year_of_birth) >= 18 and (extract(year from co1.observation_date) - p1.year_of_birth) < 30
group by co1.observation_concept_id, stratum_2"

# PPI Observation (3000)
# Todo , we co count > 0 to eliminate all the junk data now
# Note, we only want counts related to 3 survery modules which have concept id
# (1586134, 1585855,1855710)
echo "Querying PPI observation"
# Get ones with value as string

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
SELECT 0, 3000 as analysis_id, CAST(c.concept_id AS STRING) as stratum_1,
value_as_string as stratum_2, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on co1.observation_source_concept_id = c.concept_id
inner join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` r
on r.concept_id_2 = c.concept_id
where r.concept_id_1 in (1586134, 1585855,1855710) and value_as_string is not null
group by c.concept_id, value_as_string"

# Get ones with value as number.
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
SELECT 0, 3000 as analysis_id, CAST(c.concept_id AS STRING) as stratum_1,
CAST(value_as_number as STRING) as stratum_2, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on co1.observation_source_concept_id = c.concept_id
inner join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` r
on r.concept_id_2 = c.concept_id
where r.concept_id_1 in(1586134, 1585855,1855710) and value_as_number is not null
group by c.concept_id, value_as_number"

# Todo maybe for real data None were in test data
# Get ones with value as concept_id .

# 1800 Measurements - Number of persons with at least one measurement occurrence, by measurement_concept_id
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, count_value)
select  3000 as analysis_id, CAST(m.measurement_concept_id  AS STRING) as stratum_1, COUNT(distinct m.person_id) as count_value
  from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
 where co1.measurement_concept_id > 0
 group by  co1.measurement_concept_id"

# Measurement concept by gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3101 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_concept_id > 0
group by co1.measurement_concept_id, p1.gender_concept_id"

# Measurement by age deciles
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_concept_id > 0 and floor((extract(year from co1.measurement_date) - p1.year_of_birth)/10) >=3
group by co1.measurement_concept_id, stratum_2"

# Measurement  18-29 yr old decile 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value)
select 0, 3102 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,
	COUNT(distinct p1.PERSON_ID) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_concept_id > 0 and (extract(year from co1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from co1.measurement_date) - p1.year_of_birth) < 30
group by co1.measurement_concept_id, stratum_2"

# Measurement Distributions
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value, p75_value, p90_value)
WITH rawdata_1815 AS
(SELECT measurement_concept_id as subject_id, unit_concept_id, cast(value_as_number  as float64) as count_value
  FROM  \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m
where m.unit_concept_id is not null
	and m.value_as_number is not null),
overallstats  as ( select  subject_id  as stratum1_id, unit_concept_id  as stratum2_id, cast(avg(1.0 * count_value)  as float64)  as avg_value, cast(STDDEV(count_value)  as float64)  as stdev_value, min(count_value)  as min_value, max(count_value)  as max_value, COUNT(*)  as total   from  rawdata_1815
	 group by  1, 2 ), statsview  as ( select  subject_id  as stratum1_id, unit_concept_id  as stratum2_id, count_value as count_value, COUNT(*)  as total, row_number() over (partition by subject_id, unit_concept_id order by count_value)  as rn   from  rawdata_1815
   group by  1, 2, 3 ), priorstats  as ( select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total)  as accumulated   from  statsview s
  join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
 )
select  0 as id, 3115 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id, o.total as count_value, o.min_value, o.max_value, o.avg_value, o.stdev_value, min(case when p.accumulated >= .50 * o.total then count_value else o.max_value end) as median_value, min(case when p.accumulated >= .10 * o.total then count_value else o.max_value end) as p10_value, min(case when p.accumulated >= .25 * o.total then count_value else o.max_value end) as p25_value, min(case when p.accumulated >= .75 * o.total then count_value else o.max_value end) as p75_value, min(case when p.accumulated >= .90 * o.total then count_value else o.max_value end) as p90_value
  FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
 group by  o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"