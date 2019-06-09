package ebay.main.java;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileProcessor {
	List<Path> files = new ArrayList<Path>();
	List<Path> filePaths = null;
	int noOfWorkers = 0;

	public FileProcessor(List<Path> allPaths, int noOfThreads) {
		this.filePaths = allPaths;
		this.noOfWorkers = noOfThreads;	
	}

	public Map<String,Integer> processAllPaths() throws Exception{		
		for(Path path: filePaths){		
			boolean success = getAllFiles(path); 		  
			if(!success){			
				throw new Exception("Error creating file list for path: "+ path); 
			}			
		}	
		return processFiles();		
	}

	/**
	 * Creates a list of files to be processed from all directories/path
	 * @param path
	 * @return
	 */
	public boolean getAllFiles(Path path){
		boolean success = true;		
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

		}catch (IOException e) {		
			System.out.println(e.getMessage());
			e.printStackTrace();
			success = false;
		}		
		return success;		
	}

	/**
	 *  Creates threads for processing all files and returns the count of top N words
	 * @return
	 */
	public Map<String, Integer> processFiles() {	
		ExecutorService executorService = Executors.newFixedThreadPool(noOfWorkers);
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
					if (finalCountMap.containsKey(entry.getKey())){
						Integer value = finalCountMap.get(entry.getKey());
						finalCountMap.put(entry.getKey().toString(), value + entry.getValue());
					}
					else {
						finalCountMap.put(entry.getKey().toString(), entry.getValue());
					}
				}
			}			
			executorService.shutdown();	
		} catch(InterruptedException e) {		
			e.printStackTrace();		
		} catch (ExecutionException e) {			
			e.printStackTrace();
		}
		return finalCountMap;
	}
}
