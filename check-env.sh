#!/usr/bin/env bash

if [ -z "$JAVA_HOME" ]; then
  echo JAVA_HOME is not set
  exit 1
fi

if [ -z "$JAVA5_HOME" ]; then
  echo JAVA5_HOME is not set
  exit 1
fi

if [ -z "$FORREST_HOME" ]; then
  echo FORREST_HOME is not set
  exit 1
fi

echo Found all necessary env variables.
exit 0
