package ebay.main.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MainDriver {
	
	private final static Logger logger = Logger.getLogger(MainDriver.class.getName());
	public static final String MESSAGE_INVALID_PATH = "Path given does not exists";

	public static void main(String[] args) {	
		// get all the paths to traverse
		List<Path> paths = new ArrayList<Path>();
		boolean allPathsCorrect = true;
		for(int i = 0; i < args.length; i++){
			Path path = Paths.get(args[i]);
			if(!Files.exists(path)){
				allPathsCorrect = false;
				break;
			}else
				paths.add(path);
		}
		// if some paths do not exists, quit
		if(!allPathsCorrect){
			System.out.println(MESSAGE_INVALID_PATH);
			System.exit(-1);
		}
		if(paths.size() > 0) {
			int cores = Runtime.getRuntime().availableProcessors();
			long startTime = System.currentTimeMillis();
			FileProcessor fileProcessor = new FileProcessor(paths, cores);
			Map<String, Integer> result = null;
			try {
				result = fileProcessor.processAllPaths();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			logger.info("Time taken to complete: "+ TimeUnit.MILLISECONDS.toMinutes((endTime - startTime)));
			System.out.println(result);
		}
	}

}
