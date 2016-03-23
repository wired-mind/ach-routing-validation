/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.services.impl;

import com.jaunt.HttpRequest;
import com.jaunt.NotFound;
import com.jaunt.UserAgent;
import com.jaunt.component.Form;
import com.jaunt.util.HandlerForText;
import com.lyricfinancial.routingvalidation.data.DownloadResult;
import com.lyricfinancial.routingvalidation.services.RoutingDirectoryService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Routing Directories service implementation for
 * E-Payments. Retrieves records directly from the
 * Federal Reserve Bank Service. Because the FRBS
 * does not provide an API, web-scraping techniques
 * are used to handle expected user interactions,
 * such as accepting the download agreement.
 * <p>
 * Created by Craig Earley on 1/29/16.
 */
public class RoutingDirectoryServiceImpl implements RoutingDirectoryService {
    private static final Logger logger = LoggerFactory.getLogger(RoutingDirectoryServiceImpl.class.getName());
    private static final String dataUrl = "https://www.frbservices.org/EPaymentsDirectory/fpddir.txt";

    private UserAgent userAgent;
    private HandlerForText handlerForText;
    private Vertx vertx;

    private RoutingDirectoryServiceImpl() {
    }

    public RoutingDirectoryServiceImpl(Vertx vertx) {
        this.vertx = vertx;

        userAgent = new UserAgent();

        userAgent.settings.connectTimeout = 30_000;
        userAgent.settings.readTimeout = 30_000;

        handlerForText = new HandlerForText();
        userAgent.setHandler("text/plain", handlerForText);
    }

    @Override
    public void downloadFedwireParticipants(DownloadResult lastDownloadResult, Handler<AsyncResult<DownloadResult>> resultHandler) {
        vertx.executeBlocking(future -> {
            try {
                userAgent.visit("https://www.frbservices.org/EPaymentsDirectory/download.html");

                try {
                    // Handle agreement redirect
                    Form acceptedForm = userAgent.doc.getForm("<form name=acceptedForm>");
                    HttpRequest httpRequest = acceptedForm.getRequest();
                    httpRequest.addNameValuePair("agreementValue", "Agree");
                    userAgent.send(httpRequest);
                } catch (NotFound nf) {
                    logger.info("UserAgent already accepted agreement");
                }

                String url = dataUrl; // url to download all Fedwire participants (default)

                if (lastDownloadResult != null) {
                    LocalDateTime dateTime = LocalDateTime.parse(lastDownloadResult.dateTimeStamp, DownloadResult.formatter);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/YYYY");
                    String formattedDate = dateTime.toLocalDate().format(formatter);

                    logger.info("Downloading updates since " + formattedDate);

                    // Adjust url to download updates since date of lastDownloadResult
                    url = String.format("%s?updatedSinceFedwire=%s&downloadFedwireButton=",
                            dataUrl, URLEncoder.encode(formattedDate, "UTF-8"));
                } else {
                    logger.info("Downloading all records");
                }
                userAgent.visit(url);

                if (!userAgent.response.getRequestedUrlMsg().equals(url)) {
                    String message = String.format("Requested url should be %s but found %s", url, userAgent.response.getRequestedUrlMsg());
                    throw new Exception(message);
                }

                String content = handlerForText.getContent();
                if (content.startsWith("There have been no changes")) {
                    future.complete(new DownloadResult(null, false, content));
                    return;
                }
                future.complete(new DownloadResult(content, true, null));
            } catch (Exception ex) {
                future.fail(ex);
            }
        }, new Handler<AsyncResult<DownloadResult>>() {
            @Override
            public void handle(AsyncResult<DownloadResult> asyncResult) {
                if (asyncResult.failed()) {
                    resultHandler.handle(Future.failedFuture(asyncResult.cause()));
                    return;
                }
                resultHandler.handle(Future.succeededFuture(asyncResult.result()));
            }
        });
    }
}
