#!/usr/bin/env bash

# Script change version and run build
# then adds changes and do a release commit
# then creates tag with version specified by argument
# user must then push tags using `git push --tags` and do a release in github

TAG=$1
echo "Tagging version to ${TAG}"

mvn versions:set -DnewVersion=${TAG}
mvn clean install
git add "."
git diff --staged --quiet || git commit -m "Release ${TAG}"
git tag -a ${TAG} -m "${TAG}"
