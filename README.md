# akka-http-oauth2-server

A simple OAuth2 server with Akka-Http

SBT build, run and test

```sh
$ sbt clean assembly
$ sbt run
$ sbt clean test
```

Docker commands

```sh
#remove all containers
$ docker rm $(docker ps -a -q)

# remove all images
$ docker rmi $(docker images -q)

# build image
$ docker build -t oauth2-image .

# run interactive, useful for debug
$ docker run --name oauth2 -it --entrypoint /bin/bash oauth2-image

# run container
$ docker run --name oauth2 -p8086:8086 -it oauth2-image
```
