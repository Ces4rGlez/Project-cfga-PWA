FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app


COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .


COPY src src


RUN ./gradlew build -x test


RUN rm -f build/libs/*-plain.jar


FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp


COPY --from=build /workspace/app/build/libs/*.jar app.jar


ENV PORT=8081


EXPOSE 8081


ENTRYPOINT ["java","-jar","/app.jar"]
