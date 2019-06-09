package ebay.main.java;

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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UrlProcessorTask implements Callable<Map<UrlStatus,Integer>> {	
	private final static Logger logger = Logger.getLogger(UrlProcessorTask.class.getName());
	private Path filePath; 
	HttpClient client = null;

	public UrlProcessorTask(Path path) {
		this.filePath = path;
		client = HttpClient.newHttpClient();
	}

	public Map<UrlStatus, Integer> processUrl() {   		
		Map<UrlStatus,Integer> map = new HashMap<UrlStatus,Integer>();	
		logger.info("Processing file: "+ filePath.getFileName());
		try{				
			List<String> urls = new ArrayList<>();
			long startTime = System.currentTimeMillis();
			try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
				urls = br.lines().collect(Collectors.toList());
			} catch (IOException e) {
				System.out.println("Failed collecting urls");		
			}			
			urls.stream().forEach(url-> {
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
				HttpResponse<String> response = null;
				try {
					response = client.send(request, BodyHandlers.ofString());
				} catch (Exception e) {
					e.printStackTrace();
				} 
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
			});
			long endTime = System.currentTimeMillis();
			logger.info("Processed file" +filePath.getFileName() + " in "+ (TimeUnit.MILLISECONDS.toMinutes((endTime - startTime))) +" minutes.");
			Files.move(filePath, Paths.get("/Users/bhavikajain/eclipse-workspace/FileProcessor/src/resources/output"), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return map;
	}

	@Override
	public Map<UrlStatus, Integer> call() throws Exception {	
		return processUrl();
	}

}
