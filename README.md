# Opa - a converter between OMA and OPA file formats

**Warning: The oma file format is still considered experimental and
may be subject to change without warning. For this reason this
software may change too.**

The oma file format is a binary file format, making it hard to read for
humans. This program converts oma files in a human readable version,
called opa file format. This program also converts the human readable
version back to the binary format.

## Install

Download [oma.jar](/oma.jar) and make sure that Java Runtime
Environment (JRE) is available on your system.

## Usage

For conversion from oma to opa file format:

    java -jar opa.jar <input file>

The output file will be written to stdout.

For conversion from opa to oma file format:

    java -jar opa.jar <input file> <output file>

## Build

To build `opa.jar` on your own, on Linux systems you can use
`build.sh`. I havn't tested building on other operating systems.
Basically you need to compile the java files in folder
`de/kumakyoo/opa` and build a jar file from the resulting class files,
including the manifest file.

## Known bugs

There are no known bugs.

