name := "learning-akka-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"             % akkaVersion,
  
  "com.typesafe.akka" %% "akka-remote"            % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"           % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding"  % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-singleton" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools"     % akkaVersion,

  "com.typesafe.akka" %% "akka-persistence"       % akkaVersion,

  "com.typesafe.akka" %% "akka-distributed-data"  % akkaVersion,

  "com.typesafe.akka" %% "akka-stream"            % akkaVersion,
  
  
  "com.typesafe.akka" %% "akka-testkit"           % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
