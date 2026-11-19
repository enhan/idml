# Ingestion Data Mapping Language

[![Build Status](https://cloud.drone.io/api/badges/IDML/idml/status.svg)](https://cloud.drone.io/IDML/idml)


See http://idml.io/

Requires Java 17+. The libraries are published for Scala 2.13 and Scala 3, the tool and other applications for Scala 3.
See [BUILD.md](BUILD.md) for building.

`idml-core` logs through slf4j-api 2.x, so applications need an slf4j-2-compatible backend (logback 1.3+,
`log4j-slf4j2-impl`, slf4j-simple 2.x, ...); an slf4j 1.x binding is ignored (slf4j prints a
warning and falls back to NOP logging).

## sbt plugins

Both plugins require sbt 2.

- `idmltest-plugin` runs the IDML tests in `src/test/idml/*.json` (`idmlTest`, also run by `test` / `testFull`):

  ```scala
  // project/plugins.sbt
  addSbtPlugin("io.idml" % "idmltest-plugin" % "<version>")
  ```

- `idmldoc-plugin` evaluates the `idml:input` / `idml:code` blocks of the markdown in `src/main/tut` and renders the
  result as a [Laika](https://typelevel.org/Laika/) site (default Helium theme, the root page is `README.md`):

  ```scala
  // project/plugins.sbt
  addSbtPlugin("io.idml" % "idmldoc-plugin" % "<version>")
  // build.sbt
  enablePlugins(IdmlDocSbt)
  ```

  `sbt makeIdml` (or `laikaSite`) writes the site to `(laikaSite / target)`; `laikaPreview` serves it.
  Laika is configured as usual (`laikaTheme`, `laikaConfig`, ...); `IdmlDocSbt.idmlSourceDirectory` changes the input
  folder. The plugin replaces Laika's default source directory (`src/docs`) with the generated
  `IdmlDocSbt.idmlTargetDirectory`, which every Laika task regenerates first; `laikaPreview` does so once at startup,
  so restart it after editing the markdown.

## History

IDML has been developed and maintained by [DataSift](https://datasift.com) (now a [Meltwater](https://www.meltwater.com) company), where it's been used to deliver high throughput social media firehoses and to allow customers to onboard data easily.

## Releasing to Sonatype

If you're a member of the `io.idml` namespace on Sonatype Central you can perform a release with these commands
(sbt 2's built-in Sonatype Central support, credentials for host `central.sonatype.com`, e.g. a `credentials += Credentials(...)` file
or via `SONATYPE_USERNAME` / `SONATYPE_PASSWORD`):

1. add `default-key $KEYID` to `~/.gnupg/gpg.conf` to specify the key to release with
2. `sbt +publishSigned` to stage the signed artifacts in `target/sona-staging`
3. `sbt sonaRelease` to upload and release them (or `sbt sonaUpload` to upload and release manually from the portal)
4. `sbt tool/assembly` to build the tool jar
5. get the tool jar from `./target/out/jvm/scala-3.3.9/idml-tool/idml-tool-assembly-$VERSION.jar`
6. create a release on github with the format `$VERSION: Title of Release`
7. Upload the assembly jar to that release and write release notes


## Special Thanks

This project has had many contributors before being open sourced, these include:

* Andi Miller
* Jon Davey
* Stuart Dallas
* Courtney Robinson
* James Bloomer
