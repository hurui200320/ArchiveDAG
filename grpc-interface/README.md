# grpc-interface

This is the submodule for grpc code generation. All grpc related files are located in this submodule.

The files are organized by group. For example: `objects.proto` contains all node definitions for the Merkle DAG, and
it's belong to the `ariteg` package, so the proto will has `archivedag.ariteg.protos` as it package. The `protos` suffix
is keep the generated code from mixing with the normal code, those codes need to be excluded when doing the coverage
test.
