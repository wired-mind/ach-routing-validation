/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.verticles;

import com.lyricfinancial.routingvalidation.data.DownloadResult;
import com.lyricfinancial.routingvalidation.services.DatabaseService;
import com.lyricfinancial.routingvalidation.services.RoutingDirectoryService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by Craig Earley on 1/29/16.
 */
public class DataPump extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(DataPump.class.getName());
    private DatabaseService databaseService;
    private RoutingDirectoryService routingDirectoryService;
    private long timerId;

    @Override
    public void start() throws Exception {
        super.start();

        databaseService = DatabaseService.createProxy(vertx, Database.DATABASE_SERVICE);
        routingDirectoryService = RoutingDirectoryService.createProxy(vertx, RoutingDirectory.ROUTING_DIRECTORY_SERVICE);

        // Wait 30 seconds before starting the pump to
        // give all the services time to start up.
        vertx.setTimer(30 * 1_000, event -> {
            pump(); // initial pump

            long delay = config().getLong("download.delay", 24L * 60L) * 60_000L; // default 24 hrs
            timerId = vertx.setPeriodic(delay, timerId1 -> pump()); // periodic pump
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        vertx.cancelTimer(timerId);
    }

    private void pump() {
        databaseService.getLastSuccessfulDownloadResult(asyncQueryResult -> {
            if (asyncQueryResult.failed()) {
                logger.error(asyncQueryResult.cause());
                return;
            }

            DownloadResult lastDownloadResult = asyncQueryResult.result();
            routingDirectoryService.downloadFedwireParticipants(lastDownloadResult, asyncDownloadResult -> {
                if (asyncDownloadResult.failed()) {
                    logger.error(asyncDownloadResult.cause());
                    return;
                }
                DownloadResult data = asyncDownloadResult.result();
                logger.info("Processing data: " + data);
                databaseService.saveFedwireParticipants(data, asyncSaveResult -> {
                    if (asyncSaveResult.failed()) {
                        logger.error(asyncSaveResult.cause());
                    }
                });
            });
        });
    }
}
