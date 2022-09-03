FROM hseeberger/scala-sbt:11.0.13_1.5.5_2.13.7
COPY . /app
WORKDIR /app
CMD sbt server/runMain ru.wdevs.cc1503.Main
