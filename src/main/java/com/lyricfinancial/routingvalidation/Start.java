/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * Created by Craig Earley on 1/29/16.
 */
public class Start extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);

        Map<String, String> map = System.getenv();
        JsonObject config = config();

        if (map.containsKey("HTTP_PORT")) {
            config.put("http.port", Integer.valueOf(map.get("HTTP_PORT")));
        }
        if (map.containsKey("DOWNLOAD_DELAY_MINUTES")) {
            config.put("download.delay", Long.valueOf(map.get("DOWNLOAD_DELAY_MINUTES")));
        }
        if (map.containsKey("POSTGRES_USER")) {
            config.put("db.username", map.get("POSTGRES_USER"));
        }
        if (map.containsKey("POSTGRES_PASSWORD")) {
            config.put("db.password", map.get("POSTGRES_PASSWORD"));
        }

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle("service:com.lyricfinancial.routingvalidation.epayments-database-service", options, databaseSvcStarted -> {
            vertx.deployVerticle("service:com.lyricfinancial.routingvalidation.epayments-routing-directory-service", options, routingDirectorySvcStarted -> {
                vertx.deployVerticle("service:com.lyricfinancial.routingvalidation.epayments-bank-routing-lookup-service", options);
                vertx.deployVerticle("service:com.lyricfinancial.routingvalidation.epayments-datapump-service", options);
            });
        });
    }
}
