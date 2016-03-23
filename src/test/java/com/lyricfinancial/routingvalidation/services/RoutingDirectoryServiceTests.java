/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.services;

import com.lyricfinancial.routingvalidation.data.DownloadResult;
import com.lyricfinancial.routingvalidation.services.impl.RoutingDirectoryServiceImpl;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ProxyHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by Craig Earley on 1/30/16.
 */
@RunWith(VertxUnitRunner.class)
public class RoutingDirectoryServiceTests {

    static final String ROUTING_DIRECTORY_SERVICE = "routing.directory.service.tests";

    RoutingDirectoryService routingDirectoryService;
    Vertx vertx;

    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();

        ProxyHelper.registerService(RoutingDirectoryService.class, vertx,
                new RoutingDirectoryServiceImpl(vertx), ROUTING_DIRECTORY_SERVICE);

        routingDirectoryService = RoutingDirectoryService.createProxy(vertx, ROUTING_DIRECTORY_SERVICE);
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void canDownloadAllRoutingDirectories(TestContext context) throws Exception {
        Async async = context.async();

        routingDirectoryService.downloadFedwireParticipants(null, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            }

            DownloadResult downloadResult = asyncResult.result();
            context.assertNotNull(downloadResult, "DownloadResult should not be null");
            context.assertEquals(LocalDateTime.parse(downloadResult.dateTimeStamp, DownloadResult.formatter).toLocalDate(),
                    LocalDate.now(), "DownloadResult dateTimeStamp should be today");
            context.assertNotNull(downloadResult.data, "DownloadResult data should not be null");
            context.assertTrue(downloadResult.isFound, "DownloadResult isFound should be true");
            context.assertNull(downloadResult.reason, "DownloadResult reason should be null");

            async.complete();
            System.out.println(downloadResult);
        });
    }

    @Test
    public void canDownloadRoutingDirectoriesUpdatedInLastFiveDays(TestContext context) throws Exception {
        Async async = context.async();

        LocalDateTime localDateTime = LocalDateTime.now().minus(5, ChronoUnit.DAYS);
        String formattedDate = localDateTime.format(DownloadResult.formatter);

        DownloadResult fakeDownloadResult = new DownloadResult(formattedDate, null, false, null);

        routingDirectoryService.downloadFedwireParticipants(fakeDownloadResult, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            }

            DownloadResult downloadResult = asyncResult.result();
            context.assertNotNull(downloadResult, "DownloadResult should not be null");
            context.assertEquals(LocalDateTime.parse(downloadResult.dateTimeStamp, DownloadResult.formatter).toLocalDate(),
                    LocalDate.now(), "DownloadResult dateTimeStamp should be today");
            context.assertNotNull(downloadResult.data, "DownloadResult data should not be null");
            context.assertTrue(downloadResult.isFound, "DownloadResult isFound should be true");
            context.assertNull(downloadResult.reason, "DownloadResult reason should be null");

            async.complete();
            System.out.println(downloadResult);
        });
    }

    @Test
    public void canHandleNoUpdatesSinceGivenDate(TestContext context) throws Exception {
        Async async = context.async();

        LocalDateTime localDateTime = LocalDateTime.now();
        String formattedDate = localDateTime.format(DownloadResult.formatter);

        DownloadResult fakeDownloadResult = new DownloadResult(formattedDate, null, false, null);

        routingDirectoryService.downloadFedwireParticipants(fakeDownloadResult, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            }

            DownloadResult downloadResult = asyncResult.result();
            context.assertNotNull(downloadResult, "DownloadResult should not be null");
            context.assertEquals(LocalDateTime.parse(downloadResult.dateTimeStamp, DownloadResult.formatter).toLocalDate(),
                    LocalDate.now(), "DownloadResult dateTimeStamp should be today");
            context.assertNull(downloadResult.data, "DownloadResult data should be null");
            context.assertFalse(downloadResult.isFound, "DownloadResult isFound should not be true");
            context.assertNotNull(downloadResult.reason, "DownloadResult reason should not be null");
            context.assertTrue(downloadResult.reason.startsWith("There have been no changes"));

            async.complete();
            System.out.println(downloadResult);
        });
    }
}