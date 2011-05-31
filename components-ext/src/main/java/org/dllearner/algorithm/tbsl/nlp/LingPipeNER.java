package org.dllearner.algorithm.tbsl.nlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.Dictionary;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.util.AbstractExternalizable;

public class LingPipeNER implements NER{
	
	private static Logger logger = Logger.getLogger(LingPipeNER.class);
	
	private static final String DICTIONARY_PATH = "tbsl/models/dbpedia_lingpipe.dictionary";
	
	private Chunker ner;
	
	public LingPipeNER() {
		this(true, false);
	}
	
	public LingPipeNER(boolean caseSensitive) {
		this(caseSensitive, true);
	}
	
	public LingPipeNER(boolean caseSensitive, boolean allMatches) {
		try {
			long startTime = System.currentTimeMillis();
			logger.info("Initializing LingPipe NER...");
			String path = this.getClass().getClassLoader().getResource(DICTIONARY_PATH).getPath();
			Dictionary<String> dictionary = (Dictionary<String>) AbstractExternalizable.readObject(new File(path));
			ner = new ExactDictionaryChunker(dictionary, IndoEuropeanTokenizerFactory.INSTANCE, allMatches, caseSensitive);
			logger.info("Done in " + (System.currentTimeMillis()-startTime) + "ms.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<String> getNamedEntitites(String sentence) {
		List<String> namedEntities = new ArrayList<String>();
		Chunking chunking = ner.chunk(sentence);
		for(Chunk chunk : chunking.chunkSet()){
			namedEntities.add(sentence.substring(chunk.start(), chunk.end()));
		}
		return namedEntities;
	}

}
