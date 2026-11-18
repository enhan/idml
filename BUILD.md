Building
========

This project uses sbt 2.1 (see `project/build.properties`).

Scala versions:

- the libraries (`lang`, `datanodes`, `core`, `circe`, `jackson`, `jsoup`, `hashing`, `geo`, `utils`) are
  cross-built for Scala 3.3 (LTS) and Scala 2.13 (`crossScalaVersions`);
- the applications (`repl`, `idmld`, `idmltest`, `idmldoc`, `idmltutor`, `tool`) are Scala 3 only.

The default `scalaVersion` is Scala 3, so plain commands (`sbt compile`, `sbt testFull`) build everything on Scala 3.
Prefix a command with `+` to also run it on Scala 2.13 for the libraries, e.g. `sbt +testFull` or `sbt +publishLocal`.

Requirements: a JDK 17+ (the build is tested with Temurin 25). Bytecode targets Java 17
(`-release:17` / `--release 17`), so the published libraries and the tool require Java 17+ at runtime.

- `sbt compile` compiles every module.
- `sbt test` runs the tests (incremental in sbt 2; use `sbt testFull` to force a full run).
- `sbt +testFull` runs the full test suite on both Scala versions (libraries on 2.13 and 3, applications on 3).
- `sbt tool/assembly` builds the self-executing tool jar under
  `target/out/jvm/scala-3.3.9/idml-tool/`.

The ANTLR grammars in `lang/src/main/antlr4` are compiled by a source generator defined in `lang/build.sbt`.
