import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.SettingsHelper._

organization := "vevo"

name := """event-collector"""

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "central" at "https://mvnrepository.com/artifact",
  "emueller-bintray" at "http://dl.bintray.com/emueller/maven",
  "SnowPlow Repo" at "http://maven.snplow.com/releases/",
  "Twitter Maven Repo" at "http://maven.twttr.com/"
)

import TodoListPlugin._

compileWithTodolistSettings

testWithTodolistSettings

lazy val root = (project in file("."))
  .configs(IntegrationTest).settings(Defaults.itSettings: _*)
  .enablePlugins(
    BuildInfoPlugin,
    JavaAppPackaging,
    DockerPlugin,
    UniversalPlugin).settings(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    buildInfoBuildNumber),
  buildInfoPackage := "info",
  buildInfoOptions ++= Seq(BuildInfoOption.BuildTime, BuildInfoOption.ToJson)
)

scalacOptions := Seq(
  "-deprecation",
  "-unchecked",
  "-explaintypes",
  "-encoding", "UTF-8",
  "-feature",
  "-Xlog-reflective-calls",
  "-Ywarn-unused",
  "-Ywarn-value-discard",
  "-Xlint",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Xfuture",
  "-language:postfixOps",
  "-language:implicitConversions"
)

libraryDependencies ++= {
  object V {
    val akka = "2.4.11"
    val spray = "1.3.2"
    val awsSdk = "1.11.16"
    val kcl = "1.7.5"
    val kpl = "0.12.5"
    val scalazon = "0.11"
    val vevo_schema = "1.3.41"
    val specs2 = "2.5"
    val kamon = "0.6.6"
  }
  Seq(
    "com.vevo" % "event-collector-schema" % V.vevo_schema,
    "com.amazonaws" % "amazon-kinesis-client" % V.kcl,
    "com.amazonaws" % "amazon-kinesis-producer" % V.kpl,
    "com.amazonaws" % "aws-java-sdk" % V.awsSdk,
    "io.github.cloudify" %% "scalazon" % V.scalazon,
    "com.eclipsesource" %% "play-json-schema-validator" % "0.7.0",
    "com.snowplowanalytics" %% "scala-maxmind-iplookups" % "0.2.0",
    "com.typesafe" % "config" % "1.3.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-actor" % V.akka,
    "com.typesafe.akka" %% "akka-slf4j" % V.akka
      exclude("com.typesafe.akka", "akka-actor_2.11"),
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "io.spray" %% "spray-can" % V.spray,
    "io.spray" %% "spray-httpx" % V.spray,
    "io.spray" %% "spray-http" % V.spray,
    "io.spray" %% "spray-client" % V.spray,
    "io.spray" %% "spray-caching" % V.spray,
    "io.spray" %% "spray-routing" % V.spray,
    "com.typesafe.play" %% "play-json" % "2.5.10",
    // monitoring
    "io.kamon" %% "kamon-core" % V.kamon
      exclude("com.typesafe.akka", "akka-actor_2.11"),
    "io.kamon" %% "kamon-datadog" % V.kamon
      exclude("com.typesafe.akka", "akka-actor_2.11"),
    "io.kamon" %% "kamon-system-metrics" % V.kamon
      exclude("com.typesafe.akka", "akka-actor_2.11"),
    "org.aspectj" % "aspectjweaver" % "1.8.9",
    // Test dependencies
    "com.typesafe.akka" %% "akka-testkit" % V.akka % "it,test",
    "org.specs2" %% "specs2-core" % V.specs2 % "it,test",
    "io.spray" %% "spray-testkit" % "1.3+" % "it,test"
  )
}

mainClass in (Compile) := Some("BootStrap")

mappings in Universal ++= Seq(
  findJarFromUpdate("aspectjweaver", update.value) ->
    "aspectj/aspectjweaver.jar"
)

parallelExecution in Test := false

buildInfoKeys += buildInfoBuildNumber

buildInfoOptions += BuildInfoOption.BuildTime

publishMavenStyle := true

dockerExposedPorts := Seq(9000, 9443)

maintainer in Docker := "admin@vevo.com"

version in Docker := version.value +
  "-b" + sys.props.getOrElse("build_number", default = "dev")

dockerRepository := Some("vevo")

dockerCommands ++= Seq(
)

dockerUpdateLatest := true

Seq(bintrayResolverSettings: _*)

deploymentSettings

publish <<= publish.dependsOn(publish in config("universal"))

coverageEnabled.in(Test, test) := true

aspectjSettings

fork in run := true // required for kamon

fork in test := false // required for kamon

javaOptions <++= AspectjKeys.weaverOptions in Aspectj

javaOptions in Universal ++= Seq(
  "-J-Xmx2G",
  "-J-Xms1G"
)

bashScriptExtraDefines ++= Seq("addJava -javaagent:${app_home}/../aspectj/aspectjweaver.jar")

javaOptions in Universal += s"-Dkamon.auto-start=true"

def findJarFromUpdate(jarName: String, report: UpdateReport): File = {
  val filter = artifactFilter(name = jarName + "*", extension = "jar")
  val matches = report.matching(filter)
  if (matches.isEmpty) {
    val err: (String => Unit) = System.err.println
    err("can’t find jar file in resources named " + jarName)
    err("unfiltered jar list:")
    report.matching(artifactFilter(extension = "jar")).foreach(x => err(x.toString))
    throw new ResourcesException("can’t find jar file in resources named " + jarName)
  } else {
    matches.head
  }
}
