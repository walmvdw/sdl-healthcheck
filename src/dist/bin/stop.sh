#!/usr/bin/env bash

PID_FILE="sdl-healthcheck.pid"

BASEDIR=\$(dirname \$0)
cd \${BASEDIR}/..

if [ -f \$PID_FILE ]; then
    if ps -p \$(cat \$PID_FILE) > /dev/null ; then
        echo "Stopping Service Health Check"
        kill -TERM \$(cat \$PID_FILE)
        echo "Stopped Service Health Check"
    fi
    rm \$PID_FILE &> /dev/null
else
    echo "No pidfile found, service probably not running"
fi
