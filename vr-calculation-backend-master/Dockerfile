FROM bellsoft/liberica-openjdk-debian:21 AS base

FROM base AS builder
RUN apt-get update && apt-get install -y maven && apt-get clean
WORKDIR /opt
COPY sql_scripts sql_scripts
COPY src src
COPY pom.xml pom.xml
RUN mvn clean install

FROM base AS app

COPY inline.ca.der /ets/ssl/serts/

COPY *.der /opt
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -noprompt -alias inline -file /opt/inline.ca.der
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -noprompt -alias sakhalin -file /opt/sakhalin.ca.der
RUN keytool -v -printcert -file /opt/inline.ca.der

WORKDIR /opt
COPY --from=builder /opt/target/vr-calculation-backend.jar /opt/vr-calculation-backend.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "/opt/vr-calculation-backend.jar"]
