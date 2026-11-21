# Imagen del backend: se compila dentro (no hace falta Java en la máquina que construye) y corre con un JRE.
# Ubuntu (glibc) y no Alpine (musl): el nativo de OR-Tools va compilado contra glibc.
FROM eclipse-temurin:21-jdk-noble AS compilacion
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests && cp target/forja-backend-*.jar /app.jar

FROM eclipse-temurin:21-jre-noble
RUN useradd --system --uid 10001 forja
USER forja
WORKDIR /app
COPY --from=compilacion /app.jar app.jar
EXPOSE 8080
# OR-Tools carga su nativo por JNI: sin --enable-native-access, Java 21+ avisa en cada arranque.
# MaxRAMPercentage deja aire fuera del heap para el solver (memoria nativa) en un contenedor de 1 GiB.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
