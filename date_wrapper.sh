#!/bin/bash
if [[ "$*" == *"%3N"* ]]; then
    /bin/date "$@" | cut -c1-13
else
    /bin/date "$@"
fi
