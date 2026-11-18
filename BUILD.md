Building
========

This project uses sbt 2.1 (see `project/build.properties`).

Scala versions:

- the libraries (`lang`, `datanodes`, `core`, `circe`, `jackson`, `jsoup`, `hashing`, `geo`, `utils`) are
  cross-built for Scala 3.3 (LTS) and Scala 2.13 (`crossScalaVersions`);
- the applications (`repl`, `idmld`, `idmltest`, `idmldoc`, `idmltutor`, `tool`) are Scala 3 only;
- the sbt plugins (`idmltest-plugin`, `idmldoc-plugin`) are sbt 2 only, built with the Scala version sbt itself
  runs on (3.9 for sbt 2.1) and published as `idmltest-plugin_sbt2_3` / `idmldoc-plugin_sbt2_3`.

The default `scalaVersion` is Scala 3, so plain commands (`sbt compile`, `sbt testFull`) build everything on Scala 3.
Prefix a command with `+` to also run it on Scala 2.13 for the libraries, e.g. `sbt +testFull` or `sbt +publishLocal`.

Requirements: a JDK 17+ (the build is tested with Temurin 25). Bytecode targets Java 17
(`-release:17` / `--release 17`), so the published libraries and the tool require Java 17+ at runtime.

- `sbt compile` compiles every module.
- `sbt test` runs the tests (incremental in sbt 2; use `sbt testFull` to force a full run).
- `sbt +testFull` runs the full test suite on both Scala versions (libraries on 2.13 and 3, applications on 3).
- `sbt "idmltest-plugin/scripted; idmldoc-plugin/scripted"` runs the plugins' sbt scripted tests
  (`<plugin>/src/sbt-test`), which `publishLocal` the plugin and the modules it depends on first.
- `sbt tool/assembly` builds the self-executing tool jar under
  `target/out/jvm/scala-3.3.9/idml-tool/`.

The ANTLR grammars in `lang/src/main/antlr4` are compiled by a source generator defined on the `lang` project in the root `build.sbt`.

Building the docs site
----------------------

The documentation site (`docs/`) is a [Laika](https://typelevel.org/Laika/) site rendered by this repo's own
`idmldoc-plugin`, which evaluates the `idml:input` / `idml:code` blocks of the markdown in `docs/src/main/tut`.
An sbt build can't load a plugin it compiles itself, so `docs/` is a separate sbt build that takes the plugin from
the local Ivy repository. From the repository root:

    sbt 'set ThisBuild / version := "0.0.0-docs"; publishLocal'
    cd docs
    sbt makeSite

The first command publishes the plugin and the modules it depends on under the fixed version `0.0.0-docs`, the one
`docs/project/plugins.sbt` expects (pass `-Dplugin.version=<version>` to the docs build to use another one). Rerun it
after changing the language or the plugin. `makeSite` regenerates the functions index
(`docs/src/main/tut/functions/index.md`, with `docs/generate-index.py`, needs `python3`) and writes the site to
`docs/target/site`; Laika fails the build on broken internal links. `sbt laikaPreview` in `docs/` serves the site
locally. Navigation order and section titles live in the `directory.conf` files next to the pages, and the theme
(colours, top bar links) in `docs/build.sbt`.

The root build doesn't know about `docs/`, so `sbt +testFull` and the other root commands never build it.
