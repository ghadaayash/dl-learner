package org.dllearner.algorithm.tbsl.exploration.modules;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.dllearner.algorithm.tbsl.exploration.Sparql.Hypothesis;
import org.dllearner.algorithm.tbsl.exploration.Utils.Levenshtein;

public class LevenstheinModule {
	private final static double LevenstheinMin=0.92;
	
	public static ArrayList<Hypothesis> doLevensthein(String variable, String property_to_compare_with, HashMap<String, String> properties)
			throws SQLException {
		ArrayList<Hypothesis> listOfNewHypothesen= new ArrayList<Hypothesis>();

		
		 //iterate over properties
		 for (Entry<String, String> entry : properties.entrySet()) {
			 String key = entry.getKey();
			 key=key.replace("\"","");
			 key=key.replace("@en","");
			 String value = entry.getValue();
			 
			 //compare property gotten from the resource with the property from the original query
			 double nld=Levenshtein.nld(property_to_compare_with.toLowerCase(), key);
			 
			 //if(nld>=LevenstheinMin||key.contains(lemmatiser.stem(property_to_compare_with))||property_to_compare_with.contains(lemmatiser.stem(key))){

		     if(nld>=LevenstheinMin){
				 Hypothesis h = new Hypothesis(variable, key, value, "PROPERTY", nld);
				 listOfNewHypothesen.add(h);
			 }
		     
		 }
		 
		 return listOfNewHypothesen;
	}
	 
}
