#!/usr/bin/env bash

sbt clean scalastyle coverage test IntegrationTest / testOnly coverageReport dependencyUpdates
