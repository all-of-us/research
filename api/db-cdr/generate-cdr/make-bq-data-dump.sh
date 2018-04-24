#!/bin/bash

# This makes the bq data dump in csv format for a dataset and puts it in google cloud storage
# ACCOUNT must be authorized with gcloud auth login previously

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/make-bq-data-dump.sh --project <PROJECT> --dataset <DATASET>  --bucket=<BUCKET>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --dataset) DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

echo "Dumping tables to csv from $BUCKET\n"

##echo "Echoing dataset name"
##echo ${DATASET}

# Get tables in project, stripping out tableId.
# Note tables larger than 1 G need to be dumped into more than one file.
# concept_relationship and concept are only big ones now.
if [[ $DATASET == *public* ]] || [[ $DATASET == *PUBLIC* ]];
then
    tables=(achilles_analysis achilles_results achilles_results_concept concept concept_relationship criteria db_domain domain vocabulary)
else
    tables=(achilles_analysis achilles_results achilles_results_concept concept concept_relationship criteria db_domain domain vocabulary concept_ancestor)
fi

for table in ${tables[@]}; do
  echo "Dumping table : $table"
  if [[ $table =~ ^(concept|concept_relationship|concept_ancestor|achilles_results)$ ]]
  then
    bq extract $PROJECT:$DATASET.$table gs://$BUCKET/$DATASET/$table*.csv
  else
    bq extract $PROJECT:$DATASET.$table gs://$BUCKET/$DATASET/$table.csv
  fi
done

exit 0
