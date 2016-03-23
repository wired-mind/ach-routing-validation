/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.verticles;

import com.lyricfinancial.routingvalidation.services.DatabaseService;
import com.lyricfinancial.routingvalidation.services.impl.DatabaseServiceImpl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ProxyHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Craig Earley on 1/31/16.
 */
@RunWith(VertxUnitRunner.class)
public class BankRoutingLookupTests {

    Vertx vertx;

    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();

        JsonObject config = new JsonObject();

        config
                .put("host", "postgres.dev")
                .put("database", "postgres")
                .put("username", "postgres")
                .put("password", "A9d8zXUm8UxA1DLudCP0");

        ProxyHelper.registerService(DatabaseService.class, vertx,
                new DatabaseServiceImpl(vertx, config), Database.DATABASE_SERVICE);

        vertx.deployVerticle(BankRoutingLookup.class.getName(),
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void canRetrieveAllRecords(TestContext context) throws Exception {
        final Async async = context.async();

        vertx.createHttpClient().getNow(8082, "ach-routing-validation.dev", "/payments",
                response -> response.handler(body -> {
                    String data = body.toString();
                    context.assertTrue(data.contains("params"));
                    async.complete();
                }));
    }
}