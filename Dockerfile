from eclipse-temurin:25-jdk as sbt
add https://github.com/sbt/sbt/releases/download/v2.1.0/sbt-2.1.0.tgz /tmp/sbt.tgz
run tar xzf /tmp/sbt.tgz -C /opt
env PATH="/opt/sbt/bin:${PATH}"
copy . /build/
workdir /build/
run sbt tool/Docker/stage
workdir /build/target/out/jvm/scala-2.13.18/idml-tool/docker/stage

FROM amazoncorretto:17-alpine3.18-full
RUN apk update
RUN apk add bash
WORKDIR /opt/docker
# native-packager 1.13 stages layered dirs: 2 = dependencies, 4 = application + bin
COPY --from=sbt --chown=daemon:daemon /build/target/out/jvm/scala-2.13.18/idml-tool/docker/stage/2/opt /opt
COPY --from=sbt --chown=daemon:daemon /build/target/out/jvm/scala-2.13.18/idml-tool/docker/stage/4/opt /opt
EXPOSE 8081
USER daemon
ENTRYPOINT ["/opt/docker/bin/idml-tool"]
CMD []

