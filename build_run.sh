#!/bin/sh

#!/bin/sh

###################
# Config
###################
# Aborts the script if a command fails
set -e

###################
# Vars and helpers
###################
SP='********';
function info () {
  echo "";
  echo "${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}";
  echo "$SP [SCRIPT INFO] $1";
  echo "${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}${SP}";
  echo "";
}

###################
## Go woop woop
###################
# 1: Check for required deps in the $PATH
info "Check for needed binaries in PATH";
BINS_TO_CHECK=( mvn docker docker-compose )
for BIN in ${BINS_TO_CHECK[@]}; do
  command -v $BIN >/dev/null 2>&1 || { echo >&2 "I require '$BIN' but it's not installed.  Aborting."; exit 1; };
done
echo "-> Fine √"

#######
info "Checking if docker is running";
docker ps
echo "-> Fine √"

#######
info "Stopping running composed containers"
docker-compose stop

#######
info "Building artifacts and docker images"
mvn clean install -Pdocker

#######
info "Removing old containers of the artifacts"
docker-compose rm -f

#######
info "Composing microservice containers"
docker-compose up -d --remove-orphans && docker-compose logs -tf
