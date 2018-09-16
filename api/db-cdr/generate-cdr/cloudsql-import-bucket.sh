#!/bin/bash

# Imports all sql and csv files in the bucket to the database.
# IF --create-dump-file is specified, it is ran first
# Table names for csvs are determined from the filename.

set -xeuo pipefail
IFS=$'\n\t'

CREATE_DUMP_FILE=

# get options
USAGE="./generate-clousql-cdr/cloudsql-import-bucket.sh --project <PROJECT> --instance <INSTANCE> --bucket <BUCKET>"
# example account for test : all-of-us-workbench-test@appspot.gserviceaccount.com
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --database) DATABASE=$2; shift 2;;
    --create-dump-file) CREATE_DUMP_FILE=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${INSTANCE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Grant Access to files for service account
SERVICE_ACCOUNT="${PROJECT}@appspot.gserviceaccount.com"

gsutil acl ch -u $SERVICE_ACCOUNT:O gs://$BUCKET/*
gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=$(gcloud sql instances describe --project $PROJECT \
    --account $SERVICE_ACCOUNT $INSTANCE | grep serviceAccountEmailAddress \
    | cut -d: -f2)
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $BUCKET/*"
# Note, this only applies to files already existing in the bucket.
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R gs://$BUCKET/*

create_gs_file=gs://
# Create db from dump file if we need to
if [ "${CREATE_DUMP_FILE}" ]
then
    create_gs_file=$create_gs_file$BUCKET/$CREATE_DUMP_FILE
    echo "Creating cloudsql DB from dump file ${CREATE_DUMP_FILE}"
    gcloud sql import sql $INSTANCE $create_gs_file --project $PROJECT --database=$DATABASE \
        --account $SERVICE_ACCOUNT --quiet
    # If above fails let it die as user obviously intended to create the db
    gsutil mv $create_gs_file gs://$BUCKET/imported_to_cloudsql/
fi

#Import files, do sql first as they are intended for schema changes and such
#

# gsutil returns error if no files match thus the "2> /dev/null || true" part
sqls=( $(gsutil ls gs://$BUCKET/*.sql* 2> /dev/null || true))

for gs_file in "${sqls[@]}"
do
    if [ "$gs_file" = "$create_gs_file" ]
    then
        echo "Skipping create dump file ";
    else
        gcloud sql import sql $INSTANCE $gs_file --project $PROJECT --account $SERVICE_ACCOUNT --quiet --async
        echo "Import started, waiting for it to complete."
        seconds_waited=0
        wait_interval=15
        while true; do
            sleep $wait_interval
            seconds_waited=$((seconds_waited + wait_interval))
            import_status=
            if [[ $(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT.*RUNNING") ]]
            then
                echo "Import is still running after ${seconds_waited} seconds."
            else
                echo "Import finished after ${seconds_waited} seconds."
                # Move file to imported dir
                gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
                break
            fi
        done
    fi
done

csvs=( $(gsutil ls gs://$BUCKET/*.csv* 2> /dev/null || true))
for gs_file in "${csvs[@]}"
do
   # Get table name from file, Table name can only contain letters, numbers, -, _
   filename="${gs_file##*/}"
   table=
   if [[ $filename =~ ([[:alnum:]_-]*) ]]
   then
        # Strip extension and the 00000* digits in case big tables were dumped into multiple files
        table=${BASH_REMATCH[1]}
        table=${table%%[0-9]*}
        echo "Importing file into $table"
        gcloud sql import csv $INSTANCE $gs_file --project $PROJECT --quiet --account $SERVICE_ACCOUNT \
        --database=$DATABASE --table=$table --async
        echo "Import started, waiting for it to complete."
        seconds_waited=0
        wait_interval=15
        while true; do
            sleep $wait_interval
            seconds_waited=$((seconds_waited + wait_interval))
            import_status=
            if [[ $(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT.*RUNNING") ]]
            then
                echo "Import is still running after ${seconds_waited} seconds."
            else
                echo "Import finished after ${seconds_waited} seconds."
                # Move file to imported dir
                gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
                break
            fi
        done
   else
        echo "Unable to parse table from $filename. Skipping it."
   fi
done




