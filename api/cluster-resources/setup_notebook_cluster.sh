#!/bin/bash
# Initializes a Jupyter notebook cluster. This file is copied to the GCS bucket
# <PROJECT>-cluster-resources and its GCS path is passed in as
# jupyterUserScriptUri during notebook cluster creation.

# As of initial Workbench launch, we will not be offering or have a need for
# Spark on notebooks clusters. Disable the kernels to avoid presenting spurious
# kernel options to researchers. See https://github.com/DataBiosphere/leonardo/issues/321.
jupyter kernelspec uninstall -f pyspark2
jupyter kernelspec uninstall -f pyspark3

# reticulate is our preferred access method for the AoU client library - default
# to python3 as our pyclient has better support for python3. Rprofile is executed
# each time the R kernel starts.
echo "Sys.setenv(RETICULATE_PYTHON = '$(which python3)')" >> ~/.Rprofile

for v in "2.7" "3"; do
  "pip${v}" install --upgrade 'https://github.com/all-of-us/pyclient/archive/pyclient-v1-17.zip#egg=aou_workbench_client&subdirectory=py'
done
