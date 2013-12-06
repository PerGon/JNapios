package org.jnapios;

import org.apache.http.protocol.*;
import org.jnapios.http.HttpFileHandler;
import org.jnapios.http.RequestListenerThread;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.labellers.TimestampLabeller;
import org.pmw.tinylog.policies.DailyPolicy;
import org.pmw.tinylog.writers.RollingFileWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ApplicationBootstrap {

    private static int port = 0;
    private static String filePath = null;
    private static String logFolder = null;

    public static void main(String[] args) throws Exception {

        initialLogConfiguration();

        boolean abortInitialization = false;
        buildArguments(args);

        if (logFolder == null) {
            Logger.error("Argument \"logFolder\" not found. Please specify \"logFolder=/path/to/logFolder\"");
            abortInitialization = true;
        }

        if (filePath == null) {
            Logger.error("Argument \"statusFile\" not found. Please specify \"statusFile=/path/to/nagios/statusFile.dat\"");
            abortInitialization = true;
        }

        if (port == 0) {
            Logger.error("Argument \"port\" not found. Please specify \"port=9001\"");
            abortInitialization = true;
        }

        if (abortInitialization) {
            Logger.error("Not all mandatory args where specified or errors in the arguments.");
            Logger.error("Provide \"port\", \"logFolder\"  and \"statusFile\" arguments ");
            Logger.error("Aborting...");
            return;
        }

        additionalLogConfigurations();

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

    private static void initialLogConfiguration() throws IOException {
        Configurator.currentConfig().level(LoggingLevel.INFO).activate();
        Configurator.currentConfig().formatPattern("{date:yyyy-MM-dd HH:mm:ss:SSS} {level}: {class}.{method}():{line}\t{message}").activate();
    }

    private static void additionalLogConfigurations() throws IOException {
        File logFolderFile = new File(logFolder);
        if (!logFolderFile.exists()) {
            Files.createDirectories(Paths.get(logFolder));
        }
        Logger.info("Logging to folder: " + logFolderFile.getAbsolutePath());
        Configurator.currentConfig().writer(new RollingFileWriter(logFolder + "/jnapios_log.txt", 365, new TimestampLabeller("yyyy-MM-dd_HH-mm-ss"), new DailyPolicy())).activate();
    }

    private static void buildArguments(String[] args) {
        for (String arg : args) {
            getLogFolderArg(arg);
            getStatusFileArg(arg);
            getPortArg(arg);
        }
    }

    private static void getLogFolderArg(String arg) {
        if (arg.contains("logFolder="))
            logFolder = arg.substring(arg.indexOf("=") + 1);
    }

    private static void getPortArg(String arg) {
        if (arg.contains("port=")) {
            port = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
        }
    }

    private static void getStatusFileArg(String arg) {
        if (arg.contains("statusFile=")) {
            filePath = arg.substring(arg.indexOf("=") + 1);
            File file = new File(filePath);
            if (!file.exists()) {
                Logger.error("File " + filePath + " not found");
                filePath = null;
            }
        }
    }
}
