#!/bin/sh

. /etc/profile.d/bigtop.sh
exec ./gradlew "$@"
