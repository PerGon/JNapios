package org.jnapios.http;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.jnapios.parser.Parser;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class HttpFileHandler implements HttpRequestHandler {

    // Based on:
    // http://hc.apache.org/httpcomponents-core-ga/httpcore/examples/org/apache/http/examples/ElementalHttpServer.java
    // No relevant modifications made.

    private final File file;

    public HttpFileHandler(final File file) {
        super();
        this.file = file;
    }

    @Override
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {

        long time = System.currentTimeMillis();
        Logger.debug("Request recieved. Iniciating...");

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }

        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            byte[] entityContent = EntityUtils.toByteArray(entity);
            Logger.info("Incoming entity content (bytes): " + entityContent.length);
        }

        if (!file.exists()) {

            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            StringEntity entity = new StringEntity("<html><body><h1>File " + file.getPath() + " not found</h1></body></html>", ContentType.create("text/html", "UTF-8"));
            response.setEntity(entity);
            Logger.info("File " + file.getPath() + " not found");

        } else if (!file.canRead() || file.isDirectory()) {

            response.setStatusCode(HttpStatus.SC_FORBIDDEN);
            StringEntity entity = new StringEntity("<html><body><h1>Access denied</h1></body></html>", ContentType.create("text/html", "UTF-8"));
            response.setEntity(entity);
            Logger.info("Cannot read file " + file.getPath());

        } else {
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity body = new StringEntity(Parser.parser(file));
            response.setEntity(body);
            Logger.debug("Serving file " + file.getPath());
        }
        Logger.info("Request proccessed. Took: " + (System.currentTimeMillis() - time) + " ms");
    }
}