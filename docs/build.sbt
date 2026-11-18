import laika.ast.Path.Root
import laika.config.{LaikaKeys, SyntaxHighlighting}
import laika.format.Markdown
import laika.helium.Helium
import laika.helium.config.{HeliumIcon, IconLink, TextLink}
import laika.theme.config.Color

import scala.sys.process.Process

enablePlugins(IdmlDocSbt)

name := "idml-docs"

scalaVersion := "3.3.9" // nothing is compiled here; set to silence the default-version warning

// idmldoc emits fenced code blocks, a GitHub-flavored extension, as are the tables of dependencies.md
laikaExtensions := Seq(Markdown.GitHubFlavor, SyntaxHighlighting)

// index.md, not Laika's default README.md, is each directory's title document
laikaConfig := LaikaConfig.defaults.withRawContent // functions/date.md has an HTML table
  .withConfigValue(LaikaKeys.titleDocuments.inputName, "index")

laikaTheme := Helium.defaults.site
  .metadata(title = Some("IDML"), description = Some("Ingestion Data Mapping Language"), language = Some("en"))
  .site
  .themeColors(
    primary = Color.hex("C65736"),
    primaryMedium = Color.hex("E3E2E3"),
    primaryLight = Color.hex("F4F3F4"),
    secondary = Color.hex("162341"),
    text = Color.hex("453E46"),
    background = Color.hex("FFFFFF"),
    bgGradient = (Color.hex("162341"), Color.hex("1C2C52"))
  )
  .site
  .topNavigationBar(navLinks =
    Seq(
      TextLink.internal(Root / "getting-started.md", "Language Docs"),
      TextLink.internal(Root / "basic-usage.md", "API Docs"),
      TextLink.external("https://github.com/idml/idml/releases", "Releases"),
      IconLink.external("https://github.com/idml/idml", HeliumIcon.github)
    )
  )
  .build

// a stable path for publishing, instead of target/out/jvm/scala-<version>/idml-docs/docs/site
laikaSite / target := baseDirectory.value / "target" / "site"

lazy val index = taskKey[Unit]("regenerate src/main/tut/functions/index.md")
index := Def.uncached {
  val functions = IdmlDocSbt.idmlSourceDirectory.value / "functions"
  val markdown  = Process(Seq("python3", (baseDirectory.value / "generate-index.py").getPath, functions.getPath)).!!
  IO.write(functions / "index.md", markdown)
}

lazy val makeSite = taskKey[Unit]("regenerate the functions index, then build the site into target/site")
makeSite := Def.uncached(Def.sequential(index, IdmlDocSbt.makeIdml).value)
