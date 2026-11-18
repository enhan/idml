name := "idml-lang"

libraryDependencies ++= Seq(
  "org.antlr"        % "antlr4-runtime" % "4.5", // BSD license, keep in sync with project/antlr4.sbt
  "com.google.guava" % "guava"          % "27.0-jre", // Apache License 2
  "org.scalatest"   %% "scalatest"      % "3.2.19" % Test // Apache License 2
)

// Replaces sbt-antlr4 (no sbt 2 release): runs the ANTLR tool from the meta-build classpath.
Compile / sourceGenerators += Def.task {
  val grammars = ((Compile / sourceDirectory).value / "antlr4" ** "*.g4").get().toSet
  val out      = (Compile / sourceManaged).value / "antlr4"
  val options  = Seq("-o", out.getPath, "-package", "io.idml.lang", "-listener", "-visitor")
  // cache keyed on tool version + options too, so changing either regenerates
  val cacheKey = s"antlr4-${org.antlr.v4.Tool.VERSION}-${Hash.toHex(Hash(options.mkString(" "))).take(12)}"
  val generate = FileFunction.cached(streams.value.cacheDirectory / cacheKey) { (in: Set[File]) =>
    IO.delete(out)
    val tool = new org.antlr.v4.Tool((options ++ in.map(_.getPath)).toArray)
    tool.processGrammarsOnCommandLine()
    if (tool.getNumErrors > 0) sys.error(s"ANTLR reported ${tool.getNumErrors} error(s)")
    (out ** "*.java").get().toSet
  }
  generate(grammars).toSeq
}.taskValue
