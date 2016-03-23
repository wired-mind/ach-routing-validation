# This Dockerfile is a work-around for
# https://github.com/docker/compose/issues/2247
# which is preventing the mounting of host
# volumes in docker-compose.yml.
#
# Created by Craig Earley on 2/3/16.
#
FROM busybox

COPY ./postgres-init.sql /docker-entrypoint-initdb.d/