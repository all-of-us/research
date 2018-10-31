#!/bin/bash

# This generates new BigQuery dataset for use in cloudsql by the data browser
# and dumps csvs of that dataset to import to cloudsql

# End product is:
# 0) Big query dataset for cdr version publicYYYYMMDD
# 1) .csv of all the tables in a bucket

# Example usage, you need to provide a bunch of args
# ./project.rb generate-public-cdr-counts --bq-project all-of-us-ehr-dev --bq-dataset test_merge_dec26 \
# --public-project all-of-us-workbench-test --cdr-version 20180130 \
# --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'


USAGE="./generate-cdr/generate-public-cdr-counts --bq-project <PROJECT> --bq-dataset <DATASET> --public-project <PROJECT>"
USAGE="$USAGE --bucket all-of-us-workbench-cloudsql-create --cdr-version=YYYYMMDD --bin-size <BINSIZE>"
USAGE="$USAGE \n Data is generated from bq-project.bq-dataset and dumped to workbench-project.public<cdr-version>."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --public-project) PUBLIC_PROJECT=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --bin-size) BIN_SIZE=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo this requires args in right order and doesn't print usage. Prints "Unbound variable ...."

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

if [ -z "${PUBLIC_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

#Check cdr_version is of form YYYYMMDD
if [[ $CDR_VERSION =~ ^$|^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

if [ -z "${BIN_SIZE}" ]
then
  $BIN_SIZE=20
fi

PUBLIC_DATASET=public$CDR_VERSION

startDate=$(date)
echo $(date) " Starting generate-public-cdr-counts $startDate"

## Make public cdr count data
echo "Making BigQuery cdr dataset"
if ./generate-cdr/make-bq-data.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --public-project $PUBLIC_PROJECT \
 --public-dataset $PUBLIC_DATASET --cdr-version "$CDR_VERSION"
then
    echo "BigQuery public data generated"
else
    echo "FAILED To generate BigQuery data for public $CDR_VERSION"
    exit 1
fi

## Make public cdr count data
echo "Making BigQuery public dataset"
if ./generate-cdr/make-bq-public-data.sh \
  --public-project $PUBLIC_PROJECT --public-dataset $PUBLIC_DATASET --bin-size $BIN_SIZE
then
    echo "BigQuery public data generated"
else
    echo "FAILED To generate public BigQuery data for public $CDR_VERSION"
    exit 1
fi

## Dump public cdr count data
echo "Dumping public dataset to .csv"
if ./generate-cdr/make-bq-data-dump.sh --dataset $PUBLIC_DATASET --project $PUBLIC_PROJECT --bucket $BUCKET
then
    echo "Public cdr count data dumped"
else
    echo "FAILED to dump Public cdr count data"
    exit 1
fi

stopDate=$(date)
echo "Start $startDate Stop: $stopDate"
echo $(date) " Finished generate-public-cdr-counts "

