package main.java.com.ebay.urlProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Driver {

	private final static Logger logger = Logger.getLogger(Driver.class.getName());
	public static final String MESSAGE_INVALID_FILE_PATH = "Directory Path does not exists";
	public static final String MESSAGE_INVALID_ARGUMENTS = "Invalid arguments, expects atleast one arguments: Path of file.";

	private static boolean validPath (Path path) {
		if (!Files.exists(path))
			return false;
		return true;	
	}

	private static void printResultInfo (Map<String, Integer> map) {
		if (map == null || map.size() == 0)
			return;
		int totalUrlCount = map.getOrDefault(UrlStatus.TOTAL_URLS.toString(), 0);
		int passedUrlsCount = map.getOrDefault(UrlStatus.PASSED.toString(), 0);
		int failedUrlsCount = map.getOrDefault(UrlStatus.FAILED.toString(), 0);	
		logger.info("Total number of Urls processed: " +totalUrlCount);
		logger.info("Total number of Urls passed: " +passedUrlsCount);
		logger.info("Total number of Urls failed: "+failedUrlsCount);
	}

	public static void main (String[] args) throws Exception {	
		if (args == null || args.length <= 0) 
			throw new IllegalArgumentException(MESSAGE_INVALID_ARGUMENTS);
		Path directoryPath = Paths.get(args[0]);	   		
		if (!validPath(directoryPath))
			throw new IllegalArgumentException(MESSAGE_INVALID_FILE_PATH);			
		FileProcessor fileProcessor = new FileProcessor(directoryPath);
	    logger.log(Level.FINE, "Processing files from path:", directoryPath);
		Map<String, Integer> resultMap = null;
		try {
			long startTime = System.currentTimeMillis();	
			resultMap = fileProcessor.processFiles();
			long endTime = System.currentTimeMillis();
			logger.info("Time taken to complete all files: "+ TimeUnit.MILLISECONDS.toSeconds((endTime - startTime)) + " seconds.");
			printResultInfo(resultMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE,"Error in processing files. Cause:", e.getCause());
			throw new Exception(e);
		}
	}

}
