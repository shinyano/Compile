FROM java:8
WORKDIR /app/
COPY ./* ./
RUN javac App.java
RUN chmod +x App
