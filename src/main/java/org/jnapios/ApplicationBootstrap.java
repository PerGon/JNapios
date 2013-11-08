package org.jnapios;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;
import org.jnapios.parser.Parser;
import org.jnapios.tinylog.MultiWriter;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.labellers.TimestampLabeller;
import org.pmw.tinylog.policies.DailyPolicy;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.RollingFileWriter;

public class ApplicationBootstrap {

	// Based on:
	// http://hc.apache.org/httpcomponents-core-ga/httpcore/examples/org/apache/http/examples/ElementalHttpServer.java


	private static int port = 0;
	private static String filePath = null;
	private static String logFolder = null;


	public static void main(String[] args) throws Exception {

		configureTinyLog();

		if (!getPortArg(args) || !getStatusFileArg(args) || !getLogFolderArg(args)) {
			Logger.error("Not all mandatory args where specified or errors in the arguments.");
			Logger.error("Provide \"port\", \"logFolder\"  and \"statusFile\" arguments ");
			Logger.error("Aborting...");
			return;
		}

		configureTinyLogFolder();

		Logger.info("###Starting application JNAPIOS.###");

		// Set up the HTTP protocol processor
		HttpProcessor httpproc = HttpProcessorBuilder.create().add(new ResponseDate()).add(new ResponseServer("Test/1.1")).add(new ResponseContent()).add(new ResponseConnControl()).build();

		// Set up request handlers
		UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
		reqistry.register("/jnapios/state", new HttpFileHandler(new File(filePath)));

		// Set up the HTTP service
		HttpService httpService = new HttpService(httpproc, reqistry);

		Thread t = new RequestListenerThread(port, httpService, null);
		t.setDaemon(false);
		t.start();
	}

	private static void configureTinyLog() throws IOException {
		Configurator.currentConfig().level(LoggingLevel.INFO).activate();
		Configurator.currentConfig().formatPattern("{date:yyyy-MM-dd HH:mm:ss:SSS} {level}: {class}.{method}():{line}\t{message}").activate();
	}

	private static void configureTinyLogFolder() throws IOException {

		File logFolderFile = new File(logFolder);
		if (!logFolderFile.exists()) {
			Files.createDirectories(Paths.get(logFolder));
		}

		Logger.info("Logging to folder: " + logFolderFile.getAbsolutePath());

		MultiWriter multiWritter = new MultiWriter();
		multiWritter.addWriter(new RollingFileWriter(logFolder + "/jnapios_log.txt", 365, new TimestampLabeller("yyyy-MM-dd_HH-mm-ss"), new DailyPolicy()));
		multiWritter.addWriter(new ConsoleWriter());

		Configurator.currentConfig().writer(multiWritter).activate();
	}

	private static boolean getPortArg(String[] args) {
		for (String arg : args) {
			if (arg.contains("port=")) {
				port = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
				return true;
			}
		}
		Logger.error("Argument \"port\" not found. Please specify \"port=9001\"");
		return false;
	}

	private static boolean getStatusFileArg(String[] args) {
		for (String arg : args) {
			if (arg.contains("statusFile=")) {
				filePath = arg.substring(arg.indexOf("=") + 1);
				File file = new File(filePath);
				if (file.exists()) {
					return true;
				} else {
					Logger.error("File " + filePath + " not found");
					return false;
				}
			}
		}
		Logger.error("Argument \"statusFile\" not found. Please specify \"statusFile=/path/to/nagios/statusFile\"");
		return false;
	}

	private static boolean getLogFolderArg(String[] args) {
		for (String arg : args) {
			if (arg.contains("logFolder=")) {
				logFolder = arg.substring(arg.indexOf("=") + 1);
				return true;
			}
		}
		Logger.error("Argument \"logFolder\" not found. Please specify \"logFolder=/path/to/logFolder\"");
		return false;
	}

	static class HttpFileHandler implements HttpRequestHandler {

		private final File file;

		public HttpFileHandler(final File file) {
			super();
			this.file = file;
		}

		@Override
		public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {

			long time = System.currentTimeMillis();
			Logger.info("Request recieved. Iniciating...");

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
				Logger.info("Serving file " + file.getPath());
			}

			Logger.info("Request proccessed. Took: " + (System.currentTimeMillis() - time) + " ms");
		}
	}

	static class RequestListenerThread extends Thread {

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

	static class WorkerThread extends Thread {

		private final HttpService httpservice;
		private final HttpServerConnection conn;

		public WorkerThread(final HttpService httpservice, final HttpServerConnection conn) {
			super();
			this.httpservice = httpservice;
			this.conn = conn;
		}

		@Override
		public void run() {
			Logger.info("New connection thread");
			HttpContext context = new BasicHttpContext(null);
			try {
				while (!Thread.interrupted() && this.conn.isOpen()) {
					this.httpservice.handleRequest(this.conn, context);
				}
			} catch (ConnectionClosedException ex) {
				Logger.error("Client closed connection");
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
}