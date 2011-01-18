/**
 * Copyright (C) 2007-2010, Jens Lehmann
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
package org.dllearner.sparqlquerygenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.dllearner.sparqlquerygenerator.datastructures.QueryTree;
import org.dllearner.sparqlquerygenerator.datastructures.impl.QueryTreeImpl;
import org.dllearner.sparqlquerygenerator.examples.DBpediaExample;
import org.dllearner.sparqlquerygenerator.examples.LinkedGeoDataExample;
import org.dllearner.sparqlquerygenerator.impl.QueryTreeFactoryImpl;
import org.dllearner.sparqlquerygenerator.operations.lgg.LGGGenerator;
import org.dllearner.sparqlquerygenerator.operations.lgg.LGGGeneratorImpl;
import org.dllearner.sparqlquerygenerator.util.ModelGenerator;
import org.dllearner.sparqlquerygenerator.util.ModelGenerator.Strategy;
import org.junit.Assert;
import org.junit.Test;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * 
 * @author Lorenz Bühmann
 *
 */
public class LGGTest {
	
	private static final Logger logger = Logger.getLogger(LGGTest.class);
	
	@Test
	public void testLGGWithDBpediaExample(){
		QueryTreeFactory<String> factory = new QueryTreeFactoryImpl();
		
		List<QueryTree<String>> posExampleTrees = DBpediaExample.getPosExampleTrees();
		
		int cnt = 1;
		for(QueryTree<String> tree : posExampleTrees){
			System.out.println("TREE " + cnt);
			tree.dump();
			System.out.println("-----------------------------");
			cnt++;
		}
		
		LGGGenerator<String> lggGenerator = new LGGGeneratorImpl<String>();
		QueryTree<String> lgg = lggGenerator.getLGG(posExampleTrees);
		
		System.out.println("LGG");
		lgg.dump();
		
		QueryTreeImpl<String> tree = factory.getQueryTree("?");
		QueryTreeImpl<String> subTree1 = new QueryTreeImpl<String>("?");
		subTree1.addChild(new QueryTreeImpl<String>("?"), "leaderParty");
		subTree1.addChild(new QueryTreeImpl<String>("?"), "population");
		subTree1.addChild(new QueryTreeImpl<String>("Germany"), "locatedIn");
		tree.addChild(subTree1, "birthPlace");
		tree.addChild(new QueryTreeImpl<String>("?"), RDFS.label.toString());
		QueryTreeImpl<String> subTree2 = new QueryTreeImpl<String>("Person");
		subTree2.addChild(new QueryTreeImpl<String>(OWL.Thing.toString()), RDFS.subClassOf.toString());
		tree.addChild(subTree2, RDF.type.toString());
		QueryTreeImpl<String> subTree3 = new QueryTreeImpl<String>("?");
		QueryTreeImpl<String> subSubTree = new QueryTreeImpl<String>("Person");
		subSubTree.addChild(new QueryTreeImpl<String>(OWL.Thing.toString()), RDFS.subClassOf.toString());
		subTree3.addChild(subSubTree, RDFS.subClassOf.toString());
		tree.addChild(subTree3, RDF.type.toString());
		
		Assert.assertTrue(lgg.isSameTreeAs(tree));
		
		System.out.println(tree.toSPARQLQueryString());
		
	}
	
	@Test
	public void testLGGWithLinkedGeoDataExample(){
		QueryTreeFactory<String> factory = new QueryTreeFactoryImpl();
		
		List<QueryTree<String>> posExampleTrees = LinkedGeoDataExample.getPosExampleTrees();
		
		int cnt = 1;
		for(QueryTree<String> tree : posExampleTrees){
			System.out.println("TREE " + cnt);
			tree.dump();
			System.out.println("-----------------------------");
			cnt++;
		}
		
		LGGGenerator<String> lggGenerator = new LGGGeneratorImpl<String>();
		QueryTree<String> lgg = lggGenerator.getLGG(posExampleTrees);
		
		System.out.println("LGG");
		lgg.dump();
		
		QueryTreeImpl<String> tree = factory.getQueryTree("?");
		QueryTreeImpl<String> subTree = new QueryTreeImpl<String>("lgdo:Aerodome");
		subTree.addChild(new QueryTreeImpl<String>("lgdo:Aeroway"), RDFS.subClassOf.toString());
		tree.addChild(subTree, RDF.type.toString());
		tree.addChild(new QueryTreeImpl<String>("?"), RDFS.label.toString());
		tree.addChild(new QueryTreeImpl<String>("?"), "geo:long");
		tree.addChild(new QueryTreeImpl<String>("?"), "geo:lat");
		tree.addChild(new QueryTreeImpl<String>("?"), "georss:point");
		tree.addChild(new QueryTreeImpl<String>("?"), "lgdp:icao");
		
		Assert.assertTrue(lgg.isSameTreeAs(tree));
		
		System.out.println(tree.toSPARQLQueryString());
		
	}
	
//	@Test
	public void performanceTest(){
		int recursionDepth = 3;
		int limit = 100;
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpediaLiveAKSW();
		ExtractionDBCache cache = new ExtractionDBCache("cache");
		Set<String> predicateFilters = new HashSet<String>();
		predicateFilters.add("http://dbpedia.org/ontology/wikiPageWikiLink");
		predicateFilters.add("http://dbpedia.org/property/wikiPageUsesTemplate");
		ModelGenerator modelGen = new ModelGenerator(endpoint, predicateFilters, cache);
		QueryTreeFactory<String> treeFactory = new QueryTreeFactoryImpl();
		
		String queryString = "SELECT ?resource WHERE {?resource a ?class." +
				" FILTER(REGEX(?resource,'http://dbpedia.org/resource'))} LIMIT " + limit;
		SparqlQuery query = new SparqlQuery(queryString, endpoint);
		ResultSet rs = query.send();
		
		
		
		//load the models
		SortedMap<String, Model> resource2Model = new TreeMap<String, Model>();
		Model model;
		String resource;
		logger.info("Resources(#triple):");
		while(rs.hasNext()){
			try {
				resource = rs.next().get("resource").asResource().getURI();
				model = modelGen.createModel(resource, Strategy.CHUNKS, recursionDepth);
				logger.info(resource + "(" + model.size() + ")");
				resource2Model.put(resource, model);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		model = modelGen.createModel("http://dbpedia.org/resource/A_Farewell_to_Arms", Strategy.CHUNKS, recursionDepth);
//		logger.info("http://dbpedia.org/resource/A_Farewell_to_Arms" + "(" + model.size() + ")");
//		resource2Model.put("http://dbpedia.org/resource/A_Farewell_to_Arms", model);
//		model = modelGen.createModel("http://dbpedia.org/resource/Darjeeling", Strategy.CHUNKS, recursionDepth);
//		logger.info("http://dbpedia.org/resource/Darjeeling" + "(" + model.size() + ")");
//		resource2Model.put("http://dbpedia.org/resource/Darjeeling", model);
			
			
		//create the querytrees
		SortedMap<String, QueryTree<String>> resource2Tree = new TreeMap<String, QueryTree<String>>();
		Map<QueryTree<String>, String> tree2Resource = new HashMap<QueryTree<String>, String>(limit);
		List<QueryTree<String>> trees = new ArrayList<QueryTree<String>>();
		QueryTree<String> tree;
		for(Entry<String, Model> entry : resource2Model.entrySet()){
			try {
				tree = treeFactory.getQueryTree(entry.getKey(), entry.getValue());
				trees.add(tree);
				resource2Tree.put(entry.getKey(), tree);
				tree2Resource.put(tree, entry.getKey());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		LGGGenerator<String> lggGen = new LGGGeneratorImpl<String>();
		QueryTree<String> tree1;
		QueryTree<String> tree2;
		QueryTree<String> lgg;
		for(int i = 0; i < trees.size(); i++){
			for(int j = i+1; j < trees.size(); j++){
				try {
					tree1 = trees.get(i);
					tree2 = trees.get(j);
					lgg = lggGen.getLGG(tree1, tree2);
					logger.info("LGG(" + tree2Resource.get(tree1) + ", " + tree2Resource.get(tree2) + ") needed "
							+ MonitorFactory.getTimeMonitor("LGG").getLastValue() + "ms");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				logger.info("Tree 1:\n" + tree1.getStringRepresentation());
//				logger.info("Tree 2:\n" + tree2.getStringRepresentation());
//				logger.info("LGG:\n" + lgg.getStringRepresentation());
			}
		}
		logger.info("Average time to compute LGG: " + MonitorFactory.getTimeMonitor("LGG").getAvg());
		logger.info("Min time to compute LGG: " + MonitorFactory.getTimeMonitor("LGG").getMin());
		logger.info("Max time to compute LGG: " + MonitorFactory.getTimeMonitor("LGG").getMax());
		logger.info("#computed LGGs: " + MonitorFactory.getTimeMonitor("LGG").getHits());
		
		
	}

}
