#!/usr/bin/env bash

PID_FILE="sdl-healthcheck.pid"

BASEDIR=\$(dirname \$0)
cd \${BASEDIR}/..

if [ -f \$PID_FILE ]; then
    if ps -p \$(cat \$PID_FILE) > /dev/null ; then
        echo "Already running, stop first"
        exit 0
    fi
fi

if [ -f ./config/logback.xml ]; then
    JAVA_OPTS="\$JAVA_OPTS -Dlogging.config=file:./config/logback.xml"
fi

java \$JAVA_OPTS -jar ./lib/${jarName} & echo \$! > \$PID_FILE

echo "Started Service Healthceck"