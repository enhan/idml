# Ingestion Data Mapping Language

[![Build Status](https://cloud.drone.io/api/badges/IDML/idml/status.svg)](https://cloud.drone.io/IDML/idml)


See http://idml.io/

Requires Java 17+. See [BUILD.md](BUILD.md) for building.

## History

IDML has been developed and maintained by [DataSift](https://datasift.com) (now a [Meltwater](https://www.meltwater.com) company), where it's been used to deliver high throughput social media firehoses and to allow customers to onboard data easily.

## Releasing to Sonatype

If you're a member of the `io.idml` namespace on Sonatype Central you can perform a release with these commands
(sbt 2's built-in Sonatype Central support, credentials for host `central.sonatype.com`, e.g. a `credentials += Credentials(...)` file
or via `SONATYPE_USERNAME` / `SONATYPE_PASSWORD`):

1. add `default-key $KEYID` to `~/.gnupg/gpg.conf` to specify the key to release with
2. `sbt publishSigned` to stage the signed artifacts in `target/sona-staging`
3. `sbt sonaRelease` to upload and release them (or `sbt sonaUpload` to upload and release manually from the portal)
4. `sbt tool/assembly` to build the tool jar
5. get the tool jar from `./target/out/jvm/scala-2.13.18/idml-tool/idml-tool-assembly-$VERSION.jar`
6. create a release on github with the format `$VERSION: Title of Release`
7. Upload the assembly jar to that release and write release notes


## Special Thanks

This project has had many contributors before being open sourced, these include:

* Andi Miller
* Jon Davey
* Stuart Dallas
* Courtney Robinson
* James Bloomer
