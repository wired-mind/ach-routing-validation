FROM java:8
MAINTAINER Craig Earley <cearley.consulting@gmail.com>

COPY . /src
WORKDIR /src

RUN ./gradlew clean shadowJar
RUN mkdir /app
RUN cp /src/build/libs/ach-routing-validation-1.0-SNAPSHOT-fat.jar /app
RUN rm -rf /src

WORKDIR /app
CMD ["/usr/bin/java", "-jar", "/app/ach-routing-validation-1.0-SNAPSHOT-fat.jar"]
