/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.services;

import com.lyricfinancial.routingvalidation.data.DownloadResult;
import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Database service for E-Payments Routing Directories.
 * <p>
 * Created by Craig Earley on 1/29/16.
 */
@ProxyGen
public interface DatabaseService {

    static DatabaseService createProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(DatabaseService.class, vertx, address);
    }

    /**
     * Get the last successful download result for
     * E-Payments Routing Directories.
     *
     * @param resultHandler last successful DownloadResult or null
     */
    void getLastSuccessfulDownloadResult(Handler<AsyncResult<DownloadResult>> resultHandler);

    /**
     * Save download result for E-Payments Routing Directories.
     *
     * @param downloadResult data to save
     * @param resultHandler  a JsonObject result handler
     */
    void saveFedwireParticipants(DownloadResult downloadResult, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     * Find records matching given criteria.
     *
     * @param criteria      criteria to match
     * @param resultHandler a JsonObject result handler
     */
    void findFedwireParticipants(JsonObject criteria, Handler<AsyncResult<JsonObject>> resultHandler);

    @ProxyClose
    void close();
}
