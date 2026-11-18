#!/usr/bin/env bash
echo "Trying to publish docker image"
set -e
sbt tool/Docker/publish
