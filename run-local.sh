#
# This script is used to run the Official Visits API locally.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local profile (from application-local.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around setting the DB properties, SERVER_PORT and client credentials to match those used in the docker-compose files.
#

# Provide the DB connection details to local container-hosted Postgresql DB already running
export DB_SERVER=localhost
export DB_NAME=official-visits-db
export DB_USER=official-visits
export DB_PASS=official-visits
export DB_SSL_MODE=prefer

# Pull in any secret values into the current shell environment e.g. system clients/secrets, and any other values to override.
export $(cat .env | xargs)

# Run the application with stdout and local profiles active
SPRING_PROFILES_ACTIVE=stdout,local ./gradlew bootRun

# End
