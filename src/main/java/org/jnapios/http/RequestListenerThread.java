package org.jnapios.http;

import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.HttpService;
import org.pmw.tinylog.Logger;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RequestListenerThread extends Thread {

    // Based on:
    // http://hc.apache.org/httpcomponents-core-ga/httpcore/examples/org/apache/http/examples/ElementalHttpServer.java
    // No relevant modifications made.

    private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
    private final ServerSocket serversocket;
    private final HttpService httpService;

    public RequestListenerThread(final int port, final HttpService httpService, final SSLServerSocketFactory sf) throws IOException {
        this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
        this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
        this.httpService = httpService;
    }

    @Override
    public void run() {
        Logger.info("Listening on port " + this.serversocket.getLocalPort());
        while (!Thread.interrupted()) {
            try {
                // Set up HTTP connection
                Socket socket = this.serversocket.accept();
                Logger.info("Incoming connection from " + socket.getInetAddress());
                HttpServerConnection conn = this.connFactory.createConnection(socket);

                // Start worker thread
                Thread t = new WorkerThread(this.httpService, conn);
                t.setDaemon(true);
                t.start();
            } catch (InterruptedIOException ex) {
                break;
            } catch (IOException e) {
                Logger.error("I/O error initialising connection thread: " + e.getMessage());
                break;
            }
        }
    }
}
