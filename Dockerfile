FROM java:8
WORKDIR /app/
COPY ./* ./
RUN javac App.java -encoding utf-8
RUN chmod +x App
