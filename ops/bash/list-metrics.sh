#!/bin/bash
# Get all of the custom Stackdriver Monitoring Metrics in the active project.
curl -X GET "https://monitoring.googleapis.com/v3/projects/$1/metricDescriptors"  \
  --header "Authorization: Bearer $(gcloud auth print-access-token)"
