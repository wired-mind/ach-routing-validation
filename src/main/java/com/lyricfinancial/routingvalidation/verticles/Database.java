/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.verticles;

import com.lyricfinancial.routingvalidation.services.DatabaseService;
import com.lyricfinancial.routingvalidation.services.impl.DatabaseServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Created by Craig Earley on 1/29/16.
 */
public class Database extends AbstractVerticle {

    public static final String DATABASE_SERVICE = "database.service";
    private DatabaseService service;

    @Override
    public void start() throws Exception {
        super.start();

        service = new DatabaseServiceImpl(vertx, config());

        ProxyHelper.registerService(DatabaseService.class, vertx,
                service, DATABASE_SERVICE);
    }
}
