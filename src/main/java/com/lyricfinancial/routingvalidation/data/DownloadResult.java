/*
 * Copyright (c) 2016, Craig Earley. All Rights Reserved.
 */

package com.lyricfinancial.routingvalidation.data;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Download result for E-Payments Routing Directories.
 * <p>
 * Created by Craig Earley on 1/28/16.
 */
@DataObject
public class DownloadResult {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public final String dateTimeStamp;
    public final String data;
    public final boolean isFound;
    public final String reason;

    public DownloadResult() {
        this(LocalDateTime.now().format(formatter), null, false, null);
    }

    public DownloadResult(DownloadResult other) {
        this(other.dateTimeStamp, other.data, other.isFound, other.reason);
    }

    public DownloadResult(JsonObject json) {
        this(json.getString("datetimestamp", LocalDateTime.now().format(formatter)),
                json.getString("data"),
                json.getBoolean("isfound"),
                json.getString("reason"));
    }

    public DownloadResult(String data, boolean isFound, String reason) {
        this(LocalDateTime.now().format(formatter), data, isFound, reason);
    }

    public DownloadResult(String dateTimeStamp, String data, boolean isFound, String reason) {
        this.dateTimeStamp = dateTimeStamp;
        this.data = data;
        this.isFound = isFound;
        this.reason = reason;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("datetimestamp", dateTimeStamp)
                .put("data", data)
                .put("isfound", isFound)
                .put("reason", reason);
    }

    @Override
    public String toString() {
        return "DownloadResult{" +
                "datetimestamp=" + dateTimeStamp +
                ", data='" + data + '\'' +
                ", isfound=" + isFound +
                ", reason='" + reason + '\'' +
                '}';
    }
}
