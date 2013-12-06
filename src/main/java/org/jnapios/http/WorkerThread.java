package org.jnapios.http;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.pmw.tinylog.Logger;

import java.io.IOException;

public class WorkerThread extends Thread {

    // Based on:
    // http://hc.apache.org/httpcomponents-core-ga/httpcore/examples/org/apache/http/examples/ElementalHttpServer.java
    // No relevant modifications made.

    private final HttpService httpservice;
    private final HttpServerConnection conn;

    public WorkerThread(final HttpService httpservice, final HttpServerConnection conn) {
        super();
        this.httpservice = httpservice;
        this.conn = conn;
    }

    @Override
    public void run() {
        Logger.debug("New connection thread");
        HttpContext context = new BasicHttpContext(null);
        try {
            while (!Thread.interrupted() && this.conn.isOpen()) {
                this.httpservice.handleRequest(this.conn, context);
            }
        } catch (ConnectionClosedException ex) {
            Logger.debug("Client closed connection");
        } catch (IOException ex) {
            Logger.error("I/O error: " + ex.getMessage());
        } catch (HttpException ex) {
            Logger.error("Unrecoverable HTTP protocol violation: " + ex.getMessage());
        } finally {
            try {
                this.conn.shutdown();
            } catch (IOException ignore) {
            }
        }
    }
}