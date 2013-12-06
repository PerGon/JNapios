package org.jnapios.parser;

import com.google.gson.Gson;
import org.jnapios.data.Server;
import org.jnapios.data.Service;
import org.jnapios.helper.FileHelper;
import org.jnapios.helper.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    private static String HOST = "hoststatus";
    private static String SERVICE = "servicestatus";

    private static String SERVICE_DESCRIPTION = "service_description";
    private static String CURRENT_STATE = "current_state";
    private static String HOST_NAME = "host_name";

    public static String parser(File file) throws IOException {

        // File file = new File(filePath);
        String fileContent = FileHelper.getFileContentAsString(file.getAbsolutePath(), StandardCharsets.UTF_8);

        // Gets all objects (servers or services) from the file into different
        // lists
        List<Map<String, String>> serverAsMapList = getObjectWithProperties(fileContent, HOST + " {", "}");
        List<Map<String, String>> serviceAsMapList = getObjectWithProperties(fileContent, SERVICE + " {", "}");

        // parses all the list of servers to create a list of Server objects
        List<Server> serverList = createServerList(serverAsMapList);

        for (Server server : serverList) {
            // Fills each server with its services
            populateWithServices(server, serviceAsMapList);
        }

        Gson gson = new Gson();

        // convert java object to JSON format,
        // and returned as JSON formatted string
        return gson.toJson(serverList);
    }

    private static List<Server> createServerList(List<Map<String, String>> hostList) {
        List<Server> serverList = new ArrayList<>();
        for (Map<String, String> host : hostList)
            serverList.add(createServer(host));
        return serverList;
    }

    private static void populateWithServices(Server server, List<Map<String, String>> serviceList) {
        for (Map<String, String> serviceMap : serviceList)
            if (serviceMap.get(HOST_NAME).compareTo(server.getName()) == 0)
                server.getServiceList().add(createService(serviceMap));
    }

    private static Server createServer(Map<String, String> host) {
        Server server = new Server();
        server.setName(host.get(HOST_NAME));
        server.setState(Integer.parseInt(host.get(CURRENT_STATE)));
        host.remove(HOST_NAME);
        host.remove(CURRENT_STATE);
        server.setProperties(host);
        return server;
    }
    private static Service createService(Map<String, String> serviceMap) {
        Service service = new Service();
        service.setName(serviceMap.get(SERVICE_DESCRIPTION));
        service.setState(Integer.parseInt(serviceMap.get(CURRENT_STATE)));
        serviceMap.remove(SERVICE_DESCRIPTION);
        serviceMap.remove(CURRENT_STATE);
        service.getProperties().putAll(serviceMap);
        return service;
    }

    private static List<Map<String, String>> getObjectWithProperties(String fileContent, String startDelimiter, String endDelimiter) {
        List<Map<String, String>> objectsMap = new ArrayList<>();
        List<String> objects = StringUtils.substringsBetween(fileContent, startDelimiter, endDelimiter);
        for (String objectString : objects)
            objectsMap.add(getObjectWithProperties(objectString));
        return objectsMap;
    }

    private static Map<String, String> getObjectWithProperties(String string) {
        Map<String, String> objectWithProperties = new HashMap<>();
        String lines[] = string.split("\\r?\\n");
        for (String line : lines)
            checkIfLineIsPropertyAndAddToPropertiesMap(objectWithProperties, line);
        return objectWithProperties;
    }

    private static void checkIfLineIsPropertyAndAddToPropertiesMap(Map<String, String> host, String line) {
        if (line.contains("=")) {
            String key = line.substring(0, line.indexOf('='));
            String value = line.substring(line.indexOf('=') + 1);
            host.put(StringUtils.trim(key), value);
        }
    }
}