#!/bin/bash

# This generates big query denormalized tables for search, review and datasets.

set -e

export BQ_PROJECT=$1          # project
export BQ_DATASET=$2          # dataset
export CDR_DATE=$3            # cdr date
export DATA_BROWSER=$4        # data browser flag
export DRY_RUN=$5             # dry run

echo ""
echo 'Making denormalized search events table'
if ./generate-cdr/make-bq-denormalized-search-events.sh $BQ_PROJECT $BQ_DATASET $DATA_BROWSER $DRY_RUN
then
    echo "Making denormalized search table complete"
else
    echo "Making denormalized search table failed!"
    exit 1
fi

echo ""
echo "Making criteria tables"
if ./generate-cdr/generate-cb-criteria-tables.sh $BQ_PROJECT $BQ_DATASET $DATA_BROWSER $DRY_RUN
then
    echo "Making criteria tables complete"
else
    echo "Making criteria tables failed!"
    exit 1
fi

if [ "$DATA_BROWSER" == false ]
then

  echo ""
  echo 'Making denormalized search person table'
  if ./generate-cdr/make-bq-denormalized-search-person.sh $BQ_PROJECT $BQ_DATASET $CDR_DATE $DRY_RUN
  then
      echo "Making denormalized search person table complete"
  else
      echo "Making denormalized search person table failed!"
      exit 1
  fi

  echo ""
  echo "Making denormalized review tables"
  if ./generate-cdr/make-bq-denormalized-review.sh $BQ_PROJECT $BQ_DATASET $DRY_RUN
  then
      echo "Making denormalized review tables complete"
  else
      echo "Making denormalized review tables failed!"
      exit 1
  fi

  echo ""
  echo "Making denormalized dataset tables"
  if ./generate-cdr/make-bq-denormalized-dataset.sh $BQ_PROJECT $BQ_DATASET $DRY_RUN
  then
      echo "Making denormalized dataset tables complete"
  else
      echo "Making denormalized dataset tables failed"
      exit 1
  fi

  echo ""
  echo "Making dataset linking tables"
  if ./generate-cdr/make-bq-dataset-linking.sh $BQ_PROJECT $BQ_DATASET $DRY_RUN
  then
      echo "Making dataset linking tables complete"
  else
      echo "Making dataset linking tables failed!"
      exit 1
  fi
fi
