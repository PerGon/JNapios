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

    private static final String URL_REQUEST_STATE = "/jnapios/state";

    private static final String PARAMETER_LOG_FOLDER = "logFolder=";
    private static final String PARAMETER_PORT = "port=";
    private static final String PARAMETER_STATUS_FILE = "statusFile=";

    private static final String LOGGER_MESSAGE_FORMAT = "{date:yyyy-MM-dd HH:mm:ss:SSS} {level}: {class}.{method}():{line}\t{message}";
    private static final String LOG_FILE = "/jnapios_log.txt";
    private static final String LOG_TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    private static final String INFO_START_APP = "###Starting application JNAPIOS.###";
    private static final String INFO_LOG_FOLDER = "Logging to folder: {0}";

    private static final String ERROR_ABORT_INITIALIZATION = "Not all mandatory args where specified or errors in the arguments. \n Provide \"port\", \"logFolder\"  and \"statusFile\" arguments \n Aborting...";
    private static final String ERROR_PORT_ARG_MANDATORY = "Argument \"port\" not found. Please specify \"port=9001\"";
    private static final String ERROR_STATUS_FILE_ARG_MANDATORY = "Argument \"statusFile\" not found. Please specify \"statusFile=/path/to/nagios/statusFile.dat\"";
    private static final String ERROR_LOG_FOLDER_ARG_MANDATORY = "Argument \"logFolder\" not found. Please specify \"logFolder=/path/to/logFolder\"";
    private static final String ERROR_FILE_NOT_FOUND = "File {0} not found";

    private static final String STRING_EQUALS = "=";

    private static int port = 0;
    private static String filePath = null;
    private static String logFolder = null;

    public static void main(String[] args) throws Exception {

        initialLogConfiguration();

        boolean abortInitialization = false;
        buildArguments(args);

        if (logFolder == null) {
            Logger.error(ERROR_LOG_FOLDER_ARG_MANDATORY);
            abortInitialization = true;
        }

        if (filePath == null) {
            Logger.error(ERROR_STATUS_FILE_ARG_MANDATORY);
            abortInitialization = true;
        }

        if (port == 0) {
            Logger.error(ERROR_PORT_ARG_MANDATORY);
            abortInitialization = true;
        }

        if (abortInitialization) {
            Logger.error(ERROR_ABORT_INITIALIZATION);
            return;
        }

        additionalLogConfigurations();

        Logger.info(INFO_START_APP);

        // Set up the HTTP protocol processor
        HttpProcessor httpproc = HttpProcessorBuilder.create().add(new ResponseDate()).add(new ResponseServer("Test/1.1")).add(new ResponseContent()).add(new ResponseConnControl()).build();

        // Set up request handlers
        UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
        reqistry.register(URL_REQUEST_STATE, new HttpFileHandler(new File(filePath)));

        // Set up the HTTP service
        HttpService httpService = new HttpService(httpproc, reqistry);

        Thread t = new RequestListenerThread(port, httpService, null);
        t.setDaemon(false);
        t.start();
    }

    private static void initialLogConfiguration() throws IOException {
        Configurator.currentConfig().level(LoggingLevel.INFO).activate();
        Configurator.currentConfig().formatPattern(LOGGER_MESSAGE_FORMAT).activate();
    }

    private static void additionalLogConfigurations() throws IOException {
        File logFolderFile = new File(logFolder);
        if (!logFolderFile.exists()) {
            Files.createDirectories(Paths.get(logFolder));
        }
        Logger.info(INFO_LOG_FOLDER, logFolderFile.getAbsolutePath());
        Configurator.currentConfig().writer(new RollingFileWriter(logFolder + LOG_FILE, 365, new TimestampLabeller(LOG_TIME_FORMAT), new DailyPolicy())).activate();
    }

    private static void buildArguments(String[] args) {
        for (String arg : args) {
            getLogFolderArg(arg);
            getStatusFileArg(arg);
            getPortArg(arg);
        }
    }

    private static void getLogFolderArg(String arg) {
        if (arg.contains(PARAMETER_LOG_FOLDER))
            logFolder = arg.substring(arg.indexOf(STRING_EQUALS) + 1);
    }

    private static void getPortArg(String arg) {
        if (arg.contains(PARAMETER_PORT)) {
            port = Integer.parseInt(arg.substring(arg.indexOf(STRING_EQUALS) + 1));
        }
    }

    private static void getStatusFileArg(String arg) {
        if (arg.contains(PARAMETER_STATUS_FILE)) {
            filePath = arg.substring(arg.indexOf(STRING_EQUALS) + 1);
            File file = new File(filePath);
            if (!file.exists()) {
                Logger.error(ERROR_FILE_NOT_FOUND, filePath);
                filePath = null;
            }
        }
    }
}
