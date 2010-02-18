/**
 * Copyright (C) 2007-2008, Jens Lehmann
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.dllearner.algorithms.gp.ADC;
import org.dllearner.core.ComponentManager;
import org.dllearner.core.owl.DatatypeExactCardinalityRestriction;
import org.dllearner.core.owl.DatatypeMaxCardinalityRestriction;
import org.dllearner.core.owl.DatatypeMinCardinalityRestriction;
import org.dllearner.core.owl.DatatypeProperty;
import org.dllearner.core.owl.DatatypeSomeRestriction;
import org.dllearner.core.owl.DatatypeValueRestriction;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.DescriptionVisitor;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.Intersection;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.Negation;
import org.dllearner.core.owl.Nothing;
import org.dllearner.core.owl.ObjectAllRestriction;
import org.dllearner.core.owl.ObjectExactCardinalityRestriction;
import org.dllearner.core.owl.ObjectMaxCardinalityRestriction;
import org.dllearner.core.owl.ObjectMinCardinalityRestriction;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.ObjectSomeRestriction;
import org.dllearner.core.owl.ObjectValueRestriction;
import org.dllearner.core.owl.StringValueRestriction;
import org.dllearner.core.owl.Thing;
import org.dllearner.core.owl.Union;
import org.dllearner.parser.KBParser;
import org.dllearner.parser.ParseException;

/**
 * Converter from DL-Learner descriptions to a corresponding SPARQL query to get
 * all instances that are described by this description.
 * 
 * @author Sebastian Knappe
 * @author Sebastian Hellmann
 * 
 */
public class SparqlQueryDescriptionConvertVisitor implements DescriptionVisitor {

	
	private static Logger logger = Logger.getLogger(ComponentManager.class);

	private int limit = 5;
	private boolean labels = false;
	private boolean distinct = false;
	private SortedSet<String> transitiveProperties =null;
	public void setTransitiveProperties(SortedSet<String> transitiveProperties) {
		this.transitiveProperties = transitiveProperties;
	}

	private Map<String,String> classToSubclassesVirtuoso = null;
	
	private Stack<String> stack = new Stack<String>();
	private String query = "";
	private int currentObject = 0;
	private List<String> foundNamedClasses = new ArrayList<String>();
	
	public void reset(){
		currentObject = 0;
		stack = new Stack<String>();
		stack.push("subject");
		query = "";
		foundNamedClasses =  new ArrayList<String>() ;
	}
	
	public SparqlQueryDescriptionConvertVisitor() {
		stack.push("subject");
	}

	public String getSparqlQuery( String descriptionKBSyntax) throws ParseException { 
		Description description = KBParser.parseConcept(descriptionKBSyntax);
		return getSparqlQuery( description);
	}
	
	public String getSparqlQuery( Description description) { 
		description.accept(this);
		expandSubclasses();
		String ret =  "SELECT "+distinct()+"?subject "+((labels)?"?label":"")+" { "+labels()+ query + " \n } " + limit();
		reset();
		return ret;
	}
	
	private void expandSubclasses(){
		if(classToSubclassesVirtuoso == null){
			return;
		}
		int counter = 0;
		int index = 0;
		String filter = "";
		String var = "";
		String uri = "";
		StringBuffer tmp ;
		for(String nc: foundNamedClasses){
			index = query.indexOf("<"+nc+">");
			filter = classToSubclassesVirtuoso.get(nc);
			if(index == -1){
				logger.warn("named class found before, but not in query?? "+nc);
			}else if(filter != null){
				var = "?expanded"+counter;
				uri = "<"+nc+">";
				tmp = new StringBuffer();
				tmp.append(query.substring(0, index));
				tmp.append(var);
				tmp.append(query.substring(index+(uri.length())));
				tmp.append("\nFILTER ( " +var+ " in (" +filter+	") ). ");
				query = tmp.toString();
//				= query.substring(0, index)+var+query.substring(index+(uri.length()));
				
//				query += "\nFILTER (?expanded" +counter+
//						" in (" +filter+
//						") ). ";
			}else{
				logger.debug("no mapping found ("+nc+")  "+this.getClass().getSimpleName());
			}
			counter++;
		}
	}
	
	private String limit() {
		return (limit > 0) ? " LIMIT " + limit + " " : "";
	}
	private String labels() {
		return (labels)?"\n?subject rdfs:label ?label . ":"";
	}
	private String distinct() {
		return (distinct)?"DISTINCT ":"";
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public void noLimit() {
		this.limit = -1;
	}

	public void setLabels(boolean labels) {
		this.labels = labels;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}
	
	public void setClassToSubclassesVirtuoso(Map<String,String> classToSubclassesVirtuoso) {
		this.classToSubclassesVirtuoso = classToSubclassesVirtuoso;
	}

	public static String getSparqlQuery(String descriptionKBSyntax, int limit, boolean labels, boolean distinct) throws ParseException {
		Description d = KBParser.parseConcept(descriptionKBSyntax);
		return getSparqlQuery(d, limit, labels, distinct);
	}
	
	public static String getSparqlQuery(Description description, int limit, boolean labels, boolean distinct) {
		SparqlQueryDescriptionConvertVisitor visitor = new SparqlQueryDescriptionConvertVisitor();
		visitor.setDistinct(distinct);
		visitor.setLabels(labels);
		visitor.setLimit(limit);
		return visitor.getSparqlQuery(description);
	}

	/**
	 * COMMENT: write some more includes subclasses, costly function, because
	 * subclasses have to be received first. TODO mentioned method cannot be
	 * found by Javadoc tool conceptRewrite(String descriptionKBSyntax,
	 * SparqlEndpoint se, Cache c, boolean simple)
	 * 
	 * @param descriptionKBSyntax
	 *            @see #getSparqlQuery(Description description, int limit)
	 * @param resultLimit
	 *            @see #getSparqlQuery(Description description, int limit)
	 * @param maxDepth
	 * @throws ParseException
	 */
	public static String getSparqlQueryIncludingSubclasses(String descriptionKBSyntax, int resultLimit,
			SPARQLTasks st, int maxDepth) throws ParseException {
		String rewritten = SparqlQueryDescriptionConvertRDFS
				.conceptRewrite(descriptionKBSyntax, st, maxDepth);

		return getSparqlQuery(rewritten, resultLimit, false, false);

	}

	/**
	 * Used for testing the Sparql Query converter.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SortedSet<String> s = new TreeSet<String>();
			HashMap<String, String> result = new HashMap<String, String>();
			HashMap<String, String> subclassMap = new HashMap<String, String>();
			subclassMap.put("http://nlp2rdf.org/ontology/Sentence","<http://nlp2rdf.org/ontology/Subsentence>");
			String conj = "(\"http://dbpedia.org/class/yago/Person100007846\" AND \"http://dbpedia.org/class/yago/Head110162991\")";

			s.add("EXISTS \"http://dbpedia.org/property/disambiguates\".TOP");
			s.add("EXISTS \"http://dbpedia.org/property/successor\".\"http://dbpedia.org/class/yago/Person100007846\"");
			s.add("EXISTS \"http://dbpedia.org/property/successor\"." + conj);
			s.add("ALL \"http://dbpedia.org/property/disambiguates\".TOP");
			s.add("ALL \"http://dbpedia.org/property/successor\".\"http://dbpedia.org/class/yago/Person100007846\"");
			s.add("\"http://dbpedia.org/class/yago/Person100007846\"");
			s.add(conj);
			s.add("(\"http://dbpedia.org/class/yago/Person100007846\" OR \"http://dbpedia.org/class/yago/Head110162991\")");
			s.add("NOT \"http://dbpedia.org/class/yago/Person100007846\"");
			s.add("(\"http://dbpedia.org/class/yago/HeadOfState110164747\" AND (\"http://dbpedia.org/class/yago/Negotiator110351874\" AND \"http://dbpedia.org/class/yago/Representative110522035\"))");

			s.clear();
//			s.add("(\"http://nlp2rdf.org/ontology/Sentence\" AND (EXISTS \"http://nlp2rdf.org/ontology/syntaxTreeHasPart\".\"http://nachhalt.sfb632.uni-potsdam.de/owl/stts.owl#Pronoun\" AND EXISTS \"http://nlp2rdf.org/ontology/syntaxTreeHasPart\".\"http://nlp2rdf.org/ontology/sentencefinalpunctuation_tag\"))");
//			s.add("(\"http://nlp2rdf.org/ontology/Sentence\" AND (\"http://nlp2rdf.org/ontology/hasLemma\" VALUE \"test\" )");

			String prefix = "http://nlp2rdf.org/ontology/";
			String test = "(\"Sentence\" AND (EXISTS \"syntaxTreeHasPart\".\"VVPP\" AND EXISTS \"syntaxTreeHasPart\".(\"stts:AuxilliaryVerb\" AND \"hasLemma\" = werden)))";

			ObjectProperty stp  = new ObjectProperty(prefix+"syntaxTreeHasPart");
			DatatypeProperty dtp = new DatatypeProperty(prefix+"hasLemma");
			StringValueRestriction svr = new StringValueRestriction(dtp,"werden" );
			Intersection inner = new Intersection(new NamedClass(prefix+"Auxillary"), svr);
			Intersection middle = new Intersection(
					new ObjectSomeRestriction(stp, new NamedClass(prefix+"VVPP")),
					new ObjectSomeRestriction(stp, inner));
			Intersection outer = new Intersection(
					new NamedClass(prefix+"Sentence"),
					middle
					);
			
			System.out.println(outer.toKBSyntaxString(null,null));
			System.out.println(test);

//			s.add(outer.toKBSyntaxString(null,null));
			SparqlQueryDescriptionConvertVisitor testVisitor = new SparqlQueryDescriptionConvertVisitor();
			String q = testVisitor.getSparqlQuery(outer.toKBSyntaxString());
			System.out.println(q);
			if (true) {
				System.exit(0);
			}
			
//			<http://nlp2rdf.org/ontology/sentencefinalpunctuation_tag>
			String query = "";
			SparqlQueryDescriptionConvertVisitor visit = new SparqlQueryDescriptionConvertVisitor();
			visit.setLabels(false);
			visit.setDistinct(false);
//			visit.setClassToSubclassesVirtuoso(subclassMap);
			
			
			
			for (String kbsyntax : s) {
				query = visit.getSparqlQuery(kbsyntax);
				result.put(kbsyntax, query);
			}
			System.out.println("************************");
			for (String string : result.keySet()) {
				System.out.println("KBSyntayString: " + string);
				System.out.println("Query:\n" + result.get(string));
				System.out.println("************************");
			}
			System.out.println("Finished");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .Negation)
	 */
	public void visit(Negation description) {
		logger.trace("Negation");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .ObjectAllRestriction)
	 */
	public void visit(ObjectAllRestriction description) {
		logger.trace("ObjectAllRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .ObjectSomeRestriction)
	 */
	public void visit(ObjectSomeRestriction description) {
		logger.trace("ObjectSomeRestriction");
		String option = "";
		if(transitiveProperties!= null && transitiveProperties.contains(description.getRole().toString()) ){
			option =" OPTION (TRANSITIVE , t_in(?" + stack.peek()+"), t_out(?object" + currentObject + "), T_MIN(0), T_MAX(6), T_DIRECTION 1 , T_NO_CYCLES) ";
		}
		query += "\n?" + stack.peek() + " <" + description.getRole() + "> ?object" + currentObject + option + ". ";
		stack.push("object" + currentObject);
		currentObject++;
		description.getChild(0).accept(this);
		stack.pop();
		logger.trace(description.getRole().toString());
		logger.trace(description.getChild(0).toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .Nothing)
	 */
	public void visit(Nothing description) {
		logger.trace("Nothing");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .Thing)
	 */
	public void visit(Thing description) {
		logger.trace("Thing");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .Intersection)
	 */
	public void visit(Intersection description) {
		logger.trace("Intersection");
		description.getChild(0).accept(this);
		query += ". ";
		description.getChild(1).accept(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .Union)
	 */
	public void visit(Union description) {
		// HACK see replace hacks in other functions
		logger.trace("Union");
		query += "{";
		description.getChild(0).accept(this);
		query += "} UNION {";
		description.getChild(1).accept(this);
		query += "}";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .ObjectMinCardinalityRestriction)
	 */
	public void visit(ObjectMinCardinalityRestriction description) {
		logger.trace("ObjectMinCardinalityRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .ObjectExactCardinalityRestriction)
	 */
	public void visit(ObjectExactCardinalityRestriction description) {
		logger.trace("ObjectExactCardinalityRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .ObjectMaxCardinalityRestriction)
	 */
	public void visit(ObjectMaxCardinalityRestriction description) {
		logger.trace("ObjectMaxCardinalityRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .ObjectValueRestriction)
	 */
	public void visit(ObjectValueRestriction description) {
		ObjectProperty op = (ObjectProperty) description.getRestrictedPropertyExpression();
		Individual ind = description.getIndividual();
		query += "\n?" + stack.peek() + " <" + op.getName() + "> <" + ind.getName() + "> ";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .DatatypeValueRestriction)
	 */
	public void visit(DatatypeValueRestriction description) {
		logger.trace("DatatypeValueRestriction");
		query += "\n?" + stack.peek() + " <" + description.getRestrictedPropertyExpression() + ">  \""+description.getValue().getLiteral()+"\" ";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .NamedClass)
	 */
	public void visit(NamedClass description) {
		logger.trace("NamedClass");
		query += "\n?" + stack.peek() + " a <" + description.getName() + "> ";
		foundNamedClasses.add(description.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.algorithms
	 * .gp.ADC)
	 */
	public void visit(ADC description) {
		logger.trace("ADC");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .DatatypeMinCardinalityRestriction)
	 */
	public void visit(DatatypeMinCardinalityRestriction description) {
		logger.trace("DatatypeMinCardinalityRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .DatatypeExactCardinalityRestriction)
	 */
	public void visit(DatatypeExactCardinalityRestriction description) {
		logger.trace("DatatypeExactCardinalityRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .DatatypeMaxCardinalityRestriction)
	 */
	public void visit(DatatypeMaxCardinalityRestriction description) {
		logger.trace("DatatypeMaxCardinalityRestriction");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.owl.DescriptionVisitor#visit(org.dllearner.core.owl
	 * .DatatypeSomeRestriction)
	 */
	public void visit(DatatypeSomeRestriction description) {
		logger.trace("DatatypeSomeRestriction");
	}

	

}
