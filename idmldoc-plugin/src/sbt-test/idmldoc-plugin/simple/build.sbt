enablePlugins(IdmlDocSbt)

// The site must contain the IDML block's evaluated output, not just the source block.
TaskKey[Unit]("checkSite") := Def.uncached {
  val html = IO.read((laikaSite / target).value / "index.html")
  assert(html.contains("r = a + b"), s"IDML code block missing from site:\n$html")
  assert(html.matches("(?s).*&quot;r&quot;\\s*:\\s*3.*"), s"IDML output missing from site:\n$html")
  assert(!html.contains("idml:code"), s"IDML block was not processed:\n$html")
}

// Laika tasks other than laikaSite must regenerate the IDML input too.
TaskKey[Unit]("wipeGenerated") := Def.uncached(IO.delete(IdmlDocSbt.idmlTargetDirectory.value))
TaskKey[Unit]("checkGenerated") := Def.uncached {
  assert((IdmlDocSbt.idmlTargetDirectory.value / "README.md").exists, "laikaHTML did not run idml first")
}
