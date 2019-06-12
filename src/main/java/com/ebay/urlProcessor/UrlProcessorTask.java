package main.java.com.ebay.urlProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UrlProcessorTask implements Callable<Map<UrlStatus,Integer>> {	
	private final static Logger logger = Logger.getLogger(UrlProcessorTask.class.getName());
	private Path filePath; 
	HttpClient client = null;

	public UrlProcessorTask(Path path) {
		this.filePath = path;
		this.client = HttpClient.newHttpClient();
	}

	@Override
	public Map<UrlStatus, Integer> call() throws Exception {	
		return processUrlsInFile();
	}
	
    /**
     * Reads url from file, sends request to server
     * and get the total passed/failed count
     * @param filePath
     * @return
     */
	private Map<UrlStatus,Integer> getResponseStatusCounts(Path filePath) {
		Map<UrlStatus,Integer> countMap = new HashMap<UrlStatus,Integer>();	
		int noOfUrls = 0;
		try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
			String line = null;
			while((line = br.readLine()) != null) {
				URI	uri = URI.create(line);
				noOfUrls++;
				HttpResponse<String> response = sendRequest(uri);
				updateCountMap(response, countMap);
			}
		} catch (IOException e) {
			String message = String.format("Error reading urls from file: %s", filePath.getFileName());
			logger.log(Level.SEVERE, message);	
		}
		countMap.put(UrlStatus.TOTAL_URLS, noOfUrls);
		return countMap;
	}

	private void updateCountMap(HttpResponse<String> response, Map<UrlStatus,Integer> countMap) {
		int currCount = 0;
		if (response.statusCode() == 200) {
			if (countMap.containsKey(UrlStatus.PASSED)) {
				currCount = countMap.get(UrlStatus.PASSED);
			}
			countMap.put(UrlStatus.PASSED, currCount + 1);
		} else {
			if (countMap.containsKey(UrlStatus.FAILED)) {
				currCount = countMap.get(UrlStatus.FAILED);
			}
			countMap.put(UrlStatus.FAILED, currCount + 1);
		}
	}

	/**
	 * Send request to server and returns the response
	 * @param requesturi
	 * @return httpresponse
	 */
	private HttpResponse<String> sendRequest(URI requesturi) {
		HttpRequest request = HttpRequest.newBuilder().uri(requesturi).build();
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (Exception e) {
			String message = String.format("Sending request to url failed: %s. Cause: %s",  requesturi, e.getCause());
			logger.log(Level.SEVERE, message);	
		}
		return response;
	}

	/**
	 * processes urls in a file and collects the count of failed/success urls
	 * @return
	 */
	public Map<UrlStatus, Integer> processUrlsInFile() { 
		logger.info("Processing file: "+ "'"+filePath.getFileName()+"'");
		Map<UrlStatus,Integer> map = null;
		try{				
			long startTime = System.currentTimeMillis();	
			map = getResponseStatusCounts(filePath);
			long endTime = System.currentTimeMillis();
			logger.info("Processed file: " + "'"+filePath.getFileName() +"'"+ " in "+ (TimeUnit.MILLISECONDS.toMinutes((endTime - startTime))) +" minutes.");
		} catch (Exception e) {
			String message = String.format("Processing urls from file failed: %s. Cause: %s", filePath.getFileName());
			logger.log(Level.SEVERE, message);			
		}	
		return map;
	}
}
