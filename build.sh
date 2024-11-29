#!/bin/sh

set -e

javac de/kumakyoo/opa/*.java
jar cmf META-INF/MANIFEST.MF opa.jar de/kumakyoo/opa/*.class
