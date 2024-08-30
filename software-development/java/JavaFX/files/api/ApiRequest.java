package com.thomaswilde.api;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.utils.URIUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiRequest {

    private static Logger log = LoggerFactory.getLogger(ApiRequest.class);

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String PATCH = "PATCH";
    public static final String DELETE = "DELETE";

    public enum MethodType{
        GET, POST, PUT, PATCH, DELETE
    }

    public interface ProgressCallback{
        void update(float percentage);
    }

    private static ApiRequest apiRequest;

    public static ApiRequest getInstance(){
        if(apiRequest == null){
            apiRequest = new ApiRequest();
        }
        return apiRequest;
    }

    private RequestConfig config;

    private ApiRequest(){
        int timeout = 5;
        config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(timeout))
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeout))
                .build();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private MethodType methodType;
        private String baseUrl;
        private String endPoint;
        private Map<String, String> params;
        private Map<String, String> bodyParams;
        private Map<String, String> headers;
        private String body;
        private Path binary;
        private Path downloadPath;
        private ProgressCallback progressCallback;

        public Builder setMethodType(MethodType methodType) {
            this.methodType = methodType;
            return this;
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setEndPoint(String endPoint) {
            this.endPoint = endPoint;
            return this;
        }

        public Builder setParams(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setBinary(Path binary) {
            this.binary = binary;
            return this;
        }

        public Builder setBodyParams(Map<String, String> bodyParams) {
            this.bodyParams = bodyParams;
            return this;
        }

        public Builder setProgressCallback(ProgressCallback progressCallback) {
            this.progressCallback = progressCallback;
            return this;
        }

        public Builder setDownloadPath(Path downloadPath) {
            this.downloadPath = downloadPath;
            return this;
        }

        public Response send() throws IOException {
            if(methodType == null){
                log.error("METHOD type in request is null");
                return null;
            }
            return getInstance().request(methodType.toString(), baseUrl, endPoint, params, body, headers);
        }

        public Response upload() throws IOException {
            if(methodType == null){
                methodType = MethodType.POST;
            }
            return getInstance().upload(methodType.toString(), baseUrl, endPoint, params, binary, bodyParams, headers, progressCallback);
        }

        public Response download() throws IOException {
            if(methodType == null){
                methodType = MethodType.GET;
            }
            return getInstance().download(methodType.toString(), baseUrl, endPoint, params, downloadPath, bodyParams, headers, progressCallback);
        }
    }



    public Response get(String baseUrl, String endPoint, Map<String, String> params, Map<String, String> headers) throws IOException {
        return request(GET, baseUrl, endPoint, params, null, headers);
    }

    public Response post(String baseUrl,String endPoint, String bodyJson, Map<String, String> headers) throws IOException {
        return request(POST, baseUrl, endPoint, null, bodyJson, headers);
    }

    public Response put(String baseUrl, String endPoint, String bodyJson, Map<String, String> headers) throws IOException {
        return request(PUT, baseUrl, endPoint, null, bodyJson, headers);
    }

    public Response patch(String baseUrl, String endPoint, String bodyJson, Map<String, String> headers) throws IOException {
        return request(PATCH, baseUrl, endPoint, null, bodyJson, headers);
    }

    public Response delete(String baseUrl, String endPoint, String bodyJson, Map<String, String> headers) throws IOException {
        return request(DELETE, baseUrl, endPoint, null, bodyJson, headers);
    }

    public Response download(String methodName, String baseUrl, String endPoint, Map<String, String> params, Path downloadPath, Map<String, String> bodyParams, Map<String, String> headers, ProgressCallback progressCallback) throws IOException {
        log.debug("Method {}, baseUrl: {}, endPoint: {}", methodName, baseUrl, endPoint);

        HttpUriRequestBase httpRequest;

        String responseString = null;
        int code = -1;
        Header[] returnHeaders = null;
        try(
                CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()
        ){
            String url = constructUrl(baseUrl, endPoint, params);

            log.debug("Url: {}", url);

            switch (methodName) {
                case PUT:
                    httpRequest = new HttpPut(url);
                    break;
                case POST:
                    httpRequest = new HttpPost(url);
                    break;
                default:
                    httpRequest = new HttpGet(url);
                    break;
            }

            httpRequest.setHeader("Accept", "application/json;odata=nometadata");

            if(headers != null){
                headers.forEach(httpRequest::addHeader);
            }

            try (CloseableHttpResponse response = client.execute(httpRequest)) {

                if(response.getEntity() != null){

//                HttpEntity entity = response.getEntity();

                    code = response.getCode();
                    returnHeaders = response.getHeaders();

                    ProgressListener pListener = percentage -> {
                        if(progressCallback != null){
                            progressCallback.update(percentage);
                        }
                    };

                    HttpEntity entity = new ProgressEntityWrapper(response.getEntity(), pListener);

                    try (FileOutputStream outstream = new FileOutputStream(downloadPath.toFile())) {
                        entity.writeTo(outstream);
                    }

                }


            }


            return new Response(Integer.toString(code), code, returnHeaders);
        }
    }

    public Response upload(String methodName, String baseUrl, String endPoint, Map<String, String> params, Path binary, Map<String, String> bodyParams, Map<String, String> headers, ProgressCallback progressCallback) throws IOException {

        log.debug("Method {}, baseUrl: {}, endPoint: {}", methodName, baseUrl, endPoint);

        HttpUriRequestBase httpRequest;

        String responseString = null;
        int code = -1;
        Header[] returnHeaders = null;

        try(
                CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()
        ){
            String url = constructUrl(baseUrl, endPoint, params);

            log.debug("Url: {}", url);

            switch (methodName) {
                case PUT:
                    httpRequest = new HttpPut(url);
                    break;
                default:
                    httpRequest = new HttpPost(url);
                    break;
            }

            // Generate a unique boundary string
            String boundary = "-------------" + System.currentTimeMillis();

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", binary.toFile(), ContentType.MULTIPART_FORM_DATA, binary.getFileName().toString());
            if(bodyParams != null){
                bodyParams.forEach((key, value) -> builder.addTextBody(key, value, ContentType.MULTIPART_FORM_DATA));
            }

            builder.setBoundary(boundary);

            HttpEntity multipart = builder.build();

            // Add a json body if the body is not null
            //            builder.addTextBody("jsonData", body, ContentType.APPLICATION_JSON);

            ProgressListener pListener = percentage -> {
                if(progressCallback != null){
                    progressCallback.update(percentage);
                }
            };

            httpRequest.setEntity(new ProgressEntityWrapper(multipart, pListener));
            httpRequest.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
            httpRequest.setHeader("Accept", "application/json");

            if(headers != null){
                headers.forEach(httpRequest::addHeader);
            }

            for (Header header : httpRequest.getHeaders()) {
                log.debug("Header: {}, Value: {}", header.getName(), header.getValue());
            }


            try (CloseableHttpResponse response = client.execute(httpRequest)) {

                code = response.getCode();
                HttpEntity entity = response.getEntity();
                returnHeaders = response.getHeaders();

                responseString = EntityUtils.toString(entity, "UTF-8");

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return new Response(responseString, code, returnHeaders);

    }

    public Response request(String methodName, String baseUrl, String endPoint, Map<String, String> params, String bodyJson, Map<String, String> headers) throws IOException {

        log.debug("Preparing request Method {}, baseUrl: {}, endPoint: {}", methodName, baseUrl, endPoint);

        String responseString = null;
        int code = -1;
        Header[] returnHeaders = null;
        try(
                CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()
        ){

            String url = constructUrl(baseUrl, endPoint, params);

            log.debug("Url: {}", url);

            HttpUriRequestBase httpRequest;

            switch (methodName){
                case GET:
                    httpRequest = new HttpGet(url);
                    break;
                case POST:
                    httpRequest = new HttpPost(url);
                    break;
                case PUT:
                    httpRequest = new HttpPut(url);
                    break;
                case PATCH:
                    httpRequest = new HttpPatch(url);
                    break;
                case DELETE:
                    httpRequest = new HttpDelete(url);
                    break;
                default:
                    httpRequest = new HttpGet(url);
                    break;
            }
            if (bodyJson != null && !bodyJson.isEmpty()) {
                httpRequest.setEntity(new StringEntity(bodyJson));
                httpRequest.setHeader("Accept", "application/json");
                httpRequest.setHeader("Content-type", "application/json");
            }


            if(headers != null){
                headers.forEach(httpRequest::setHeader);
            }

            log.debug("Sending request: Method {}, baseUrl: {}, endPoint: {}", httpRequest.getMethod(), baseUrl, endPoint);


            try (CloseableHttpResponse response = client.execute(httpRequest)) {
                code = response.getCode();
                HttpEntity entity = response.getEntity();
                returnHeaders = response.getHeaders();

                responseString = EntityUtils.toString(entity, "UTF-8");


            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return new Response(responseString, code, returnHeaders);
    }

    private String constructUrl(String baseUrl, String endPoint){
        return constructUrl(baseUrl, endPoint, null);
    }
    private String constructUrl(String baseUrl, String endPoint, Map<String, String> params) {
        String url;
        if(endPoint != null){
            url = baseUrl + endPoint;
        }else{
            url = baseUrl;
        }


        if (params != null && !params.isEmpty()) {
            List<String> paramComponents = new ArrayList<>();
            params.forEach((key, value) -> paramComponents.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));

            url += "?" + String.join("&", paramComponents);
        }

        return url;
    }

    public static interface ProgressListener {
        void progress(float percentage);
    }

    public class ProgressEntityWrapper extends HttpEntityWrapper {
        private ProgressListener listener;

        public ProgressEntityWrapper(HttpEntity entity, ProgressListener listener) {
            super(entity);
            this.listener = listener;
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            super.writeTo(new CountingOutputStream(outstream, listener, getContentLength()));
        }
    }

    public static class CountingOutputStream extends FilterOutputStream {
        private ProgressListener listener;
        private long transferred;
        private long totalBytes;

        public CountingOutputStream(
                OutputStream out, ProgressListener listener, long totalBytes) {
            super(out);
            this.listener = listener;
            transferred = 0;
            this.totalBytes = totalBytes;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            transferred += len;
            listener.progress(getCurrentProgress());
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            transferred++;
            listener.progress(getCurrentProgress());
        }

        private float getCurrentProgress() {
            return ((float) transferred / totalBytes) * 100;
        }
    }
}
