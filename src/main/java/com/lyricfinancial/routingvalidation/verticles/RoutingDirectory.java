/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.verticles;

import com.lyricfinancial.routingvalidation.services.RoutingDirectoryService;
import com.lyricfinancial.routingvalidation.services.impl.RoutingDirectoryServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Created by Craig Earley on 1/31/16.
 */
public class RoutingDirectory extends AbstractVerticle {

    public static final String ROUTING_DIRECTORY_SERVICE = "routing.directory.service";
    private RoutingDirectoryService service;

    @Override
    public void start() throws Exception {
        super.start();

        service = new RoutingDirectoryServiceImpl(vertx);

        ProxyHelper.registerService(RoutingDirectoryService.class, vertx,
                service, ROUTING_DIRECTORY_SERVICE);
    }
}
