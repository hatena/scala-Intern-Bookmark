import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

lazy val internbookmark = (project in file(".")).
  settings(ScalatraPlugin.scalatraWithJRebel: _*).
  settings(scalateSettings: _*).
  settings(
    name := "scala-Intern-Bookmark",
    version := "0.0.1",
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-jackson" % "3.2.11",
      "org.json4s" %% "json4s-ext" % "3.2.11",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
      "joda-time" % "joda-time" % "2.7",
      "org.joda" % "joda-convert" % "1.7",
      "com.typesafe.slick" % "slick_2.11" % "3.0.0",
      "com.github.tarao" %% "slick-jdbc-extension" % "0.0.2",
      "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
      "mysql" % "mysql-connector-java" % "5.1.35",
      "com.zaxxer" % "HikariCP-java6" % "2.3.8",
      "ch.qos.logback"  %  "logback-classic"   % "1.1.2",
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
      "-feature"
    ),
    initialCommands := "import internbookmark._",
    TwirlKeys.templateImports += "internbookmark.model._"
  ).
  enablePlugins(SbtTwirl)

