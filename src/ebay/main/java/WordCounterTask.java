package ebay.main.java;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;


public class WordCounterTask implements Callable<Map<String,Integer>>{

	private Path filePath; 
	
	public WordCounterTask(Path path) {
		this.filePath = path;
	}
	
	/**
	 * counts the number of words in a file
	 * @return
	 */
	public Map<String, Integer> countWords(){   
		Map<String,Integer> map = new HashMap<String,Integer>();	
		try{			
			BufferedReader reader = Files.newBufferedReader(filePath,StandardCharsets.UTF_8);			
			String line = null;			
			while((line = reader.readLine()) != null){				
				String [] words = line.split("\\s+");				
				if(words.length > 0){					
					for(String word : words){							
						if(word.length() > 0){							
							if(map.containsKey(word)){							
								map.put(word, map.get(word)+1);
							}else
								map.put(word, 1);						
						}
					}
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}	
		return map;
	}

	
	@Override
	public Map<String, Integer> call() throws Exception {	
		return countWords();
	}

}
