Building
========

This project uses sbt 2.1 (see `project/build.properties`) and Scala 2.13.

Requirements: a JDK 17+ (the build is tested with Temurin 25). Bytecode targets Java 17
(`-release:17` / `--release 17`), so the published libraries and the tool require Java 17+ at runtime.

- `sbt compile` compiles every module.
- `sbt test` runs the tests (incremental in sbt 2; use `sbt testFull` to force a full run).
- `sbt tool/assembly` builds the self-executing tool jar under
  `target/out/jvm/scala-2.13.18/idml-tool/`.

The ANTLR grammars in `lang/src/main/antlr4` are compiled by a source generator defined in `lang/build.sbt`.
