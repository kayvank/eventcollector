# event-collector

Akka actor based event collection service

### Building on local mahine
The final build product is a docker image, consequently your environment must be set up to accordingly.
We use [sbt native packager plugin!](http://www.scala-sbt.org/sbt-native-packager/index.html)
#### Pre-Requisites
- JDK8
- sbt & scala
- [install docker!](https://viget.com/extend/how-to-use-docker-on-os-x-the-missing-guide) (optional)


#### Build
To build and run the unit tests:

```sh
$ sbt clean update compile test ## no test failure
```

To run locally:

```sh
$ sbt run
```

To package as a docker image

```sh
sbt clean compile docker:stage docker:publishLocal ## docker immage will be in the $prject/target/docker
sbt docker:publishLocal     ## docker immage will be published to your local repo
docker images               ## should see vevo/enventcollector
docker run -p 9000:9000 vevo/event-collector:latest ## Run the application in docker locally
```

#### Basic Architecture

```

                              Kinesis
              +------------------------------------------->
              ^                           |       |      |
              |                           |       |      |
              |                           v       v      v
  +----------------------+             Redshift  RDS    S3
  |    EVENT COLLECTOR   |
  |                      |
  +----------------------+
  |                      |
  | 3 Publish to Kinesis |
  |                      |
  |  +----------------+  |
  |                      |
  |  2 Event Enrichment  |
  |                      |
  |  +----------------+  |
  |                      |
  |     1 Validation     |
  |                      |
  +----------------------+
             ^
             |
             | Network (HTTP)
             |
             |
             |
     +------------------+
     |                  |
     |  Endo Analytics  |
     |      Client      |
     |                  |
     +------------------+
```
