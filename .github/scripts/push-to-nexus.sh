#!/usr/bin/env bash

export GPG_TTY=$(tty)

echo "${GPG_SIGNING_KEY}" | base64 -d > signing.gpg
gpg --batch --import signing.gpg

GPG_EXECUTABLE=gpg ./mvnw -DskipTests -s ./.github/scripts/settings.xml -P central-sonatype clean deploy

rm -rf signing.gpg
gpg --delete-keys
gpg --delete-secret-keys