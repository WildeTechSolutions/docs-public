package com.thomaswilde.api;

import org.apache.hc.core5.http.Header;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Response {
    private String body;
    private int code;
    private Map<String, String> headers = new HashMap<>();

    public Response() {
    }

    public Response(String body, int code) {
        this.body = body;
        this.code = code;
    }

    public Response(String body, int code, Header[] headers) {
        this.body = body;
        this.code = code;

        if(headers != null){
            for(Header header : headers){
                this.headers.put(header.getName(), header.getValue());
            }
        }

    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
