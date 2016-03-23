# ach-routing-validation

Bank routing validation service for E-Payments. Automatically, retrieves, stores
and updates records directly from the Federal Reserve Bank Service. Provides the
following REST API:

    /epayments                          # all records
    /epayments/:routingNumber           # by routing number
    /epayments/:routingNumber/:bankName # by routing number and bank name

## Docker

Revise **env/postgres.env** and/or **docker-compose.yml** according to your
needs. Then,

    docker-compose build
    docker-compose up -d

## Without Docker

Create a PostgreSQL database using the script in **db/postgres-init.sql**.
Build a fat-jar with,

    ./gradlew assemble shadowJar

Next, export the following environment variables,

    export HTTP_PORT=8082
    export DOWNLOAD_DELAY_MINUTES=1440
    export POSTGRES_USER=postgres
    export POSTGRES_PASSWORD=password

and run with,

    java -jar build/libs/ach-routing-validation-1.0-SNAPSHOT-fat.jar

Alternatively, use **conf.json** file,

    {
      "http.port" : 8080,
      "download.delay" : 1440, // 24 hours
      "db.username" : postgres,
      "db.password" : password
    }

and run with,

    java -jar build/libs/ach-routing-validation-1.0-SNAPSHOT-fat.jar -conf conf.json

## Unit Testing

Running unit tests:

    docker-compose up -d
    $ ./gradlew clean assemble test
    $ docker-compose stop
    $ docker-compose rm -f
