#imagen base
#docker build -t app-customers .
#docker build -t alhehita/app-customers .
#docker login
#docker logout
#docker push alhehita/app-customers

FROM eclipse-temurin:21.0.7_6-jre-noble

#crear directorio de la aplicación
RUN mkdir /app
WORKDIR /app

ENV LANGUAGE="en_US:en"

#copiar la estructura de la aplicación en el directorio
#son cosideradas capas que no se reconstruyen si no hay cambios
#al registro se suben solo las capas que han cambiado
COPY build/quarkus-app/lib ./lib
COPY build/quarkus-app/quarkus ./quarkus
COPY build/quarkus-app/app ./app
COPY build/quarkus-app/quarkus-run.jar app.jar

CMD ["java", "-jar", "app.jar"]

# Exponer el puerto 8080