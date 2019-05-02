#!/bin/sh

if [ "$#" -eq 0 ]; then
    TAGNAME="master"
else
    TAGNAME=$1
fi

docker build docker/wb --no-cache --rm -t devgaoeng/artemis:$TAGNAME
docker push devgaoeng/artemis:$TAGNAME

