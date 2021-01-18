FROM java:8
WORKDIR /app/
COPY ./* ./
RUN javac -encoding utf-8 App.java
RUN chmod +x App
