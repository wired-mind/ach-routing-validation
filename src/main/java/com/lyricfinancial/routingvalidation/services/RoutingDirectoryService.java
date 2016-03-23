/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.services;

import com.lyricfinancial.routingvalidation.data.DownloadResult;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Routing Directories service for E-Payments.
 * <p>
 * Created by Craig Earley on 1/29/16.
 */
@ProxyGen
public interface RoutingDirectoryService {

    static RoutingDirectoryService createProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(RoutingDirectoryService.class, vertx, address);
    }

    /**
     * Download Fedwire participants.
     *
     * @param lastDownloadResult If not null then retrieve entries that have been updated since
     *                           the date specified in its dateTimeStamp, otherwise retrieve all
     *                           Fedwire participants.
     * @param resultHandler      a new DownloadResult with data (if found) in FIXED text format.
     */
    void downloadFedwireParticipants(DownloadResult lastDownloadResult, Handler<AsyncResult<DownloadResult>> resultHandler);
}
