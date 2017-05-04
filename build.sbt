name := "SQLiGRH"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "mysql" % "mysql-connector-java" % "5.1.28",
  "org.apache.xmlbeans" % "xmlbeans" % "2.4.0",
  "org.apache.poi" % "poi" % "3.9",
  "org.apache.poi" % "poi-ooxml" % "3.9",
  "org.apache.poi" % "ooxml-schemas" % "1.1",
  "org.apache.poi" % "poi-ooxml-schemas" % "3.9",
  "net.sourceforge.dynamicreports" % "dynamicreports-core" % "3.1.6",
  "com.lowagie" % "itext" % "2.1.7",
  "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2"
)

play.Project.playJavaSettings


