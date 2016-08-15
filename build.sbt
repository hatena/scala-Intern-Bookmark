import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

lazy val internbookmark = (project in file(".")).
  settings(ScalatraPlugin.scalatraWithJRebel: _*).
  settings(scalateSettings: _*).
  settings(
    name := "scala-Intern-Bookmark",
    version := "0.0.2",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % "2.9.4",
      "org.joda" % "joda-convert" % "1.8",
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
      "com.github.tarao" %% "slick-jdbc-extension" % "0.0.5",
      "com.github.tototoshi" %% "slick-joda-mapper" % "2.1.0",
      "mysql" % "mysql-connector-java" % "5.1.39",
      "ch.qos.logback" % "logback-classic" % "1.1.7",

      "org.json4s" %% "json4s-jackson" % "3.2.11",
      "org.json4s" %% "json4s-ext" % "3.2.11",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
      "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
      "org.scalatra" %% "scalatra" % "2.3.0",
      "org.scalatra" %% "scalatra-scalatest" % "2.3.0",
      "org.scalatra" %% "scalatra-json" % "2.3.0",
      "org.scalatra" %% "scalatra-auth" % "2.3.0",
      "org.eclipse.jetty" % "jetty-webapp" % "9.1.5.v20140505" % "container",
      "org.eclipse.jetty" % "jetty-plus" % "9.1.5.v20140505" % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0"
    ),
    fork in Test := true,
    javaOptions in Test += "-Dconfig.resource=test.conf",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xlint",
      "-Xlint:-missing-interpolator",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-value-discard"
    ),
    initialCommands := "import internbookmark._",
    TwirlKeys.templateImports += "internbookmark.model._"
  ).
  enablePlugins(SbtTwirl)

