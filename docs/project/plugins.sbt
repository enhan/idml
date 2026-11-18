// The idmldoc plugin comes from the root build: `sbt 'set ThisBuild / version := "0.0.0-docs"; publishLocal'` there first.
// An sbt build can't load a plugin it compiles itself, so the docs are a separate build (see BUILD.md).
addSbtPlugin("io.idml" % "idmldoc-plugin" % sys.props.getOrElse("plugin.version", "0.0.0-docs"))
