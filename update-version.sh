#!/bin/bash
#
# Purpose:  Bump up version after release in develop branch and checked in.
#
# Author:  Steve Hu, stevehu@gmail.com
#
# Parameters:
#   $1:  old version
#   $2:  new version
#

old=$1
new=$2

function showHelp {
    echo " "
    echo "Error: $1"
    echo " "
    echo "    update-version.sh [old-version] [new-version]"
    echo " "
    echo "    where [old-version] is the previous version number that needs to be replaced."
    echo "          [new-version] is the new version number for the next release"
    echo " "
    echo "    example: ./update-version.sh 1.4.6 1.5.0"
    echo " "
}

if [ -z $1 ]; then
    showHelp "[old-version] parameter is missing"
    exit
fi

if [ -z $2 ]; then
    showHelp "[new-version] parameter is missing"
    exit
fi

# For light-4j, the only thing that needs to be done is to change the version
# number. The docs folder will be moved to light-doc in the future.

# version is after release is in develop branch only
git checkout develop
# just ensure develop is in sync with master.
git merge master
# update pom.xml version
mvn versions:set -DnewVersion=$new -DgenerateBackupPoms=false
# check in the update
git add .
git commit -m "update to new version after release"
git push origin develop
