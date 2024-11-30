# Opa - a converter between OMA and OPA file formats

***Note: [Oma](https://github.com/kumakyoo42/Oma) software (including
additional programs like Opa and libraries) and [related file
formats](https://github.com/kumakyoo42/oma-file-formats) are currently
experimental and subject to change without notice.***

The [oma
format](https://github.com/kumakyoo42/oma-file-formats/blob/main/OMA.md)
is a binary format, making it hard to read for humans. Opa can be used
to convert between files in OMA format and a human readable version
called OPA format.

Files converted to OPA format and back should be identical to the
original files. This is not true for the other way round. In this case
formatting and comments may differ.

## Install

Download [opa.jar](/opa.jar) and make sure that a Java Runtime
Environment (JRE) is available on your system.

## Usage

For conversion from OMA to OPA file format:

    java -jar opa.jar <input file>

The output file will be written to stdout.

For conversion from OPA to OMA file format:

    java -jar opa.jar <input file> <output file>

## Build

On Linux systems you can use the shell script `build.sh` to build
`opa.jar` on your own.

Building on other platforms is neither tested nor directly supported
yet. Basically you need to compile the java files in folder
`de/kumakyoo/opa` and build a jar file from the resulting class files,
including the manifest file.

## Known bugs

There are no known bugs, but a known flaw:

* When converting from OMA format to OPA format, there is currently no
possibility to write directly to a file.

