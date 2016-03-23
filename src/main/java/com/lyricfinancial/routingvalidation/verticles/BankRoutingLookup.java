/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.verticles;

import com.lyricfinancial.routingvalidation.services.DatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by Craig Earley on 1/29/16.
 */
public class BankRoutingLookup extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(BankRoutingLookup.class.getName());
    private DatabaseService databaseService;

    @Override
    public void start() {
        databaseService = DatabaseService.createProxy(vertx, Database.DATABASE_SERVICE);

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.get("/epayments/:routingNo/:bankName").handler(this::handleGetRoutingNumberAndBankName);
        router.get("/epayments/:routingNo").handler(this::handleGetRoutingNumber);
        router.get("/epayments").handler(this::handleListPayments);

        vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http.port", 8080));
    }

    private void handleGetRoutingNumberAndBankName(RoutingContext routingContext) {
        String routingNo = routingContext.request().getParam("routingNo");
        String bankName = routingContext.request().getParam("bankName");
        HttpServerResponse response = routingContext.response();
        if (routingNo == null || bankName == null) {
            sendError(400, response);
        } else {
            JsonObject criteria = new JsonObject().put("routingNo", routingNo).put("bankName", bankName);
            databaseService.findFedwireParticipants(criteria, asyncResult -> {
                JsonObject result = asyncResult.result();
                if (result == null) {
                    sendError(404, response);
                } else {
                    response.putHeader("content-type", "application/json").end(result.encodePrettily());
                }
            });
        }
    }

    private void handleGetRoutingNumber(RoutingContext routingContext) {
        String routingNo = routingContext.request().getParam("routingNo");
        logger.info("handleGetRoutingNumber: " + routingNo);
        HttpServerResponse response = routingContext.response();
        if (routingNo == null) {
            sendError(400, response);
        } else {
            JsonObject criteria = new JsonObject().put("routingNo", routingNo);
            databaseService.findFedwireParticipants(criteria, asyncResult -> {
                JsonObject result = asyncResult.result();
                if (result == null) {
                    sendError(404, response);
                } else {
                    response.putHeader("content-type", "application/json").end(result.encodePrettily());
                }
            });
        }
    }

    private void handleListPayments(RoutingContext routingContext) {
        databaseService.findFedwireParticipants(null, asyncResult -> routingContext.response().putHeader("content-type", "application/json").end(asyncResult.result().encodePrettily()));
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }
}
