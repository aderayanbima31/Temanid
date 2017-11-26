package com.mra.temanid.service;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by robbyseptian on 22/11/17.
 * 081236821613
 */

public class TemanClientServer {
    private static final String BASE_URL = "https://api.mainapi.net/wifiidlocator/v1.0/lonlat";

    private static AsyncHttpClient client = new AsyncHttpClient();


    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(BASE_URL), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.removeAllHeaders();
        client.addHeader("X-API-KEY","bbd310e2-b356-4dfb-b674-aad3c73d19d5");
        client.addHeader("Authorization","Bearer 85f553c9ab2c4efe6fc38fde07c185f0");
        //client.addHeader("Content-Type","application/json");
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

}
