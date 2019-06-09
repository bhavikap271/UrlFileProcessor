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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	 * Method: getUrls
	 * reads all the urls from file
	 * @param filePath
	 * @return list of urls fetched from file
	 */
	private List<URI> getUrls(Path filePath) {
		List<URI> urls = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
			String line = null;
			while((line = br.readLine()) != null) {
				URI	uri = URI.create(line);
				urls.add(uri);
			}
		} catch (IOException e) {
			String message = String.format("Error reading urls from file: %s", filePath.getFileName());
			logger.log(Level.SEVERE, message);	
		} 
		return urls;
	}

    /**
     * Counts the number of passed/failed urls
     * @param responses
     * @return
     */
	private Map<UrlStatus,Integer> getCounts(List<HttpResponse<String>> responses) {
		Map<UrlStatus,Integer> map = new HashMap<UrlStatus,Integer>();	
		for (HttpResponse<String> response : responses) {
			int currCount = 0;
			if (response.statusCode() == 200) {
				if (map.containsKey(UrlStatus.PASSED)) {
					currCount = map.get(UrlStatus.PASSED);
				}
				map.put(UrlStatus.PASSED, currCount + 1);
			} else {
				if (map.containsKey(UrlStatus.FAILED)) {
					currCount = map.get(UrlStatus.FAILED);
				}
				map.put(UrlStatus.FAILED, currCount + 1);
			}
		}
		return map;
	}

	/**
	 * Send request to server for each url and collects the response
	 * @param urls : list of responses
	 * @return
	 */
	private List<HttpResponse<String>> sendRequests(List<URI> urls) {
		List<HttpResponse<String>> responses = new ArrayList<>();
		urls.stream().forEach(url-> {
			HttpRequest request = HttpRequest.newBuilder().uri(url).build();
			try {
				responses.add(client.send(request, BodyHandlers.ofString()));
			} catch (Exception e) {
				String message = String.format("Sending request to url failed: %s. Cause: %s",  url, e.getCause());
				logger.log(Level.SEVERE, message);	
			} 
		});		
		return responses;
	}

	/**
	 * processes urls in a file and collects the response status
	 * @return
	 */
	public Map<UrlStatus, Integer> processUrlsInFile() { 
		logger.info("Processing file: "+ filePath.getFileName());
		Map<UrlStatus,Integer> map = null;
		try{				
			List<URI> urls = getUrls(filePath);
			long startTime = System.currentTimeMillis();	
			List<HttpResponse<String>> responses = sendRequests(urls);
			map = getCounts(responses);
			map.put(UrlStatus.TOTAL_URLS, urls.size());
			long endTime = System.currentTimeMillis();
			logger.info("Processed file: " + filePath.getFileName() + " in "+ (TimeUnit.MILLISECONDS.toMinutes((endTime - startTime))) +" minutes.");
		} catch (Exception e) {
			String message = String.format("Processing urls from file failed: %s. Cause: %s", filePath.getFileName());
			logger.log(Level.SEVERE, message);			
		}	
		return map;
	}

}
