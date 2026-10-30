# syntax=docker/dockerfile:1
# Construcción: se compila con el wrapper (Maven 3.9.16) dentro del JDK 25.
FROM eclipse-temurin:25-jdk-noble AS construccion
WORKDIR /fuente
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY eventos-contrato/pom.xml eventos-contrato/
COPY nucleo/pom.xml nucleo/
COPY agente-telegram/pom.xml agente-telegram/
COPY eventos-contrato/src eventos-contrato/src
COPY agente-telegram/src agente-telegram/src
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -pl agente-telegram -am package -DskipTests

# Ejecución: solo el JRE, usuario sin privilegios.
FROM eclipse-temurin:25-jre-noble
RUN groupadd --system uniproject && useradd --system --gid uniproject --no-create-home uniproject
WORKDIR /app
COPY --from=construccion /fuente/agente-telegram/target/agente-telegram-0.1.0-SNAPSHOT.jar /app/app.jar
USER uniproject
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
