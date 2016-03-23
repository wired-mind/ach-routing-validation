/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.services.impl;

import com.lyricfinancial.routingvalidation.data.DownloadResult;
import com.lyricfinancial.routingvalidation.services.DatabaseService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Postgres database service implementation
 * for E-Payments Routing Directories.
 * <p>
 * Created by Craig Earley on 1/29/16.
 */
public class DatabaseServiceImpl implements DatabaseService {
    // 'values' uses format placeholder (%s); provide values in common table expression format
    public static final String UPSERT_SQL = "WITH new_values (a, b, c, d, e, f, g, h, i) AS (\n" +
            "  values \n" +
            "    %s\n" +
            "),\n" +
            "upsert AS (\n" +
            "  UPDATE public.bank_routing_lookup m\n" +
            "    SET\n" +
            "      name = nv.c,\n" +
            "      state = nv.d,\n" +
            "      city = nv.e,\n" +
            "      transfer_eligible = nv.f,\n" +
            "      settlement_only = nv.g,\n" +
            "      securities_transfer_status = nv.h,\n" +
            "      date_last_revised = nv.i\n" +
            "    FROM new_values nv\n" +
            "    WHERE m.routing_number=nv.a AND m.short_name=nv.b\n" +
            "RETURNING m.*) \n" +
            "INSERT INTO public.bank_routing_lookup (\n" +
            "  routing_number, \n" +
            "  short_name, \n" +
            "  name, \n" +
            "  state,\n" +
            "  city,\n" +
            "  transfer_eligible,\n" +
            "  settlement_only,\n" +
            "  securities_transfer_status,\n" +
            "  date_last_revised\n" +
            " )\n" +
            "SELECT a, b, c, d, e, f, g, h, i\n" +
            "FROM new_values\n" +
            "WHERE NOT EXISTS (SELECT 1 FROM upsert up \n" +
            "  WHERE up.routing_number = new_values.a AND up.short_name = new_values.b\n" +
            ")";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class.getName());
    private AsyncSQLClient client;

    private DatabaseServiceImpl() {
    }

    public DatabaseServiceImpl(Vertx vertx, JsonObject config) {
        client = PostgreSQLClient.createShared(vertx,
                new JsonObject()
                        .put("host", config.getString("db.host", "postgres.dev"))
                        .put("database", config.getString("db.username", "postgres"))
                        .put("username", config.getString("db.username", "postgres"))
                        .put("password", config.getString("db.password"))
        );
    }

    @Override
    public void getLastSuccessfulDownloadResult(Handler<AsyncResult<DownloadResult>> resultHandler) {
        client.getConnection(asyncResult -> {
            if (asyncResult.failed()) {
                logger.error(asyncResult.cause());
                resultHandler.handle(Future.failedFuture(asyncResult.cause()));
                return;
            }

            String sql = "SELECT date_time_stamp as datetimestamp, data, is_found as isfound, reason\n" +
                    "  FROM download_results\n" +
                    "  WHERE is_found = true\n" +
                    "  ORDER BY date_time_stamp DESC\n" +
                    "  LIMIT 1";
            SQLConnection conn = asyncResult.result();
            conn.query(sql, asyncQueryResult -> {
                if (asyncQueryResult.failed()) {
                    logger.error(asyncQueryResult.cause());
                    resultHandler.handle(Future.failedFuture(asyncQueryResult.cause()));
                    conn.close();
                    return;
                }

                List<JsonObject> rows = asyncQueryResult.result().getRows();
                if (rows.size() == 0) {
                    resultHandler.handle(Future.succeededFuture(null));
                } else {
                    DownloadResult result = new DownloadResult(rows.get(0));
                    resultHandler.handle(Future.succeededFuture(result));
                }
                conn.close();
            });
        });

    }

    @Override
    public void saveFedwireParticipants(DownloadResult downloadResult, Handler<AsyncResult<JsonObject>> resultHandler) {
        client.getConnection(asyncResult -> {
            if (asyncResult.failed()) {
                logger.error(asyncResult.cause());
                resultHandler.handle(Future.failedFuture(asyncResult.cause()));
                return;
            }

            String upsertSql = null;
            try {
                upsertSql = String.format(UPSERT_SQL, fedwireFormatToCommonTableFormat(downloadResult.data));
            } catch (Exception e) {
                logger.error(e);
                resultHandler.handle(Future.failedFuture(e));
                return;
            }

            JsonObject result = new JsonObject();

            SQLConnection conn = asyncResult.result();
            conn.update(upsertSql, asyncUpdateResult -> {
                if (asyncUpdateResult.failed()) {
                    logger.error(asyncUpdateResult.cause());
                    resultHandler.handle(Future.failedFuture(asyncUpdateResult.cause()));
                    conn.close();
                    return;
                }

                result.put("participants", asyncUpdateResult.result().toJson());
                logger.info(result);

                String query = "INSERT INTO download_results(\n" +
                        "            date_time_stamp, data, is_found)\n" +
                        "    VALUES (?, ?, ?)";
                JsonArray params = new JsonArray()
                        .add(downloadResult.dateTimeStamp)
                        .add(downloadResult.data)
                        .add(downloadResult.isFound);

                conn.updateWithParams(query, params, res -> {
                    if (res.failed()) {
                        logger.error(res.cause());
                        resultHandler.handle(Future.failedFuture(res.cause()));
                        conn.close();
                        return;
                    }
                    result.put("downloadResults", res.result().toJson());
                    resultHandler.handle(Future.succeededFuture(result));
                    conn.close();
                });
            });
        });
    }

    @Override
    public void findFedwireParticipants(JsonObject criteria, Handler<AsyncResult<JsonObject>> resultHandler) {
        client.getConnection(asyncResult -> {
            if (asyncResult.failed()) {
                logger.error(asyncResult.cause());
                resultHandler.handle(Future.failedFuture(asyncResult.cause()));
                return;
            }

            String query = "SELECT * FROM public.bank_routing_lookup";
            JsonArray params = new JsonArray();
            if (criteria != null && criteria.containsKey("routingNo")) {
                logger.info("Looking up: " + criteria.getString("routingNo"));
                query += " t WHERE t.routing_number = ?";
                params.add(criteria.getString("routingNo"));

                if (criteria.containsKey("bankName")) {
                    logger.info("Refining query where bank name matches: " + criteria.getString("bankName"));
                    query += " AND t.name LIKE ?";
                    params.add(String.format("%%%s%%", criteria.getString("bankName")));
                }
            }

            SQLConnection conn = asyncResult.result();
            conn.queryWithParams(query, params, res -> {
                if (res.failed()) {
                    logger.error(res.cause());
                    resultHandler.handle(Future.failedFuture(res.cause()));
                    conn.close();
                    return;
                }

                JsonObject result = new JsonObject();
                result.put("params", params);
                result.put("rows", res.result().getRows());

                resultHandler.handle(Future.succeededFuture(result));
                conn.close();
            });
        });
    }

    @Override
    public void close() {
        if (client != null) {
            client.close(asyncResult -> {
            });
        }
    }

    private String fedwireFormatToCommonTableFormat(String text) throws Exception {
        // See https://www.frbservices.org/EPaymentsDirectory/fedwireFormat.html

        // Remove empty lines - courtesy http://stackoverflow.com/questions/4123385/remove-all-empty-lines
        String adjusted = text.replaceAll("(?m)^[ \t]*\r?\n", "");

        BufferedReader reader = new BufferedReader(new StringReader(adjusted));
        return reader.lines().map(s -> "('" +
                s.substring(0, 9) + "','" +
                s.substring(9, 27).replace("'", "''") + "','" +
                s.substring(27, 63).replace("'", "''") + "','" +
                s.substring(63, 65) + "','" +
                s.substring(65, 90).replace("'", "''") + "','" +
                s.substring(90, 91) + "','" +
                s.substring(91, 92) + "','" +
                s.substring(92, 93) + "','" +
                s.substring(93, 101) + "')")
                .collect(Collectors.joining(","));
    }
}
