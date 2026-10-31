# AeroFast calculation engine for Google Cloud Run. Build context = repository root:
#   gcloud run deploy aerofast-engine --source . --region asia-south1 ...
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /src
COPY app/src/main/java/com/example/data app/src/main/java/com/example/data
COPY server server
RUN gradle -p server installDist --no-daemon -q

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 engine
COPY --from=build /src/server/build/install/aerofast-engine /opt/engine
USER engine
ENV PORT=8080
CMD ["/opt/engine/bin/aerofast-engine"]
