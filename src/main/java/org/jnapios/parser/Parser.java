package org.jnapios.parser;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jnapios.data.Server;
import org.jnapios.data.Service;

import com.google.gson.Gson;

public class Parser {

	private static String HOST = "hoststatus";
	private static String SERVICE = "servicestatus";

	private static String SERVICE_DESCRIPTION = "service_description";
	private static String CURRENT_STATE = "current_state";
	private static String HOST_NAME = "host_name";

	public static String parser(File file) throws IOException {

		// File file = new File(filePath);
		String fileContent = readFile(file.getAbsolutePath(), StandardCharsets.UTF_8);

		// Gets all objects (servers or services) from the file into different
		// lists
		List<Map<String, String>> hostList = getObjectWithProperties(fileContent, HOST + " {", "}");
		List<Map<String, String>> serviceList = getObjectWithProperties(fileContent, SERVICE + " {", "}");

		// parses all the list of servers to create a list of Server objects
		List<Server> serverList = createServerList(hostList);

		for (Server server : serverList) {
			// Fills each server with its services
			populateWithServices(server, serviceList);
		}

		Gson gson = new Gson();

		// convert java object to JSON format,
		// and returned as JSON formatted string
		return gson.toJson(serverList);
	}

	private static void populateWithServices(Server server, List<Map<String, String>> serviceList) {

		for (Map<String, String> serviceMap : serviceList) {
			if (serviceMap.get(HOST_NAME).compareTo(server.getName()) == 0) {
				Service service = new Service();
				service.setName(serviceMap.get(SERVICE_DESCRIPTION));
				service.setState(Integer.parseInt(serviceMap.get(CURRENT_STATE)));
				serviceMap.remove(SERVICE_DESCRIPTION);
				serviceMap.remove(CURRENT_STATE);
				service.getProperties().putAll(serviceMap);
				server.getServiceList().add(service);
			}
		}
	}

	private static List<Server> createServerList(List<Map<String, String>> hostList) {
		List<Server> serverList = new ArrayList<>();
		for (Map<String, String> host : hostList) {
			Server server = new Server();
			server.setName(host.get(HOST_NAME));
			server.setState(Integer.parseInt(host.get(CURRENT_STATE)));
			host.remove(HOST_NAME);
			host.remove(CURRENT_STATE);
			server.setProperties(host);
			serverList.add(server);
		}
		return serverList;
	}

	private static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	private static List<Map<String, String>> getObjectWithProperties(String fileContent, String startDelimiter, String endDelimiter) {
		List<Map<String, String>> objectsMap = new ArrayList<>();
		String[] objects = StringUtils.substringsBetween(fileContent, startDelimiter, endDelimiter);
		for (String string : objects) {
			Map<String, String> host = new HashMap<>();
			String lines[] = string.split("\\r?\\n");
			for (String line : lines) {
				if (line.contains("=")) {
					String key = line.substring(0, line.indexOf('='));
					String value = line.substring(line.indexOf('=') + 1);
					host.put(StringUtils.trim(key), value);
				}
			}
			objectsMap.add(host);
		}
		return objectsMap;
	}
}