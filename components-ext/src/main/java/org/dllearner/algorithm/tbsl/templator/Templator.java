package org.dllearner.algorithm.tbsl.templator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.dllearner.algorithm.tbsl.converter.DRS2SPARQL_Converter;
import org.dllearner.algorithm.tbsl.converter.DUDE2UDRS_Converter;
import org.dllearner.algorithm.tbsl.ltag.parser.LTAGLexicon;
import org.dllearner.algorithm.tbsl.ltag.parser.LTAG_Lexicon_Constructor;
import org.dllearner.algorithm.tbsl.ltag.parser.Parser;
import org.dllearner.algorithm.tbsl.ltag.parser.Preprocessor;
import org.dllearner.algorithm.tbsl.nlp.ApachePartOfSpeechTagger;
import org.dllearner.algorithm.tbsl.nlp.LingPipeLemmatizer;
import org.dllearner.algorithm.tbsl.nlp.PartOfSpeechTagger;
import org.dllearner.algorithm.tbsl.sem.drs.DRS;
import org.dllearner.algorithm.tbsl.sem.drs.UDRS;
import org.dllearner.algorithm.tbsl.sem.dudes.data.Dude;
import org.dllearner.algorithm.tbsl.sem.dudes.reader.ParseException;
import org.dllearner.algorithm.tbsl.sparql.Slot;
import org.dllearner.algorithm.tbsl.sparql.Template;

public class Templator {
	
	String[] GRAMMAR_FILES = {"tbsl/lexicon/english.lex"};
	
	PartOfSpeechTagger tagger;
	LTAGLexicon g;
	LTAG_Lexicon_Constructor LTAG_Constructor = new LTAG_Lexicon_Constructor();
	Parser p;
	Preprocessor pp;
	
	WordNet wordnet;
	LingPipeLemmatizer lem = new LingPipeLemmatizer();
	
    DUDE2UDRS_Converter d2u = new DUDE2UDRS_Converter();
    DRS2SPARQL_Converter d2s = new DRS2SPARQL_Converter();
	
	boolean ONE_SCOPE_ONLY = true;
	boolean UNTAGGED_INPUT = true;
	
	public Templator() {
		
		List<InputStream> grammarFiles = new ArrayList<InputStream>();
		for(int i = 0; i < GRAMMAR_FILES.length; i++){
			grammarFiles.add(this.getClass().getClassLoader().getResourceAsStream(GRAMMAR_FILES[i]));
		}
		
        g = LTAG_Constructor.construct(grammarFiles);

//        tagger = new StanfordPartOfSpeechTagger();
        tagger = new ApachePartOfSpeechTagger();
		
	    p = new Parser();
	    p.SHOW_GRAMMAR = true;
	    p.USE_DPS_AS_INITTREES = true;
	    p.CONSTRUCT_SEMANTICS = true;
	    p.MODE = "LEIPZIG";
	    
	    pp = new Preprocessor(true);
	    
		wordnet = new WordNet();
	}
	
	public void setUNTAGGED_INPUT(boolean b) {
		UNTAGGED_INPUT = b;
	}

	public Set<Template> buildTemplates(String s) {
		
		boolean clearAgain = true;
        
		String tagged;
		if (UNTAGGED_INPUT) {		
			s = pp.normalize(s);
			tagged = tagger.tag(s);
			System.out.println("Tagged input: " + tagged);
		}
		else {
			tagged = s;
		}
		
		String newtagged = pp.condenseNominals(pp.findNEs(tagged,s));
		newtagged = pp.condense(newtagged);
		System.out.println("Preprocessed: " + newtagged); 
        
        p.parse(newtagged,g);
        
        if (p.getDerivationTrees().isEmpty()) {
            p.clear(g,p.getTemps());
            clearAgain = false;
            System.out.println("[Templator.java] '" + s + "' could not be parsed.");
        }
        else {
        try {
        	p.buildDerivedTrees(g);
        } catch (ParseException e) {
            System.err.println("[Templator.java] ParseException at '" + e.getMessage() + "'");
        }
        }

        // build pairs <String,POStag> from tagged
        Hashtable<String,String> postable = new Hashtable<String,String>();
        for (String st : newtagged.split(" ")) {
			postable.put(st.substring(0,st.indexOf("/")),st.substring(st.indexOf("/")+1));;
		}
        //
        
        Set<DRS> drses = new HashSet<DRS>();
        Set<Template> templates = new HashSet<Template>();
        
        for (Dude dude : p.getDudes()) {
            UDRS udrs = d2u.convert(dude);
            if (udrs != null) { 
                
            	for (DRS drs : udrs.initResolve()) {
                	
                	List<Slot> slots = new ArrayList<Slot>();
            		slots.addAll(dude.getSlots());
            		d2s.setSlots(slots);
                	d2s.redundantEqualRenaming(drs);
                	
                	if (!containsModuloRenaming(drses,drs)) {
//                    	// DEBUG
//                		System.out.println(dude);
//                		System.out.println(drs);
//                		for (Slot sl : slots) {
//                			System.out.println(sl.toString());
//                		}
//                		//
                		drses.add(drs);
                		
                		try {
                			Template temp = d2s.convert(drs,slots);
                			
                			// find WordNet synonyms
            				List<String> newwords;
            				String word; 
            				String pos;
                			for (Slot slot : temp.getSlots()) {
                				if (!slot.getWords().isEmpty()) {
                					
                					word = slot.getWords().get(0);
                					pos = postable.get(word.replace(" ","_"));
                					List<String> strings = wordnet.getAttributes(word);
                					
                					newwords = new ArrayList<String>();
                					newwords.add(word);
                					newwords.addAll(strings);
                					if (strings.isEmpty()) {
                						newwords.addAll(wordnet.getBestSynonyms(word,pos));
                					} else {
	                					for (String att : strings) {
	                						newwords.addAll(wordnet.getBestSynonyms(att,pos));
	                					}
                					}
                					if (newwords.isEmpty()) {
                						newwords.add(slot.getWords().get(0));
                					}
                					// stem = lem.stem(slot.getWords().get(0)); newwords.add(stem);
                					slot.setWords(newwords);
                				}
                			}
                			// 
                			
                			templates.add(temp);
                		} catch (java.lang.ClassCastException e) {
                			continue;
                		}
                		if (ONE_SCOPE_ONLY) { break; }
                	}	
                }
            }
        }
 
        if (clearAgain) {
        	p.clear(g,p.getTemps());
        }
        System.gc();
        
        return templates;
    }
	
	private boolean containsModuloRenaming(Set<DRS> drses, DRS drs) {

		for (DRS d : drses) {
			if (d.equalsModuloRenaming(drs)) {
				return true;
			}
		}
		return false;
	}

}
