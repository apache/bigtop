#!/bin/bash

. /etc/profile.d/bigtop.sh
exec ./gradlew "$@"
