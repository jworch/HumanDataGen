#/bin/bash

export ENV_TTRAIN_DATA=/home/cedric/data/db_forest1/variables.sh

source /home/cedric/data/db_forest1/runCreateSamples.sh
cd /u/ccagniart/source/hadoop-0.21.0
source /u/ccagniart/source/hadoop-0.21.0/myapps/run.sh
cd -
