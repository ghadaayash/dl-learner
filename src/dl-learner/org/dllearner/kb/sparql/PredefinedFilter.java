/**
 * Copyright (C) 2007, Sebastian Hellmann
 *
 * This file is part of DL-Learner.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.dllearner.kb.sparql;

import java.util.HashSet;
import java.util.Set;

public class PredefinedFilter {
  
	    
	public static SparqlQueryType getFilter(int i) {

		switch (i) {
		case 1:
			return YagoFilter();
		case 2: 
			return SKOS();
		}
		return null;
	}
	
	
	public static SparqlQueryType YagoFilter(){
	Set<String> pred = new HashSet<String>();
		pred.add("http://www.w3.org/2004/02/skos/core");
		pred.add("http://www.w3.org/2002/07/owl#sameAs");
		pred.add("http://xmlns.com/foaf/0.1/");
		
		pred.add("http://dbpedia.org/property/reference");
		pred.add("http://dbpedia.org/property/website");
		pred.add("http://dbpedia.org/property/wikipage");
		pred.add("http://dbpedia.org/property/wikiPageUsesTemplate");


		Set<String> obj = new HashSet<String>();
		obj.add("http://dbpedia.org/resource/Category:Wikipedia_");
		obj.add("http://dbpedia.org/resource/Category:Articles_");
		obj.add("http://dbpedia.org/resource/Template");
		obj.add("http://xmlns.com/foaf/0.1/");
		obj.add("http://upload.wikimedia.org/wikipedia/commons");
		obj.add("http://upload.wikimedia.org/wikipedia");
		obj.add("http://www.geonames.org");
		obj.add("http://www.w3.org/2006/03/wn/wn20/instances/synset");
		obj.add("http://www4.wiwiss.fu-berlin.de/flickrwrappr");
		obj.add("http://www.w3.org/2004/02/skos/core");

		return new SparqlQueryType("forbid", obj, pred, "false");
	}
	
	public static SparqlQueryType SKOS(){
			Set<String> pred = new HashSet<String>();
			//pred.add("http://www.w3.org/2004/02/skos/core");
			pred.add("http://www.w3.org/2002/07/owl#sameAs");
			pred.add("http://xmlns.com/foaf/0.1/");
			
			pred.add("http://dbpedia.org/property/reference");
			pred.add("http://dbpedia.org/property/website");
			pred.add("http://dbpedia.org/property/wikipage");
			pred.add("http://www.w3.org/2004/02/skos/core#narrower");
			pred.add("http://dbpedia.org/property/wikiPageUsesTemplate");

			Set<String> obj = new HashSet<String>();
			//obj.add("http://dbpedia.org/resource/Category:Wikipedia_");
			//obj.add("http://dbpedia.org/resource/Category:Articles_");
			obj.add("http://xmlns.com/foaf/0.1/");
			obj.add("http://upload.wikimedia.org/wikipedia/commons");
			obj.add("http://upload.wikimedia.org/wikipedia");
			
			obj.add("http://www.geonames.org");
			obj.add("http://www.w3.org/2006/03/wn/wn20/instances/synset");
			obj.add("http://www4.wiwiss.fu-berlin.de/flickrwrappr");
			
			
			obj.add("http://dbpedia.org/class/yago");
			obj.add("http://dbpedia.org/resource/Template");
			
			
			return new SparqlQueryType("forbid", obj, pred, "false");
		}
	
	
	
}
