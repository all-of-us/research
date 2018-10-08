#!/bin/bash

# Runs measurement queries to populate count db of measurement data for cloudsql in BigQuery
set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-clousql-cdr/run-measurement-queries.sh --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --cdr-version=YYYYMMDD"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

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

# Next Populate achilles_results
echo "Running measurement queries..."

# 3000 Measurements that have numeric values - Number of persons with at least one measurement occurrence by measurement_concept_id, bin size of the measurement value for 10 bins, maximum and minimum from measurement value. Added value for measurement rows
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
  'Measurement' as stratum_3,
	COUNT(distinct co1.PERSON_ID) as count_value, (select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2
	where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_concept_id > 0
group by co1.measurement_concept_id
union all
 select 0 as id,3000 as analysis_id,CAST(co1.measurement_source_concept_id AS STRING) as stratum_1,
  'Measurement' as stratum_3,
 COUNT(distinct co1.PERSON_ID) as count_value,COUNT(distinct co1.PERSON_ID) as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
 where co1.measurement_concept_id != co1.measurement_source_concept_id
 group by co1.measurement_source_concept_id"

# 1815 Measurement response by gender distribution
echo "Getting measurement response by gender distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_1815 as
(select measurement_concept_id as subject_id, cast(unit_concept_id as string) as unit, p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and unit_concept_id != 0
union all
select measurement_source_concept_id as subject_id, cast(unit_concept_id as string) as unit,p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and unit_concept_id != 0
union all
select measurement_concept_id as subject_id, lower(unit_source_value) as unit, p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_source_concept_id as subject_id, lower(unit_source_value) as unit,p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_concept_id as subject_id, '' as unit, p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and unit_concept_id = 0 and unit_source_value is null
union all
select measurement_source_concept_id as subject_id, '' as unit,p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and unit_concept_id = 0 and unit_source_value is null
),
overallstats as
(select subject_id as stratum1_id, unit as stratum2_id, gender as stratum3_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1815 group by 1,2,3
),
statsview as
(select subject_id as stratum1_id, unit as stratum2_id, gender as stratum3_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,unit,gender order by count_value) as rn from rawdata_1815 group by 1,2,3,4
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.stratum3_id as stratum3_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and s.stratum3_id = p.stratum3_id
and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.stratum3_id, s.count_value, s.total, s.rn
)
select 0 as id, 1815 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,CAST(o.stratum3_id  AS STRING) as stratum3_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id and p.stratum3_id = o.stratum3_id
group by o.stratum1_id, o.stratum2_id, o.stratum3_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1814 Measurement response distribution
echo "Getting measurement response distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_2,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_1814 as
(select measurement_concept_id as subject_id, cast(unit_concept_id as string) as unit,cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and unit_concept_id != 0
union all
select measurement_source_concept_id as subject_id, cast(unit_concept_id as string) as unit,cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and unit_concept_id != 0
union all
select measurement_concept_id as subject_id, lower(unit_source_value) as unit,cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_source_concept_id as subject_id, lower(unit_source_value) as unit,cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_concept_id as subject_id, '' as unit,cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and unit_concept_id = 0 and unit_source_value is null
union all
select measurement_source_concept_id as subject_id, '' as unit,cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and unit_concept_id = 0 and unit_source_value is null
),
overallstats as
(select subject_id as stratum1_id, unit as stratum2_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1814 group by 1,2
),
statsview as
(select subject_id as stratum1_id,unit as stratum2_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,unit order by count_value) as rn from rawdata_1814 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id
and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
)
select 0 as id, 1814 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1816 Measurement response by age at occurrence distribution
echo "Getting Measurement response by age at occurrence distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_1816 as
(select measurement_concept_id as subject_id, CAST(unit_concept_id as string) as unit, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and unit_concept_id != 0
union all
select measurement_source_concept_id as subject_id, CAST(unit_concept_id as string) as unit, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and unit_concept_id != 0
union all
select measurement_concept_id as subject_id, CAST(unit_concept_id as string) as unit, '2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and unit_concept_id != 0
union all
select measurement_source_concept_id as subject_id, CAST(unit_concept_id as string) as unit,'2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and unit_concept_id != 0
union all
select measurement_concept_id as subject_id, lower(unit_source_value) as unit, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_source_concept_id as subject_id, lower(unit_source_value) as unit, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_concept_id as subject_id, lower(unit_source_value) as unit, '2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_source_concept_id as subject_id, lower(unit_source_value) as unit,'2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and unit_concept_id = 0 and unit_source_value is not null
union all
select measurement_concept_id as subject_id, '' as unit, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and unit_concept_id = 0 and unit_source_value is null
union all
select measurement_source_concept_id as subject_id, '' as unit, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and unit_concept_id = 0 and unit_source_value is null
union all
select measurement_concept_id as subject_id, '' as unit, '2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_concept_id != 0 and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and unit_concept_id = 0 and unit_source_value is null
union all
select measurement_source_concept_id as subject_id, '' as unit,'2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and unit_concept_id = 0 and unit_source_value is null

),
overallstats as
(select subject_id as stratum1_id, unit as stratum2_id, age_decile as stratum3_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1816 group by 1,2,3
),
statsview as
(select subject_id as stratum1_id, unit as stratum2_id, age_decile as stratum3_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id, unit, age_decile order by count_value) as rn from rawdata_1816 group by 1,2,3,4
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.stratum3_id as stratum3_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and s.stratum3_id = p.stratum3_id and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.stratum3_id, s.count_value, s.total, s.rn
)
select 0 as id, 1816 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum2_id  AS STRING) as stratum2_id, CAST(o.stratum3_id  AS STRING) as stratum3_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id and p.stratum3_id = o.stratum3_id
group by o.stratum1_id, o.stratum2_id, o.stratum3_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
"

# 3005 (Distribution of counts per person by measurement_concept_id), 3006 (Distribution of counts per person by measurement_concept_id, gender), 3007 (Distribution of counts per person by measurement_concept_id and age at occurrence decile), 3008 (Distribution of counts per person by measurement_concept_id, gender, age at occurrence decile)
echo "Getting distribution of counts per person for each measurement concept that has unique unit"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"
insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_3005 as
(select m.measurement_concept_id as subject_id, p.gender_concept_id as gender,
CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.measurement_concept_id != 0 and (m.value_as_number is not null or m.value_as_concept_id != 0)
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
group by m.measurement_concept_id,m.measurement_datetime,2,3,m.person_id
union all
select m.measurement_source_concept_id as subject_id, p.gender_concept_id as gender,
CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.measurement_source_concept_id != 0 and (m.value_as_number is not null or m.value_as_concept_id != 0)
and m.measurement_source_concept_id != m.measurement_concept_id
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
group by m.measurement_source_concept_id,m.measurement_datetime,2,3,m.person_id
union all
select m.measurement_concept_id as subject_id, p.gender_concept_id as gender,
'2' as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.measurement_concept_id = 3025315 and (m.value_as_number is not null or m.value_as_concept_id != 0)
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
group by m.measurement_concept_id,m.measurement_datetime,2,3,m.person_id
union all
select m.measurement_source_concept_id as subject_id, p.gender_concept_id as gender,
'2' as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
where m.measurement_concept_id = 3025315 and (m.value_as_number is not null or m.value_as_concept_id != 0)
and m.measurement_concept_id != m.measurement_source_concept_id
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
group by m.measurement_source_concept_id,m.measurement_datetime,2,3,m.person_id
),
overallstats_a as
(select subject_id as stratum1_id,age_decile as stratum2_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2
),
statsview_a as
(select subject_id as stratum1_id, age_decile as stratum2_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id, age_decile order by count_value) as rn from rawdata_3005 group by 1,2,3
),
priorstats_a as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id,
s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview_a s
  join statsview_a p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id  and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
),
age_dist as
(
select 0 as id, 3007 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum2_id  AS STRING) as stratum2_id,
'' as stratum3_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats_a p
join overallstats_a o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
overallstats_g as
(select subject_id as stratum1_id,gender as stratum2_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2
),
statsview_g as
(select subject_id as stratum1_id, gender as stratum2_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,gender order by count_value) as rn from rawdata_3005 group by 1,2,3
),
priorstats_g as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id,
s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview_g s
  join statsview_g p on s.stratum1_id = p.stratum1_id  and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
),
gender_dist as
(
select 0 as id, 3006 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum2_id  AS STRING) as stratum2_id,
'' as stratum3_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats_g p
join overallstats_g o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
overallstats as
(select subject_id as stratum1_id,'' as stratum2_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2
),
statsview as
(select subject_id as stratum1_id, '' as stratum2_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id order by count_value) as rn from rawdata_3005 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
  join statsview p on s.stratum1_id = p.stratum1_id  and p.rn <= s.rn
   group by  s.stratum1_id, s.count_value, s.total, s.rn
),
dist as
(
select 0 as id, 3005 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,'' as stratum2_id,'' as stratum3_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
overallstats_a_g as
(select subject_id as stratum1_id,age_decile as stratum2_id,gender as stratum3_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2,3
),
statsview_a_g as
(select subject_id as stratum1_id, age_decile as stratum2_id, gender as stratum3_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,age_decile,gender order by count_value) as rn from rawdata_3005 group by 1,2,3,4
),
priorstats_a_g as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.stratum3_id as stratum3_id,
s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview_a_g s
  join statsview_a_g p on s.stratum1_id = p.stratum1_id  and s.stratum2_id = p.stratum2_id  and s.stratum3_id = p.stratum3_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.stratum3_id, s.count_value, s.total, s.rn
),
age_gender_dist as
(
select 0 as id, 3008 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
CAST(o.stratum3_id  AS STRING) as stratum1_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats_a_g p
join overallstats_a_g o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id and p.stratum3_id = o.stratum3_id
group by o.stratum1_id, o.stratum2_id, o.stratum3_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
results as
(select * from age_dist
union all
select * from gender_dist
union all
select * from dist
union all
select * from age_gender_dist)
select id,analysis_id,stratum1_id,stratum2_id,stratum3_id,cast(total as int64),min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value from results
"

# 1806 Measurement age by gender distribution
echo "Getting measurement age by gender distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value, p75_value, p90_value)
WITH rawdata_1806 AS
(SELECT o1.measurement_concept_id as subject_id, p1.gender_concept_id, o1.measurement_start_year - p1.year_of_birth as count_value
FROM  \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
(select  person_id, measurement_concept_id, min(EXTRACT(YEAR from measurement_date)) as measurement_start_year from  \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` group by  1, 2 ) o1
on p1.person_id = o1.person_id),
overallstats  as
( select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, cast(avg(1.0 * count_value)  as float64)  as avg_value,
cast(STDDEV(count_value)  as float64)  as stdev_value, min(count_value)  as min_value, max(count_value)  as max_value,
COUNT(*)  as total   from  rawdata_1806 group by  1, 2 ),
statsview  as
(select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, count_value as count_value, COUNT(*)  as total, row_number() over (partition by subject_id, gender_concept_id order by count_value)  as rn   from  rawdata_1806
 group by  1, 2, 3 ),
priorstats  as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total)  as accumulated   from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn)
select 0 as id, 1806 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by  o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1806 Measurement response age distribution (source concepts)
echo "Getting measurement response age distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value, p75_value, p90_value)
WITH rawdata_1806 AS
(SELECT o1.measurement_source_concept_id as subject_id, p1.gender_concept_id, o1.measurement_start_year - p1.year_of_birth as count_value
  FROM  \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1
inner join
(select  person_id, measurement_source_concept_id, min(EXTRACT(YEAR from measurement_date)) as measurement_start_year from  \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`
where measurement_concept_id != measurement_source_concept_id group by  1, 2 ) o1
on p1.person_id = o1.person_id),
overallstats  as ( select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, cast(avg(1.0 * count_value)  as float64)  as avg_value, cast(STDDEV(count_value)  as float64)  as stdev_value, min(count_value)  as min_value, max(count_value)  as max_value, COUNT(*)  as total   from  rawdata_1806 group by  1, 2 ),
statsview  as ( select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, count_value as count_value, COUNT(*)  as total, row_number() over (partition by subject_id, gender_concept_id order by count_value)  as rn   from  rawdata_1806
 group by  1, 2, 3 ),
priorstats  as ( select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total)  as accumulated   from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn)
select 0 as id, 1806 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by  o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# Update iqr_min and iqr_max in distributions for debugging purposes
echo "updating iqr_min and iqr_max"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
set stratum_4 = cast((case when (p25_value - 1.5*(p75_value-p25_value)) > min_value then (p25_value - 1.5*(p75_value-p25_value)) else min_value end) as string),
stratum_5 = cast((case when (p75_value + 1.5*(p75_value-p25_value)) < max_value then (p75_value + 1.5*(p75_value-p25_value)) else max_value end) as string)
where analysis_id in (1814,1815,1816)"

# Update iqr_min and iqr_max in distributions for debugging purposes
echo "updating iqr_min and iqr_max"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
set stratum_4 = cast(p10_value as string),
stratum_5 = cast(p90_value as string)
where analysis_id in (1814,1815,1816)
and stratum_4=stratum_5"

# 1900 Measurement numeric value counts (This query generates counts, source counts of the binned value and gender combination. It gets bin size from joining the achilles_results)
# We do net yet generate the binned source counts of standard concepts
echo "Getting measurements binned gender value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,count_value,source_count_value)
with measurement_quartile_data as
(
select cast(stratum_1 as int64) as concept,stratum_2 as unit,cast(stratum_3 as int64)as gender,cast(stratum_4 as float64) as iqr_min,cast(stratum_5 as float64) as iqr_max,min_value,max_value,p10_value,p25_value,p75_value,p90_value,
((cast(stratum_5 as float64)-cast(stratum_4 as float64))/11) as bin_width from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\` where analysis_id=1815
)
select 0 as id,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
unit as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
      when m1.value_as_number > iqr_max then iqr_max
      when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
      when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
      when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
      when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
      when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
      when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
      when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
      when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
      when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
      when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
      else iqr_max
     end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
      when m1.value_as_number > p90_value then p90_value
      when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
      else p90_value
     end),2)
     end) as string) as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join measurement_quartile_data on m1.measurement_concept_id=concept
where m1.measurement_concept_id != 0
and m1.value_as_number is not null and p1.gender_concept_id=gender and (cast(m1.unit_concept_id as string)=unit or lower(m1.unit_source_value)=unit)
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4
union all
select 0 as id, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
unit as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
      when m1.value_as_number > iqr_max then iqr_max
      when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
      when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
      when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
      when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
      when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
      when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
      when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
      when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
      when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
      when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
      else iqr_max
     end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
      when m1.value_as_number > p90_value then p90_value
      when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
      else p90_value
     end),2)
     end) as string) as stratum_4,
COUNT(distinct p1.PERSON_ID) as count_value, COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join measurement_quartile_data on m1.measurement_concept_id=concept
where m1.measurement_source_concept_id != 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_as_number is not null and p1.gender_concept_id=gender and (cast(m1.unit_concept_id as string)=unit or lower(m1.unit_source_value)=unit)
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4
"

# 1900 Measurement string value counts (This query generates counts, source counts of the value and gender combination. It gets bin size from joining the achilles_results)
# We do not yet generate the source counts of standard concepts
echo "Getting measurements unbinned gender value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1,stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
SELECT 0,1900 as analysis_id,
cast(m1.measurement_concept_id as string) as stratum_1,CAST(p1.gender_concept_id AS STRING) as stratum_2,'' as stratum_3,
c2.concept_name as stratum_4,
cast(m1.value_as_concept_id as string) as stratum_5,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c2 on c2.concept_id=m1.value_as_concept_id
where m1.value_as_concept_id != 0
and m1.measurement_concept_id > 0
group by m1.measurement_concept_id,c2.concept_name,p1.gender_concept_id,m1.value_as_concept_id
union all
SELECT 0,1900 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,CAST(p1.gender_concept_id AS STRING) as stratum_2,'' as stratum_3,
c2.concept_name as stratum_4,
cast(m1.value_as_concept_id as string) as stratum_5,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c2 on c2.concept_id=m1.value_as_concept_id
where m1.value_as_concept_id != 0
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,c2.concept_name,p1.gender_concept_id,m1.unit_concept_id,m1.value_as_concept_id"

# 1901 Measurement response, age decile histogram data (age decile > 2)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurement response, age decile histogram data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,count_value,source_count_value)
with measurement_quartile_data as
(
select cast(stratum_1 as int64) as concept,stratum_2 as unit,cast(stratum_3 as int64)as age_decile,cast(stratum_4 as float64) as iqr_min,cast(stratum_5 as float64) as iqr_max,min_value,max_value,p10_value,p25_value,p75_value,p90_value,
((cast(stratum_5 as float64)-cast(stratum_4 as float64))/11) as bin_width from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\` where analysis_id=1816
)
select 0,1901 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
unit as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_concept_id=ar.concept
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and (cast(m1.unit_concept_id as string)=unit or lower(m1.unit_source_value)=unit)
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4
union all
select 0, 1901 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
unit as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
COUNT(distinct p1.PERSON_ID) as count_value, COUNT(distinct p1.PERSON_ID) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_source_concept_id=ar.concept
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and (cast(m1.unit_concept_id as string)=unit or lower(m1.unit_source_value)=unit)
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4"


# 1901 Measurement response, age decile histogram data (age decile = 2)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurement response, age decile histogram data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,count_value,source_count_value)
with measurement_quartile_data as
(
select cast(stratum_1 as int64) as concept,stratum_2 as unit,cast(stratum_3 as int64)as age_decile,cast(stratum_4 as float64) as iqr_min,cast(stratum_5 as float64) as iqr_max,min_value,max_value,p10_value,p25_value,p75_value,p90_value,
((cast(stratum_5 as float64)-cast(stratum_4 as float64))/11) as bin_width from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\` where analysis_id=1816
)
select 0,1901 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
'2' as stratum_2,
unit as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_concept_id=ar.concept
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and (cast(m1.unit_concept_id as string)=unit or lower(m1.unit_source_value)=unit)
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4
union all
select 0, 1901 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
'2' as stratum_2,
unit as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
COUNT(distinct p1.PERSON_ID) as count_value, COUNT(distinct p1.PERSON_ID) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_source_concept_id=ar.concept
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and (cast(m1.unit_concept_id as string)=unit or lower(m1.unit_source_value)=unit)
and m1.value_as_number is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4"

# 1901 Measurement response, age decile histogram data (For concepts that have text values)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurement response, age decile histogram data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value,source_count_value)
SELECT 0,1901 as analysis_id,
cast(m1.measurement_concept_id as string) as stratum_1,CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'' as stratum_3,
c2.concept_name as stratum_4,
cast(m1.value_as_concept_id as string) as stratum_5,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c2 on c2.concept_id=m1.value_as_concept_id
where m1.value_as_concept_id != 0
and m1.measurement_concept_id > 0
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_concept_id,c2.concept_name,m1.value_as_concept_id,stratum_2
union all
SELECT 0,1901 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'' as stratum_3,
c2.concept_name as stratum_4,
cast(m1.value_as_concept_id as string) as stratum_5,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c2 on c2.concept_id=m1.value_as_concept_id
where m1.value_as_concept_id != 0
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,c2.concept_name,m1.value_as_concept_id,stratum_2"

#1901 Measurement string value counts (This query generates counts, source counts of the value and age decile 2. It gets bin size from joining the achilles_results)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurements unbinned age decile 2 value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5,count_value,source_count_value)
SELECT 0,1901 as analysis_id,
cast(m1.measurement_concept_id as string) as stratum_1,'2' as stratum_2,'' as stratum_3,
c2.concept_name as stratum_4,
cast(m1.value_as_concept_id as string) as stratum_5,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c2 on c2.concept_id=m1.value_as_concept_id
where m1.value_as_concept_id != 0
and m1.measurement_concept_id > 0
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_concept_id,c2.concept_name,m1.value_as_concept_id,stratum_2
union all
SELECT 0,1901 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,'2' as stratum_2,'' as stratum_3,
c2.concept_name as stratum_4,
cast(m1.value_as_concept_id as string) as stratum_5,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c2 on c2.concept_id=m1.value_as_concept_id
where m1.value_as_concept_id != 0
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,c2.concept_name,m1.value_as_concept_id,stratum_2"


# Set the counts > 0 and < 20 to 20
echo "Binning counts < 20"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
set count_value = 20, source_count_value = 20 where analysis_id in (1900,1901) and ((count_value>0 and count_value<20) or (source_count_value>0 and source_count_value<20))"

