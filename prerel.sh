#!/bin/bash
#
# Purpose:  Pre-Release script to update version number in the project
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
    echo "    prerel.sh [old-version] [new-version]"
    echo " "
    echo "    where [old-version] is the previous version number that needs to be replaced."
    echo "          [new-version] is the new version number for the next release"
    echo " "
    echo "    example: ./prerel.sh 1.4.6 1.5.0"
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

mvn versions:set -DnewVersion=$new -DgenerateBackupPoms=false
