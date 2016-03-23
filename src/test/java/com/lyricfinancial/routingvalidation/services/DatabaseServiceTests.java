/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.services;

import com.lyricfinancial.routingvalidation.data.DownloadResult;
import com.lyricfinancial.routingvalidation.services.impl.DatabaseServiceImpl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceproxy.ProxyHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

/**
 * Created by Craig Earley on 1/30/16.
 */
@RunWith(VertxUnitRunner.class)
public class DatabaseServiceTests {

    static final String DATABASE_SERVICE = "database.service.tests";
    static String bankData = null;
    Vertx vertx;
    AsyncSQLClient client;
    SQLConnection conn;
    DatabaseService databaseService;

    @Before
    public void init(TestContext context) {
        vertx = Vertx.vertx();

        JsonObject config = new JsonObject();

        client = PostgreSQLClient.createShared(vertx,
                config
                        .put("host", "postgres.dev")
                        .put("database", "postgres")
                        .put("username", "postgres")
                        .put("password", "A9d8zXUm8UxA1DLudCP0")
        );

        ProxyHelper.registerService(DatabaseService.class, vertx,
                new DatabaseServiceImpl(vertx, config), DATABASE_SERVICE);

        databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE);

        upsertTestData(context);
    }

    @After
    public void cleanup(TestContext context) {
        if (conn != null) {
            conn.close(event -> client.close(closed -> vertx.close(context.asyncAssertSuccess())));
        } else if (client != null) {
            client.close(closed -> vertx.close(context.asyncAssertSuccess()));
        }
    }

    @Test
    public void canGetLastSuccessfulDownloadResult(TestContext context) throws Exception {
        Async async = context.async();

        databaseService.getLastSuccessfulDownloadResult(asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            } else {
                DownloadResult result = asyncResult.result();
                context.assertNotNull(result, "result should not be null");
                context.assertTrue(result.isFound, "result.isFound should be true");

                System.out.println(result);
                async.complete();
            }
        });
    }

    @Test
    public void canSave(TestContext context) throws Exception {
        Async async = context.async();

        String data = "011000015FRB-BOS           FEDERAL RESERVE BANK OF BOSTON      MABOSTON                   Y Y20040910\n" +
                "011000028STATE ST BOS      STATE STREET'BOSTON                 MABOSTON                   Y Y        \n" +
                "011000536FHLB BOSTON       FEDERAL HOME'LOAN BANK'             MABOSTON                   Y Y        \n" +
                "011001234MELLON TRUST OF NETHE BANK OF NEW YORK MELLON         MABOSTON                   Y N20120815\n" +
                " \n" +
                "011001276ONEUNITED BANK    ONEUNITED BANK                      MABOSTON                   Y Y20021231\n" +
                "\n";

        data = getBankData();

        DownloadResult downloadResult = new DownloadResult(data, true, "test");

        databaseService.saveFedwireParticipants(downloadResult, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            } else {
                System.out.println(asyncResult.result());
                async.complete();
            }
        });
    }

    @Test
    public void canRetrieveAllRecords(TestContext context) throws Exception {
        Async async = context.async();

        databaseService.findFedwireParticipants(null, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            } else {
                JsonObject result = asyncResult.result();
                JsonArray rows = result.getJsonArray("rows");

                context.assertTrue(rows.size() >= 2, "There should be at least two rows");

                System.out.println(result);
                async.complete();
            }
        });
    }

    @Test
    public void canFindByRoutingNumber(TestContext context) throws Exception {
        Async async = context.async();

        JsonObject criteria = new JsonObject()
                .put("routingNo", "123456789");

        databaseService.findFedwireParticipants(criteria, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            } else {
                JsonObject result = asyncResult.result();
                JsonArray rows = result.getJsonArray("rows");

                context.assertTrue(rows.size() == 2, "There should be two rows");

                System.out.println(result);
                async.complete();
            }
        });
    }

    @Test
    public void canFindByRoutingNumberAndBankName(TestContext context) throws Exception {
        Async async = context.async();

        JsonObject criteria = new JsonObject()
                .put("routingNo", "123456789")
                .put("bankName", "ONE");

        databaseService.findFedwireParticipants(criteria, asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
            } else {
                JsonObject result = asyncResult.result();
                JsonArray rows = result.getJsonArray("rows");

                context.assertTrue(rows.size() == 1, "There should be one row");

                System.out.println(result);
                async.complete();
            }
        });
    }

    /**
     * Insert test data without using the service.
     *
     * @param context
     */
    private void upsertTestData(TestContext context) {
        Async async = context.async();

        client.getConnection(asyncResult -> {
            if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
                return;
            }

            conn = asyncResult.result();

            // Data in common table expression format
            String data = "('123456789', 'TEST BANK ONE', 'TEST BANK ONE OF PORTLAND', 'ME', 'PORTLAND', 'Y', 'N', 'Y', '20160204')," +
                    "('123456789', 'TEST BANK TWO', 'TEST BANK TWO OF NEW YORK', 'NY', 'NY', 'N', 'Y', 'N', '20160204')";

            String upsert = String.format(DatabaseServiceImpl.UPSERT_SQL, data);

            conn.update(upsert, asyncUpdate -> {
                if (asyncUpdate.failed()) {
                    context.fail(asyncUpdate.cause());
                } else {
                    String insert = "INSERT INTO download_results(\n" +
                            "            date_time_stamp, data, is_found, reason)\n" +
                            "    VALUES (?, ?, ?, ?)";
                    JsonArray params = new JsonArray()
                            .add(LocalDateTime.now().format(DownloadResult.formatter))
                            .add("Test")
                            .add(true)
                            .add("Test");
                    conn.updateWithParams(insert, params, asyncResult1 -> {
                        if (asyncUpdate.failed()) {
                            context.fail(asyncUpdate.cause());
                        } else {
                            async.complete();
                        }
                    });
                }
            });
        });
    }

    public String getBankData() throws IOException {
        if (null == bankData) {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("BankData.txt").getFile());
            String fileString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            bankData = fileString;
        }
        return bankData;
    }
}