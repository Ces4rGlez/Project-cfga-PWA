FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

# Copiar archivos de configuracion de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copiar el codigo fuente
COPY src src

# Compilar el proyecto omitiendo los tests para que el despliegue sea mas rapido
RUN ./gradlew build -x test

# Eliminar el archivo plain.jar para evitar conflictos en la copia
RUN rm -f build/libs/*-plain.jar

# Imagen ligera de Java para ejecucion
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

# Copiar el JAR compilado desde la etapa anterior
COPY --from=build /workspace/app/build/libs/*.jar app.jar

# Variables de entorno por defecto (se sobreescribiran en Render)
ENV PORT=8081

# Exponer el puerto
EXPOSE 8081

# Ejecutar la aplicacion
ENTRYPOINT ["java","-jar","/app.jar"]
