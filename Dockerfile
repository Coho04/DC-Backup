# Verwende ein offizielles Java-Image als Basis
FROM alpine/java:21-jdk

# Installiere mariadb-client, das mysqladmin enthält
RUN apk --no-cache add mariadb-client

# Arbeitsverzeichnis erstellen
WORKDIR /app

# Kopiere das JAR-File in das Image
COPY DC-Backup-1.0.jar /app/DC-Backup-1.0.jar

# Start-Skript für das Java-Programm
COPY start.sh /start.sh
COPY .env /app/.env
RUN chmod +x /start.sh

EXPOSE 8080

# Standardkommando zum Starten
CMD ["/start.sh"]
