package main.java.com.ebay.urlProcessor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileProcessor {

	private final static Logger logger = Logger.getLogger(FileProcessor.class.getName());
	Path directoryPath = null;

	public FileProcessor(Path path) {
		this.directoryPath = path;
	}

	public Map<String,Integer> processFiles() throws Exception {
		List<Path> files = getAllFiles(this.directoryPath); 		 		
		return processUrlsInAllFiles(files);		
	}

	/**
	 * Method : getAllFiles()
	 * Creates a list of files to be processed from directory.
	 * @param path : directory path
	 * @return true : if files successfully read, false otherwise.
	 * @throws Exception 
	 */
	public List<Path> getAllFiles(Path path) throws Exception{
		List<Path> files = new ArrayList<Path>();
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs){	
					// skipping empty file
					if(attrs.isRegularFile() && attrs.size() != 0){
						files.add(path);
					}					
					return FileVisitResult.CONTINUE;
				}	
			});
			return files;
		}catch (IOException e) {		
			String message = String.format("Error reading files from directory: %s", this.directoryPath);
			logger.log(Level.SEVERE, message);
			throw new Exception(message); 
		}		
	}

	/**
	 * Method : processUrlsInAllFiles()
	 * @param files : list of files to be processed
	 * @return Map<String, Integer> : returns totalUrlProcessed/failed/passed count
	 * @throws Exception 
	 */
	public Map<String, Integer> processUrlsInAllFiles(List<Path> files) throws Exception {	
		int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(cores);
		List<Callable<Map<UrlStatus,Integer>>> tasks = new ArrayList<Callable<Map<UrlStatus,Integer>>>();
		// create tasks
		for (Path path : files) {
			tasks.add(new UrlProcessorTask(path));
		}
		Map<String,Integer> finalCountMap = new HashMap<String,Integer>();	
		try {
			List<Future<Map<UrlStatus,Integer>>> futures = executorService.invokeAll(tasks);
			for (Future<Map<UrlStatus,Integer>> future : futures) {
				Map<UrlStatus, Integer> entries = future.get();
				for (Map.Entry<UrlStatus , Integer> entry : entries.entrySet()) {
					if (finalCountMap.containsKey(entry.getKey().toString())){
						Integer value = finalCountMap.get(entry.getKey().toString());
						finalCountMap.put(entry.getKey().toString(), value + entry.getValue());
					}
					else {
						finalCountMap.put(entry.getKey().toString(), entry.getValue());
					}
				}
			}
			return finalCountMap;
		} catch (Exception e) {	
			String message = String.format("Error processing urls.Cause: %s", e.getCause());
			logger.log(Level.SEVERE, message);
			throw new Exception(message); 
		} finally {
		   executorService.shutdown();	
		}
	}
}
