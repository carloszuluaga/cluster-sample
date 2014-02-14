import spray.revolver.RevolverPlugin._

seq(Revolver.settings: _*)

/* sbt */
name := "poc-expedicion-core"

version := "0.0.1"

scalaVersion := "2.10.3"

resolvers ++= Seq(
  "Typesafe Repo"     at  "http://repo.typesafe.com/typesafe/releases/"
)

val akkaVersion = "2.3.0-RC2"

libraryDependencies ++= Seq(
  "junit"                             %   "junit"               			     %   "4.10"        % "test",
  "org.scalatest"                     %%  "scalatest"           			     %   "1.9.1"       % "test",
  "com.typesafe"                      %%  "scalalogging-slf4j"  			     %   "1.0.1",
  "com.typesafe"                      %   "config"              			     %   "1.0.2",
  "com.typesafe.akka"        		  %% 	"akka-actor"       			         % akkaVersion,
  "com.typesafe.akka"     %% 	"akka-slf4j"       			         % akkaVersion,
  "com.typesafe.akka"  						    %% 	"akka-testkit"     			         % akkaVersion      % "test",
  "com.typesafe.akka"                 %%  "akka-cluster"                   % akkaVersion,
  "com.typesafe.akka"                 %%  "akka-persistence-experimental"  % akkaVersion,
  "com.typesafe.akka"                 %%  "akka-contrib"                   % akkaVersion
)