package org.dllearner.tools.ore.sparql;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.dllearner.kb.extraction.ExtractionAlgorithm;
import org.dllearner.utilities.JamonMonitorLogger;
import org.dllearner.utilities.owl.OWLVocabulary;
import org.mindswap.pellet.PelletOptions;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyDomainAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ProgressMonitor;

import aterm.ATermAppl;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.owlapi.explanation.io.manchester.ManchesterSyntaxExplanationRenderer;
import com.clarkparsia.owlapiv3.XSD;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.jamonapi.Monitor;

public class IncrementalInconsistencyFinder {
	
	private static Logger logger = Logger.getRootLogger();
	
	private static final String ENDPOINT_URL = "http://dbpedia-live.openlinksw.com/sparql";
	private static String DEFAULT_GRAPH_URI = "http://dbpedia.org";
	private static String DBPEDIA_PREDICATE_FILTER = "!regex(?predicate, \"http://dbpedia.org/property\")";
	private static String DBPEDIA_SUBJECT_FILTER = "!regex(?subject, \"http://dbpedia.org/property\")";
	private static String OWL_THING_OBJECT_FILTER = "!regex(?object, \"http://www.w3.org/2002/07/owl#Thing\")";
//	private static final String ENDPOINT_URL = "http://localhost:8890/sparql";
//	private static String DEFAULT_GRAPH_URI = "http://opencyc2.org"; //(version 2.0)
	
	private static int RESULT_LIMIT = 100;
	private static int OFFSET = 100;
	private static int RECURSION_DEPTH = 100;
	private static int AXIOM_COUNT = 100;
	
	//stop if algorithm founds unsatisfiable class or ontology is inconsistent
	private static boolean BREAK_AFTER_ERROR_FOUND = true;
	
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private OWLReasoner reasoner;
	
	private String endpointURI;
	private String defaultGraphURI;
	
	private SPARQLProgressMonitor mon;
	
	private boolean consistent = true;
	
	private PelletExplanation expGen;
	
	private Monitor overallMonitor = JamonMonitorLogger.getTimeMonitor(ExtractionAlgorithm.class, "Overall monitor");
	private Monitor queryMonitor = JamonMonitorLogger.getTimeMonitor(ExtractionAlgorithm.class, "Query monitor");
	private Monitor reasonerMonitor = JamonMonitorLogger.getTimeMonitor(ExtractionAlgorithm.class, "Reasoning monitor");
	
	public IncrementalInconsistencyFinder() throws OWLOntologyCreationException, IOException{
		
		
		SimpleLayout layout = new SimpleLayout();
		ConsoleAppender consoleAppender = new ConsoleAppender(layout);
//		DailyRollingFileAppender fileAppender = new DailyRollingFileAppender(layout, "log/incremental.log", "'.'yyyy-MM-dd_HH");
		logger.removeAllAppenders();
		logger.addAppender(consoleAppender);
//		logger.addAppender(fileAppender);
		logger.setLevel(Level.INFO);
		
		PelletOptions.USE_COMPLETION_QUEUE = true;
		PelletOptions.USE_INCREMENTAL_CONSISTENCY = true;
		PelletOptions.USE_SMART_RESTORE = false;
		
	}
	
	public void setProgressMonitor(SPARQLProgressMonitor mon){
		this.mon = mon;
	}
	
	public void run(String endpointURI, String defaultGraphURI){
		this.endpointURI = endpointURI;
		this.defaultGraphURI = defaultGraphURI;
		logger.info("Searching for inconsistency in " + endpointURI);
		
		mon.setMessage("Searching...");
	
		manager = OWLManager.createOWLOntologyManager();
		try {
			ontology = manager.createOntology(IRI.create(defaultGraphURI + "/" + "fragment"));
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}
		factory = manager.getOWLDataFactory();
		reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
		expGen = new PelletExplanation((PelletReasoner)reasoner);
		
		consistent = true;
		
		overallMonitor.reset();
		reasonerMonitor.reset();
		queryMonitor.reset();
		overallMonitor.start();
		
		int disjointWithCount = getAxiomCountForPredicate(OWL.disjointWith);
		int equivalentClassCount = getAxiomCountForPredicate(OWL.equivalentClass);
		int subClassOfCount = getAxiomCountForPredicate(RDFS.subClassOf);
		
		int domainCount = getAxiomCountForPredicate(RDFS.domain);
		int rangeCount = getAxiomCountForPredicate(RDFS.range);
		
		int subPropertyOfCount = getAxiomCountForPredicate(RDFS.subPropertyOf);
		int equivalentPropertyCount = getAxiomCountForPredicate(OWL.equivalentProperty);
		int inverseOfCount = getAxiomCountForPredicate(OWL.inverseOf);
		int functionalCount = getAxiomCountForObject(OWL.FunctionalProperty);
		int inverseFunctionalCount = getAxiomCountForObject(OWL.InverseFunctionalProperty);
		int transitiveCount = getAxiomCountForObject(OWL.TransitiveProperty);
		
		int classAssertionCount = getAxiomCountForPredicate(RDF.type);
		
		mon.setSize(disjointWithCount + equivalentClassCount + subClassOfCount + domainCount + rangeCount 
				+ subPropertyOfCount + equivalentPropertyCount + inverseOfCount + functionalCount
				+ inverseFunctionalCount + transitiveCount);
		
		
		Set<OWLClass> visitedClasses = new HashSet<OWLClass>();
		Set<OWLNamedIndividual> visitedIndividuals = new HashSet<OWLNamedIndividual>();
		Set<OWLObject> visitedLinkedDataResources = new HashSet<OWLObject>();
		
		Set<OWLAxiom> disjointAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> domainAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> rangeAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> subClassOfAxioms = new HashSet<OWLAxiom>();
		Set<OWLClassAssertionAxiom> classAssertionAxioms = new HashSet<OWLClassAssertionAxiom>();
		
		boolean schemaComplete = false;
		for(int i = 0; i <= RECURSION_DEPTH; i++){
			//first we expand the ontology schema
			
			//retrieve TBox axioms
			if(ontology.getAxiomCount(AxiomType.DISJOINT_CLASSES) < disjointWithCount){
				disjointAxioms.addAll(retrieveClassExpressionsAxioms(OWL.disjointWith, AXIOM_COUNT, OFFSET * i)); 
				manager.addAxioms(ontology, disjointAxioms);
			}
			mon.setProgress(disjointAxioms.size());
			if(ontology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES) < equivalentClassCount){
				manager.addAxioms(ontology, retrieveClassExpressionsAxioms(OWL.equivalentClass, AXIOM_COUNT, OFFSET * i));
			}
			if(ontology.getAxiomCount(AxiomType.SUBCLASS_OF) < subClassOfCount){
				subClassOfAxioms.addAll(retrieveClassExpressionsAxioms(RDFS.subClassOf, AXIOM_COUNT, OFFSET * i));
				manager.addAxioms(ontology, subClassOfAxioms);
			}
			//retrieve RBox axioms
			if(ontology.getAxiomCount(AxiomType.OBJECT_PROPERTY_DOMAIN) + ontology.getAxiomCount(AxiomType.DATA_PROPERTY_DOMAIN) < domainCount){
				domainAxioms.addAll(retrievePropertyAxioms(RDFS.domain, AXIOM_COUNT, OFFSET * i));
				manager.addAxioms(ontology, domainAxioms);
			}
			if(ontology.getAxiomCount(AxiomType.OBJECT_PROPERTY_RANGE) + ontology.getAxiomCount(AxiomType.DATA_PROPERTY_RANGE) < rangeCount){
				rangeAxioms.addAll(retrievePropertyAxioms(RDFS.range, AXIOM_COUNT, OFFSET * i));
				manager.addAxioms(ontology, rangeAxioms);
			}
			if(ontology.getAxiomCount(AxiomType.SUB_OBJECT_PROPERTY) + ontology.getAxiomCount(AxiomType.SUB_DATA_PROPERTY) < subPropertyOfCount){
				manager.addAxioms(ontology, retrievePropertyAxioms(RDFS.subPropertyOf, AXIOM_COUNT, OFFSET * i));
			}
			if(ontology.getAxiomCount(AxiomType.EQUIVALENT_OBJECT_PROPERTIES) + ontology.getAxiomCount(AxiomType.EQUIVALENT_DATA_PROPERTIES) < equivalentPropertyCount){
				manager.addAxioms(ontology, retrievePropertyAxioms(OWL.equivalentProperty, AXIOM_COUNT, OFFSET * i));
			}
			if(ontology.getAxiomCount(AxiomType.INVERSE_OBJECT_PROPERTIES) < inverseOfCount){
				manager.addAxioms(ontology, retrievePropertyAxioms(OWL.inverseOf, AXIOM_COUNT, OFFSET * i));
			}
			if(ontology.getAxiomCount(AxiomType.FUNCTIONAL_OBJECT_PROPERTY) + ontology.getAxiomCount(AxiomType.FUNCTIONAL_DATA_PROPERTY) < functionalCount){
				manager.addAxioms(ontology, retrievePropertyCharacteristicAxioms(OWL.FunctionalProperty, AXIOM_COUNT, OFFSET * i));
			}
			if(ontology.getAxiomCount(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY) < inverseFunctionalCount){
				manager.addAxioms(ontology, retrievePropertyCharacteristicAxioms(OWL.InverseFunctionalProperty, AXIOM_COUNT, OFFSET * i));
			}
			if(ontology.getAxiomCount(AxiomType.TRANSITIVE_OBJECT_PROPERTY) < transitiveCount){
				manager.addAxioms(ontology, retrievePropertyCharacteristicAxioms(OWL.TransitiveProperty, AXIOM_COUNT, OFFSET * i));
			}
			//removal due to find other inconsistencies not evolving this axiom
//			manager.removeAxiom(ontology, factory.getOWLObjectPropertyRangeAxiom(
//					factory.getOWLObjectProperty(IRI.create("http://dbpedia.org/ontology/artist")),
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Person"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Person")),
//					factory.getOWLClass(IRI.create("http://dbpedia.org/ontology/Organisation"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r16i50nS6EdaAAACgyZzFrg")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4rCX1yJHS-EdaAAACgyZzFrg"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r16i50nS6EdaAAACgyZzFrg")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r0y21PnS-EdaAAACgyZzFrg"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r8LonGrboEduAAADggVbxzQ")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx8Ngh4rD2crlmAKQdmLOr88Soi59h4r8LonGrboEduAAADggVbxzQ"))));
//			manager.removeAxiom(ontology, factory.getOWLDisjointClassesAxiom(
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx4r8LonGrboEduAAADggVbxzQ")),
//					factory.getOWLClass(IRI.create("http://sw.opencyc.org/concept/Mx8Ngh4rD2crlmAKQdmLOr88Soi59h4r8LonGrboEduAAADggVbxzQ"))));
					
			for(OWLAxiom ax : disjointAxioms){
				//retrieve instances contained in both classes
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClasses(
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(0).asOWLClass(),
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(1).asOWLClass(), AXIOM_COUNT));
				//retrieve instances contained in first class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(0).asOWLClass(), AXIOM_COUNT));
				//retrieve instances contained in second class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						((OWLDisjointClassesAxiom)ax).getClassExpressionsAsList().get(1).asOWLClass(), AXIOM_COUNT));
				
			}
			if(!isConsistent()){
				break;
			}
			OWLClass domain;
			for(OWLAxiom ax : domainAxioms){
				domain = ((OWLPropertyDomainAxiom<?>)ax).getDomain().asOWLClass();
				//retrieve instances for the domain class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						domain, AXIOM_COUNT));
				//retrieve property assertions
				if(ax instanceof OWLObjectPropertyDomainAxiom){
					manager.addAxioms(ontology, retrieveObjectPropertyAssertionAxioms(((OWLObjectPropertyDomainAxiom)ax).getProperty().asOWLObjectProperty(), AXIOM_COUNT));
				} else {
					manager.addAxioms(ontology, retrieveDataPropertyAssertionAxioms(((OWLDataPropertyDomainAxiom)ax).getProperty().asOWLDataProperty(), AXIOM_COUNT));
				}
				
			}
			if(!isConsistent()){
				break;
			}
			OWLClass range;
			for(OWLAxiom ax : rangeAxioms){
				range = ((OWLObjectPropertyRangeAxiom)ax).getRange().asOWLClass();
				//retrieve instances for the range class
				manager.addAxioms(ontology, retrieveClassAssertionAxiomsForClass(
						range, AXIOM_COUNT));
				//retrieve property assertions
				manager.addAxioms(ontology, retrieveObjectPropertyAssertionAxioms(((OWLObjectPropertyRangeAxiom)ax).getProperty().asOWLObjectProperty(), AXIOM_COUNT));
				
			}
			if(!isConsistent()){
				break;
			}
			//retrieve ClassAssertion axioms for each new class
			for(OWLAxiom ax : subClassOfAxioms){
				for(OWLClass cls : ax.getClassesInSignature()){
					if(visitedClasses.contains(cls)){
						continue;
					}
					classAssertionAxioms.addAll(retrieveClassAssertionAxiomsForClass(cls, 50));
					visitedClasses.add(cls);
				}
				
			}
			manager.addAxioms(ontology, classAssertionAxioms);
			if(!isConsistent()){
				break;
			}
			//for each individual in the ClassAssertion axioms found above, we retrieve further informations
			for(OWLClassAssertionAxiom ax : classAssertionAxioms){
				if(visitedIndividuals.contains(ax.getIndividual().asOWLNamedIndividual())){
					continue;
				}
				manager.addAxioms(ontology, retrieveAxiomsForIndividual(ax.getIndividual().asOWLNamedIndividual(), 1000));
				visitedIndividuals.add(ax.getIndividual().asOWLNamedIndividual());
				if(!isConsistent()){
					break;
				}
			}
			if(!isConsistent()){
				break;
			}
			for(OWLClass cls : ontology.getClassesInSignature()){
				if(cls.toStringID().contains("#")){
					if(visitedLinkedDataResources.contains(cls)){
						continue;
					}
					manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(cls.getIRI()));
					visitedLinkedDataResources.add(cls);
				}
			}
			if(!isConsistent()){
				break;
			}
			for(OWLObjectProperty prop : ontology.getObjectPropertiesInSignature()){
				if(prop.toStringID().contains("#")){
					if(visitedLinkedDataResources.contains(prop)){
						continue;
					}
					manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(prop.getIRI()));
					visitedLinkedDataResources.add(prop);
				}
			}
			if(!isConsistent()){
				break;
			}
			for(OWLDataProperty prop : ontology.getDataPropertiesInSignature()){
				if(prop.toStringID().contains("#")){
					if(visitedLinkedDataResources.contains(prop)){
						continue;
					}
					manager.addAxioms(ontology, getAxiomsFromLinkedDataSource(prop.getIRI()));
					visitedLinkedDataResources.add(prop);
				}
			}
			if(!isConsistent()){
				break;
			}
			
			
			
			disjointAxioms.clear();
			domainAxioms.clear();
			rangeAxioms.clear();
			subClassOfAxioms.clear();
			classAssertionAxioms.clear();
		}
		
		overallMonitor.stop();
		//show some statistics
		showStats();
		
		logger.info("Ontology is consistent: " + reasoner.isConsistent());
//		((PelletReasoner)reasoner).getKB().setDoExplanation(true);
//		for(ATermAppl a : ((PelletReasoner)reasoner).getKB().getExplanationSet()){
//			System.err.println(a);
//		}
//		for(Set<OWLAxiom> exp : expGen.getInconsistencyExplanations()){
//			for(OWLAxiom ax : exp){
//				System.out.println(ax);
//				for(OWLClass cls : ax.getClassesInSignature()){
//					System.out.println(cls + " = " + getLabel(cls.toStringID()));
//				}
//				for(OWLObjectProperty prop : ax.getObjectPropertiesInSignature()){
//					System.out.println(prop + " = " + getLabel(prop.toStringID()));
//				}
//				for(OWLNamedIndividual ind : ax.getIndividualsInSignature()){
//					System.out.println(ind + " = " + getLabel(ind.toStringID()));
//				}
//				for(OWLDataProperty prop : ax.getDataPropertiesInSignature()){
//					System.out.println(prop + " = " + getLabel(prop.toStringID()));
//				}
//			}
//			
//		}
//		try {
//			ManchesterSyntaxExplanationRenderer renderer = new ManchesterSyntaxExplanationRenderer();
//			PrintWriter out = new PrintWriter( System.out );
//			renderer.startRendering( out );
//			renderer.render(expGen.getInconsistencyExplanations());
//			renderer.endRendering();
//		} catch (UnsupportedOperationException e) {
//			e.printStackTrace();
//		} catch (OWLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	
	}
	
	private boolean isConsistent(){
		reasonerMonitor.start();
		if(!consistent || !reasoner.isConsistent()){
//			mon.inconsistencyFound(expGen.getInconsistencyExplanation());
			consistent = false;
		}
		reasonerMonitor.stop();
		return consistent;
	}
	
	public OWLOntology getOntology(){
		return ontology;
	}
	
	public Set<OWLAxiom> getExplanation(){
		return expGen.getInconsistencyExplanation();
	}
	
	/**
	 * THis method checks incrementally for unsatisfiable classes in the knowledgebase.
	 * @param endpointURI
	 */
	private void checkForUnsatisfiableClasses(String endpointURI){
		this.endpointURI = endpointURI;
		logger.info("Searching for unsatisfiable classes in " + endpointURI);
		
//		reasoner = PelletIncremantalReasonerFactory.getInstance().createReasoner(ontology);
		ReasonerFactory reasonerFactory = new ReasonerFactory();
		reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		
		overallMonitor.reset();
		reasonerMonitor.reset();
		queryMonitor.reset();
		overallMonitor.start();
		
		
		Set<OWLClass> visitedClasses = new HashSet<OWLClass>();
		Set<OWLClass> classesToVisit = new HashSet<OWLClass>();
		Set<OWLClass> tmp = new HashSet<OWLClass>();
		
		//we are starting with disjointClasses axioms
		Set<OWLDisjointClassesAxiom> disjointAxioms = retrieveDisjointClassAxioms();
		manager.addAxioms(ontology, disjointAxioms);
		
		for(OWLDisjointClassesAxiom ax : disjointAxioms){
			for(OWLClassExpression cl : ax.getClassExpressions()){
				if(!cl.isAnonymous()){
					classesToVisit.add(cl.asOWLClass());
				}
			}
		}
		boolean foundUnsatisfiableClasses = false;
		Set<OWLClass> unsatClasses;
		logger.info("Starting with " + classesToVisit.size() + " classes to visit.");
		for(int i = 1; i <= RECURSION_DEPTH; i++){
			logger.info("Recursion depth = " + i);
			for(OWLClass cl : classesToVisit){
				logger.info("Starting retrieving axioms for class " + cl);
				Set<OWLAxiom> axioms = retrieveAxiomsForClass(cl, false);
//				Set<OWLAxiom> axioms = retrieveAxiomsForClassSingleQuery(cl, false);
				manager.addAxioms(ontology, axioms);
//				if(!axioms.isEmpty()){
//					logger.info("Checking for unsatisfiable classes");
//					reasonerMonitor.start();
//					unsatClasses = reasoner.getUnsatisfiableClasses().getEntities();
//					foundUnsatisfiableClasses = !unsatClasses.isEmpty();
//					logger.info("Found " + unsatClasses.size() + " unsatisfiable classes");
//					reasonerMonitor.stop();
//					
//				}
				for(OWLAxiom ax : axioms){
					tmp.addAll(ax.getClassesInSignature());
				}
				if(foundUnsatisfiableClasses && BREAK_AFTER_ERROR_FOUND){
					logger.info("Found unsatisfiable classes. Aborting.");
					break;
				}
			}
			if(foundUnsatisfiableClasses && BREAK_AFTER_ERROR_FOUND){
				break;
			}
			visitedClasses.addAll(classesToVisit);
			tmp.removeAll(visitedClasses);
			classesToVisit.clear();
			classesToVisit.addAll(tmp);
			tmp.clear();
		}
		

		overallMonitor.stop();
		showStats();
		
		Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
		logger.info("Found " + unsatisfiableClasses.getSize() + " unsatisfiable class(es):");
		logger.info(unsatisfiableClasses.getEntities());
		if(!unsatisfiableClasses.getEntities().isEmpty()){
			BlackBoxExplanation expGen = new BlackBoxExplanation(ontology, reasonerFactory, reasoner);
			for(OWLClass cls : unsatisfiableClasses){
				System.err.println(cls);
				System.out.println(expGen.getExplanation(cls));
			}
			
		}
		
		
	}
	
	/**
	 * This method checks incrementally the consistency of the knowledgebase.
	 * @param endpointURI
	 */
	private void checkForInconsistency(String endpointURI){
		this.endpointURI = endpointURI;
		logger.info("Searching for inconsistency in " + endpointURI);
		
		PelletOptions.USE_COMPLETION_QUEUE = true;
		PelletOptions.USE_INCREMENTAL_CONSISTENCY = true;
		PelletOptions.USE_SMART_RESTORE = false;
		
		OWLReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
		
		overallMonitor.reset();
		reasonerMonitor.reset();
		queryMonitor.reset();
		overallMonitor.start();
		
		Set<OWLClass> visitedClasses = new HashSet<OWLClass>();
		Set<OWLObjectProperty> visitedObjectProperties = new HashSet<OWLObjectProperty>();
		Set<OWLNamedIndividual> visitedIndividuals = new HashSet<OWLNamedIndividual>();
		Set<OWLClass> classesToVisit = new HashSet<OWLClass>();
		Set<OWLClass> tmp = new HashSet<OWLClass>();
		
		//we are starting with disjointClasses axioms
		Set<OWLDisjointClassesAxiom> disjointAxioms = retrieveDisjointClassAxioms();
		manager.addAxioms(ontology, disjointAxioms);
		
		for(OWLDisjointClassesAxiom ax : disjointAxioms){
			for(OWLClassExpression cl : ax.getClassExpressions()){
				if(!cl.isAnonymous()){
					classesToVisit.add(cl.asOWLClass());
				}
			}
		}
		boolean isConsistent = true;
		Set<OWLAxiom> axioms;
		logger.info("Starting with " + classesToVisit.size() + " classes to visit.");
		for(int i = 1; i <= RECURSION_DEPTH; i++){
			logger.info("Recursion depth = " + i);
			//we retrieve axioms for each class
			for(OWLClass cl : classesToVisit){
				axioms = retrieveAxiomsForClass(cl, RESULT_LIMIT);
				manager.addAxioms(ontology, axioms);
				System.out.println(ontology.getObjectPropertiesInSignature());
				if(!axioms.isEmpty()){
					logger.info("Checking for consistency");
					reasonerMonitor.start();
					isConsistent = reasoner.isConsistent();
					reasonerMonitor.stop();
					
				}
				for(OWLAxiom ax : axioms){
					tmp.addAll(ax.getClassesInSignature());
				}
				if(!isConsistent && BREAK_AFTER_ERROR_FOUND){
					logger.info("Detected inconsistency. Aborting.");
					break;
				}
				
			}
			if(!isConsistent && BREAK_AFTER_ERROR_FOUND){
				break;
			}
			//we retrieve axioms for each individual in the ontology
			logger.info("Retrieving axioms for " + ontology.getIndividualsInSignature().size() + " instances");
			int cnt = 0;
			for(OWLNamedIndividual ind : ontology.getIndividualsInSignature()){
				if(!visitedIndividuals.contains(ind)){
					manager.addAxioms(ontology, retrieveAxiomsForIndividual(ind, RESULT_LIMIT));
					visitedIndividuals.add(ind);
					logger.info("Checking for consistency");
					reasonerMonitor.start();
					isConsistent = reasoner.isConsistent();
					reasonerMonitor.stop();
				}
				if(!isConsistent && BREAK_AFTER_ERROR_FOUND){
					logger.info("Detected inconsistency. Aborting.");
					break;
				}
				cnt++;
				if(cnt == 100){
					break;
				}
				
			}
			if(!isConsistent && BREAK_AFTER_ERROR_FOUND){
				break;
			}
			cnt = 0;
			//we retrieve axioms for each object property in the ontology
			logger.info("Retrieving axioms for " + ontology.getObjectPropertiesInSignature().size() + " properties");
			for(OWLObjectProperty prop : ontology.getObjectPropertiesInSignature()){
				if(!visitedObjectProperties.contains(prop)){
					manager.addAxioms(ontology, retrieveAxiomsForObjectProperty(prop, RESULT_LIMIT));
					visitedObjectProperties.add(prop);
					logger.info("Checking for consistency");
					reasonerMonitor.start();
					isConsistent = reasoner.isConsistent();
					reasonerMonitor.stop();
				}
				if(!isConsistent && BREAK_AFTER_ERROR_FOUND){
					logger.info("Detected inconsistency. Aborting.");
					break;
				}
				cnt++;
				if(cnt == 100){
					break;
				}
				
			}
			if(!isConsistent && BREAK_AFTER_ERROR_FOUND){
				break;
			}
			visitedClasses.addAll(classesToVisit);
			tmp.removeAll(visitedClasses);
			classesToVisit.clear();
			classesToVisit.addAll(tmp);
			tmp.clear();
		}
		
		overallMonitor.stop();
		//show some statistics
		showStats();

		//compute an explanation
		if(!reasoner.isConsistent()){
			((PelletReasoner)reasoner).getKB().setDoExplanation(true);
//			ExplanationGenerator expGen = new PelletExplanationGenerator(ontology);
//			logger.info(expGen.getExplanation(factory.getOWLSubClassOfAxiom(factory.getOWLThing(), factory.getOWLNothing())));
			for(ATermAppl a : ((PelletReasoner)reasoner).getKB().getExplanationSet()){
				System.out.println(a);
			}
		}
	}
	
	private int getAxiomCountForPredicate(Property predicate){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(*) WHERE {");
		sb.append("?subject ").append("<").append(predicate).append(">").append(" ?object.");
//		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		return sparqlResults.nextSolution().getLiteral("?callret-0").getInt();
	}
	
	private int getAxiomCountForObject(Resource resource){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(*) WHERE {");
		sb.append("?subject ").append("?predicate ").append("<").append(resource).append(">.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		return sparqlResults.nextSolution().getLiteral("?callret-0").getInt();
	}
	
	private Set<OWLAxiom> retrieveClassExpressionsAxioms(Property property, int limit, int offset){
		logger.info("Retrieving " + property + " axioms");
		mon.setMessage("Retrieving " + property.getLocalName() + " axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(property).append(">").append(" ?object.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		sb.append(" OFFSET ").append(offset);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		OWLClass cls1;
		OWLClass cls2;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			cls1 = factory.getOWLClass(IRI.create(rdfNodeSubject.toString()));
			cls2 = factory.getOWLClass(IRI.create(rdfNodeObject.toString()));
			
			if(property.equals(RDFS.subClassOf)){
				axioms.add(factory.getOWLSubClassOfAxiom(cls1, cls2));
			} else if(property.equals(OWL.disjointWith)){
				axioms.add(factory.getOWLDisjointClassesAxiom(cls1, cls2));
			} else if(property.equals(OWL.equivalentClass)){
				axioms.add(factory.getOWLEquivalentClassesAxiom(cls1, cls2));
			}
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrievePropertyAxioms(Property property, int limit, int offset){
		logger.info("Retrieving " + property + " axioms");
		mon.setMessage("Retrieving " + property.getLocalName() + " axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(property).append(">").append(" ?object.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		sb.append(" OFFSET ").append(offset);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		boolean isObjectProperty = true;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			if(property.equals(OWL.inverseOf)){
				axioms.add(factory.getOWLInverseObjectPropertiesAxiom(
						factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			} else	if(property.equals(OWL.equivalentProperty)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLEquivalentObjectPropertiesAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLEquivalentDataPropertiesAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLDataProperty(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(property.equals(RDFS.subPropertyOf)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLSubObjectPropertyOfAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLSubDataPropertyOfAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLDataProperty(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(property.equals(RDFS.domain)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLObjectPropertyDomainAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else {
					axioms.add(factory.getOWLDataPropertyDomainAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(property.equals(RDFS.range)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLObjectPropertyRangeAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString())),
							factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else {
					//TODO
				}
			}
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrievePropertyCharacteristicAxioms(Resource characteristic, int limit, int offset){
		logger.info("Retrieving " + characteristic + " axioms");
		mon.setMessage("Retrieving " + characteristic.getLocalName() + " axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(characteristic).append(">.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		sb.append(" OFFSET ").append(offset);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		boolean isObjectProperty = true;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon()){
				continue;
			}
			
			if(characteristic.equals(OWL.FunctionalProperty)){
				isObjectProperty = isObjectProperty(rdfNodeSubject.toString());
				if(isObjectProperty){
					axioms.add(factory.getOWLFunctionalObjectPropertyAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString()))));
				} else {
					axioms.add(factory.getOWLFunctionalDataPropertyAxiom(
							factory.getOWLDataProperty(IRI.create(rdfNodeSubject.toString()))));
				}
			} else if(characteristic.equals(OWL.TransitiveProperty)){
				axioms.add(factory.getOWLTransitiveObjectPropertyAxiom(
						factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString()))));
			} else if(characteristic.equals(OWL.InverseFunctionalProperty)){
				axioms.add(factory.getOWLInverseFunctionalObjectPropertyAxiom(
						factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString()))));
			}
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLClassAssertionAxiom> retrieveClassAssertionAxioms(int limit, int offset){
		logger.info("Retrieving ClassAssertionAxioms axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("?object.");
		sb.append("FILTER ").append("(").append(DBPEDIA_SUBJECT_FILTER).append(")");
		sb.append("}");
		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		sb.append(" OFFSET ").append(offset);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			//TODO filter classes like owl:TransitiveProperty, owl:Class, rdf:Property
			axioms.add(factory.getOWLClassAssertionAxiom(
					factory.getOWLClass(IRI.create(rdfNodeObject.toString())),
					factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString()))));
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private boolean isObjectProperty(String propertyURI){
		if(ontology.getObjectPropertiesInSignature().contains(factory.getOWLObjectProperty(IRI.create(propertyURI)))){
			return true;
		}
		if(ontology.getDataPropertiesInSignature().contains(factory.getOWLDataProperty(IRI.create(propertyURI)))){
			return false;
		}
		logger.info("Checking if property " + propertyURI + " is ObjectProperty");
		queryMonitor.start();
		
		//first check if triple ($prop rdf:type owl:ObjectProperty) is in the knowledgebase
		StringBuilder sb = new StringBuilder();
		sb.append("ASK {");
		sb.append("<").append(propertyURI).append("> ").append("<").append(RDF.type).append("> ").append("<").append(OWL.ObjectProperty).append(">");
		sb.append("}");
		Query query = QueryFactory.create(sb.toString());
		QueryExecution sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		if(sparqlQueryExec.execAsk()){
			logger.info("YES");
			return true;
		}
		
		//we check if triple ($prop rdf:type owl:DaatProperty) is in the knowledgebase
		sb = new StringBuilder();
		sb.append("ASK {");
		sb.append("<").append(propertyURI).append("> ").append("<").append(RDF.type).append("> ").append("<").append(OWL.DatatypeProperty).append(">");
		sb.append("}");
		query = QueryFactory.create(sb.toString());
		sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		if(sparqlQueryExec.execAsk()){
			logger.info("NO");
			return false;
		}
		
		//we check for one triple, and decide if object is an literal
		sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(propertyURI).append("> ").append("?object");
		sb.append("}");
		sb.append("LIMIT 1");
		query = QueryFactory.create(sb.toString());
		sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet results = sparqlQueryExec.execSelect();
		
		boolean isObjectProperty = true;
		while(results.hasNext()){
			QuerySolution solution = results.nextSolution();
			
			RDFNode rdfNodeSubject = solution.get("?object");
			isObjectProperty = !rdfNodeSubject.isLiteral();
		}
		queryMonitor.stop();
		logger.info((isObjectProperty ? "YES" : "NO"));
		return isObjectProperty;
	}
	
	private Set<OWLClassAssertionAxiom> retrieveClassAssertionAxiomsForClasses(OWLClass cls1, OWLClass cls2, int limit){
		logger.info("Retrieving ClassAssertion axioms for class " + cls1 + " and " + cls2);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(cls1.toStringID()).append(">.");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(cls2.toStringID()).append(">.");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLNamedIndividual individual;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			
			individual = factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString()));
			
			axioms.add(factory.getOWLClassAssertionAxiom(cls1, individual));
			axioms.add(factory.getOWLClassAssertionAxiom(cls2, individual));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveObjectPropertyAssertionAxioms(OWLObjectProperty prop, int limit){
		logger.info("Retrieving ObjectPropertyAssertion axioms for property " + prop);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(prop.toStringID()).append("> ").append("?object");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			if(solution.get("?object").isLiteral()){
				continue;
			}
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			axioms.add(factory.getOWLObjectPropertyAssertionAxiom(
					prop,
					factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString())),
					factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveDataPropertyAssertionAxioms(OWLDataProperty prop, int limit){
		logger.info("Retrieving DataPropertyAssertion axioms for property " + prop);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(prop.toStringID()).append("> ").append("?object");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		Literal object;
		OWLLiteral literal;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			if(solution.get("?object").isResource()){
				continue;
			}
			
			rdfNodeSubject = solution.getResource("?subject");
			object = solution.getLiteral("?object");
			
			if(object.getDatatype() != null){
				if(object.getDatatype().equals(XSD.DOUBLE)){
					literal = factory.getOWLTypedLiteral(object.getDouble());
				} else if(object.getDatatype().equals(XSD.INT)){
					literal = factory.getOWLTypedLiteral(object.getInt());
				} else if(object.getDatatype().equals(XSD.FLOAT)){
					literal = factory.getOWLTypedLiteral(object.getFloat());
				} else if(object.getDatatype().equals(XSD.BOOLEAN)){
					literal = factory.getOWLTypedLiteral(object.getBoolean());
				} else if(object.getDatatype().equals(XSD.STRING)){
					literal = factory.getOWLTypedLiteral(object.getString());
				} else {
					//TODO 
//					literal = factory.getOWLTypedLiteral(object.getString(),
//							factory.getOWLDatatype(IRI.create(object.getDatatype().getURI())));
					literal = factory.getOWLStringLiteral(object.getString());
				}
			} else {
				literal = factory.getOWLStringLiteral(object.getString());
			}
			axioms.add(factory.getOWLDataPropertyAssertionAxiom(
					prop,
					factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString())),
					literal));
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLClassAssertionAxiom> retrieveClassAssertionAxiomsForClass(OWLClass cls, int limit){
		logger.info("Retrieving ClassAssertion axioms for class " + cls);
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDF.type).append("> ").append("<").append(cls.toStringID()).append(">.");
		sb.append("}");
		sb.append(" ORDER BY ").append("?subject");
		sb.append(" LIMIT ").append(limit);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLNamedIndividual individual;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			
			individual = factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString()));
			
			axioms.add(factory.getOWLClassAssertionAxiom(cls, individual));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLDisjointClassesAxiom> retrieveDisjointClassAxioms(){
		logger.info("Retrieving disjointClasses axioms");
		mon.setMessage("Retrieving disjointClasses axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(OWL.disjointWith).append(">").append(" ?object");
		sb.append("}");
		sb.append("LIMIT ").append(RESULT_LIMIT);
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLDisjointClassesAxiom> axioms = new HashSet<OWLDisjointClassesAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		OWLClass disjointClass1;
		OWLClass disjointClass2;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			disjointClass1 = factory.getOWLClass(IRI.create(rdfNodeSubject.toString()));
			disjointClass2 = factory.getOWLClass(IRI.create(rdfNodeObject.toString()));
			
			axioms.add(factory.getOWLDisjointClassesAxiom(disjointClass1, disjointClass2));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLClassAssertionAxiom> retrievePropertyAssertionAxiomsWithFunctionalProperty(int limit){
		queryMonitor.start();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ?s, ?p, COUNT(?o) WHERE {");
		sb.append("?p").append("<").append(RDF.type).append("> ").append("<").append(OWL.FunctionalProperty).append(">.");
		sb.append("?s ?p ?o.");
		sb.append("}");
		sb.append(" HAVING COUNT(?o)>1"); 
		sb.append(" LIMIT ").append(limit);
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLClassAssertionAxiom> axioms = new HashSet<OWLClassAssertionAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodePredicate;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?s");
			rdfNodePredicate = solution.getResource("?p");
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveSubClassAxioms(){
		logger.info("Retrieving SubClassOf axioms");
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("?subject ").append("<").append(RDFS.subClassOf).append(">").append(" ?object");
		sb.append("}");
		sb.append("LIMIT ").append(RESULT_LIMIT);
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodeObject;
		OWLClass subClass;
		OWLClass superClass;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?subject");
			rdfNodeObject = solution.getResource("?object");
			
			//skip if solution contains blank node
			if(rdfNodeSubject.isAnon() || rdfNodeObject.isAnon()){
				continue;
			}
			
			subClass = factory.getOWLClass(IRI.create(rdfNodeSubject.toString()));
			superClass = factory.getOWLClass(IRI.create(rdfNodeObject.toString()));
			
			axioms.add(factory.getOWLSubClassOfAxiom(subClass, superClass));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	/**
	 * Get axioms for a given class.
	 * Axiom types: SubClassOf, EquivalentClass, ClassAssertion
	 * 
	 * @param cl
	 * @return
	 */
	private Set<OWLAxiom> retrieveAxiomsForClass(OWLClass cl, int limit){
		logger.info("Retrieving axioms for class " + cl);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("{<").append(cl.toStringID()).append("> ").append("?predicate").append(" ?object.}");
		sb.append(" UNION ");
		sb.append("{?subject").append("?predicate").append(" <").append(cl.toStringID()).append("> }");
		sb.append("}");
		sb.append("LIMIT ").append(limit);
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet results = queryExec.execSelect();
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		RDFNode rdfNodePredicate;
		RDFNode rdfNodeObject;
		while(results.hasNext()){
			solution = results.nextSolution();
			if(solution.get("?object") != null){
				rdfNodePredicate = solution.getResource("?predicate");
				rdfNodeObject = solution.get("?object");
				//skip if object is a blank node
				if(rdfNodeObject.isAnon()){
					continue;
				}
				if(rdfNodePredicate.equals(RDFS.subClassOf)){
					axioms.add(factory.getOWLSubClassOfAxiom(cl, factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				} else if(rdfNodePredicate.equals(OWL.equivalentClass)){
					axioms.add(factory.getOWLEquivalentClassesAxiom(cl, factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
				}
			} else if(solution.get("?subject") != null){
				rdfNodePredicate = solution.getResource("?predicate");
				rdfNodeSubject = solution.get("?subject");
				//skip if subject is a blank node
				if(rdfNodeSubject.isAnon()){
					continue;
				}
				if(rdfNodePredicate.equals(RDF.type)){
					axioms.add(factory.getOWLClassAssertionAxiom(cl, factory.getOWLNamedIndividual(IRI.create(rdfNodeSubject.toString()))));
				} else if(rdfNodePredicate.equals(RDFS.subClassOf)){
					axioms.add(factory.getOWLSubClassOfAxiom(factory.getOWLClass(IRI.create(rdfNodeSubject.toString())), cl));
				}
			}
			
			
		}System.out.println(axioms);
		if(axioms.isEmpty()){
			axioms.addAll(getAxiomsFromLinkedDataSource(cl.getIRI()));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	/**
	 * Get axioms for a given ObjectProperty.
	 * Axiom types: TransitiveProperty, FunctionalProperty, InverseFunctionalProperty, SymmetricProperty, EquivalentProperty,
	 * SubProperty, InverseOf, Domain, Range
	 * 
	 * @param prop
	 * @return
	 */
	private Set<OWLAxiom> retrieveAxiomsForObjectProperty(OWLObjectProperty prop, int limit){
		logger.info("Retrieving axioms for property " + prop);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("<").append(prop.toStringID()).append("> ").append("?predicate").append(" ?object.}");
		sb.append("LIMIT ").append(limit);
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet results = queryExec.execSelect();
		QuerySolution solution;
		RDFNode rdfNodePredicate;
		RDFNode rdfNodeObject;
		while(results.hasNext()){
			solution = results.nextSolution();
			rdfNodePredicate = solution.getResource("?predicate");
			rdfNodeObject = solution.get("?object");
			//skip if object is a blank node
			if(rdfNodeObject.isAnon()){
				continue;
			}
			if(rdfNodePredicate.equals(RDF.type)){
				if(rdfNodeObject.equals(OWL.TransitiveProperty)){
					axioms.add(factory.getOWLTransitiveObjectPropertyAxiom(prop));
				} else if(rdfNodeObject.equals(OWL.FunctionalProperty)){
					axioms.add(factory.getOWLFunctionalObjectPropertyAxiom(prop));
				} else if(rdfNodeObject.equals(OWL.InverseFunctionalProperty)){
					axioms.add(factory.getOWLInverseFunctionalObjectPropertyAxiom(prop));
				} else if(rdfNodeObject.equals(OWL.SymmetricProperty)){
					axioms.add(factory.getOWLSymmetricObjectPropertyAxiom(prop));
				}
			} else if(rdfNodePredicate.equals(RDFS.subPropertyOf)){
				axioms.add(factory.getOWLSubObjectPropertyOfAxiom(prop,
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(OWL.inverseOf)){
				axioms.add(factory.getOWLInverseObjectPropertiesAxiom(prop,
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(RDFS.domain)){
				axioms.add(factory.getOWLObjectPropertyDomainAxiom(prop,
						factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(RDFS.range)){
				axioms.add(factory.getOWLObjectPropertyRangeAxiom(prop,
						factory.getOWLClass(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(OWL.equivalentProperty)){
				axioms.add(factory.getOWLEquivalentObjectPropertiesAxiom(prop,
						factory.getOWLObjectProperty(IRI.create(rdfNodeObject.toString()))));
			}
			
		}
		if(axioms.isEmpty()){
			axioms.addAll(getAxiomsFromLinkedDataSource(prop.getIRI()));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}

	/**
	 * Retrieve axioms for a given individual.
	 * Axiom types: SameAs, DifferentFrom, ClassAssertion, ObjectPropertyAssertion, DataPropertyAssertion
	 * @param ind
	 * @return
	 */
	private Set<OWLAxiom> retrieveAxiomsForIndividual(OWLNamedIndividual ind, int limit){
		logger.info("Retrieving axioms for individual " + ind);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("<").append(ind.toStringID()).append("> ").append("?predicate").append(" ?object.");
		sb.append("FILTER ").append("(").append(DBPEDIA_PREDICATE_FILTER).append(" && ").append(OWL_THING_OBJECT_FILTER).append(")");
		sb.append("}");
		sb.append(" LIMIT ").append(limit);
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet results = queryExec.execSelect();
		QuerySolution solution;
		RDFNode rdfNodePredicate;
		RDFNode rdfNodeObject;
		OWLLiteral literal;
		while(results.hasNext()){
			solution = results.nextSolution();
			rdfNodePredicate = solution.getResource("?predicate");
			rdfNodeObject = solution.get("?object");
			//skip if object is a blank node
			if(rdfNodeObject.isAnon()){
				continue;
			}
			if(rdfNodePredicate.equals(RDF.type)){
				axioms.add(factory.getOWLClassAssertionAxiom(factory.getOWLClass(IRI.create(rdfNodeObject.toString())), ind));
			} else if(rdfNodePredicate.equals(OWL.sameAs)){
				axioms.add(factory.getOWLSameIndividualAxiom(ind, factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(OWL.differentFrom)){
				axioms.add(factory.getOWLDifferentIndividualsAxiom(ind, factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
			} else if(rdfNodePredicate.equals(RDFS.comment)){
				continue;
			} else if(rdfNodePredicate.equals(RDFS.label)){
				continue;
			} else {
				// we check if property is a DataProperty or ObjectProperty
				if(isObjectProperty(rdfNodePredicate.toString())){
					// for ObjectProperties we skip assertions with literals
					if(rdfNodeObject.isLiteral()){
						continue;
					}
					axioms.add(factory.getOWLObjectPropertyAssertionAxiom(
							factory.getOWLObjectProperty(IRI.create(rdfNodePredicate.toString())),
							ind,
							factory.getOWLNamedIndividual(IRI.create(rdfNodeObject.toString()))));
				} else {
					// for DataProperties we skip assertions with literals
					if(!rdfNodeObject.isLiteral()){
						continue;
					}
					if(rdfNodeObject.equals(RDFS.comment)){
						
					} else if(rdfNodeObject.equals(RDFS.label)){
						
					} else {
						if(((Literal)rdfNodeObject).getDatatype() != null){
							if(((Literal)rdfNodeObject).getDatatype().equals(XSD.DOUBLE)){
								literal = factory.getOWLTypedLiteral(((Literal)rdfNodeObject).getDouble());
							} else if(((Literal)rdfNodeObject).getDatatype().equals(XSD.INT)){
								literal = factory.getOWLTypedLiteral(((Literal)rdfNodeObject).getInt());
							} else if(((Literal)rdfNodeObject).getDatatype().equals(XSD.FLOAT)){
								literal = factory.getOWLTypedLiteral(((Literal)rdfNodeObject).getFloat());
							} else if(((Literal)rdfNodeObject).getDatatype().equals(XSD.BOOLEAN)){
								literal = factory.getOWLTypedLiteral(((Literal)rdfNodeObject).getBoolean());
							} else if(((Literal)rdfNodeObject).getDatatype().equals(XSD.STRING)){
								literal = factory.getOWLTypedLiteral(((Literal)rdfNodeObject).getString());
							} else {
								//TODO occured some conflicts with xsd:date
//								literal = factory.getOWLTypedLiteral(((Literal)rdfNodeObject).getString(),
//										factory.getOWLDatatype(IRI.create(((Literal)rdfNodeObject).getDatatype().getURI())));
								literal = factory.getOWLStringLiteral(((Literal)rdfNodeObject).getString());
							}
						} else {
							literal = factory.getOWLStringLiteral(((Literal)rdfNodeObject).getString());
						}
						axioms.add(factory.getOWLDataPropertyAssertionAxiom(
								factory.getOWLDataProperty(IRI.create(rdfNodePredicate.toString())),
								ind,
								literal));
					}
				}
				
			}
				
				
			
		}
		if(axioms.isEmpty()){
			axioms.addAll(getAxiomsFromLinkedDataSource(ind.getIRI()));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private int getAxiomCount(){
		StringBuilder sb = new StringBuilder();
//		sb.append("SELECT COUNT(*) WHERE {");
//		sb.append("?s").append(" <").append(OWL.disjointWith).append("> ").append("?o}");
		sb.append("select * where {?s ?p ?o} group by ?s having(count(distinct) < 5)");
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet results = queryExec.execSelect();
		System.out.println(results.nextSolution());
		
		return 1;
	}
	
	
	private Set<OWLAxiom> retrieveSubClassAxiomsForClass(OWLClass cl){
		logger.info("Retrieving subClassOf axioms for class " + cl);
		queryMonitor.start();
		
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		QuerySolution solution;
		RDFNode rdfNode;
		
		Query sparqlQuery = createSimpleSelectSPARQLQuery("?x", OWLVocabulary.RDFS_SUBCLASS_OF,
				cl.toStringID(), "regex(?x,\"http://dbpedia.org/ontology/\", \"i\")", RESULT_LIMIT);
		QueryExecution sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, sparqlQuery, defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		if(!sparqlResults.hasNext()){
			logger.info("Got empty SPARQL result. Trying to get informations from linked data uri.");
			try{
				axioms.addAll(manager.loadOntology(IRI.create(cl.toStringID())).getLogicalAxioms());
			} catch (Exception e){
				logger.info("No linked data retrieved.");
			}
		} else {
			OWLClass subClass;
			while(sparqlResults.hasNext()){
				solution = sparqlResults.nextSolution();
				
				rdfNode = solution.getResource("?x");
				if(rdfNode.isAnon()){
					System.out.println("BLANKNODE detected in solution " + solution);
					continue;
				}
				
				subClass = factory.getOWLClass(IRI.create(rdfNode.toString()));
				
				axioms.add(factory.getOWLSubClassOfAxiom(subClass, cl));
			}
		}
		
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLObjectPropertyDomainAxiom> retrievePropertyDomainAxiomsForClass(OWLClass cl){
		logger.info("Retrieving objectPropertyDomain axioms for class " + cl);
		queryMonitor.start();
		
		Query sparqlQuery = createSimpleSelectSPARQLQuery("?s", OWLVocabulary.RDFS_domain, cl.toStringID(),
				"regex(?s,\"http://dbpedia.org/ontology/\", \"i\")", RESULT_LIMIT);
		QueryExecution sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, sparqlQuery, defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLObjectPropertyDomainAxiom> axioms = new HashSet<OWLObjectPropertyDomainAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLObjectProperty property;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?s");
			
			property = factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString()));
			
			axioms.add(factory.getOWLObjectPropertyDomainAxiom(property, cl));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLObjectPropertyRangeAxiom> retrievePropertyRangeAxiomsForClass(OWLClass cl){
		logger.info("Retrieving objectPropertyRange axioms for class " + cl);
		queryMonitor.start();
		
		Query sparqlQuery = createSimpleSelectSPARQLQuery("?s", OWLVocabulary.RDFS_range, cl.toStringID(),
				"regex(?s,\"http://dbpedia.org/ontology/\", \"i\")", RESULT_LIMIT);
		QueryExecution sparqlQueryExec = QueryExecutionFactory.sparqlService(endpointURI, sparqlQuery, defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		Set<OWLObjectPropertyRangeAxiom> axioms = new HashSet<OWLObjectPropertyRangeAxiom>();
		
		QuerySolution solution;
		RDFNode rdfNodeSubject;
		OWLObjectProperty property;
		while(sparqlResults.hasNext()){
			solution = sparqlResults.nextSolution();
			
			rdfNodeSubject = solution.getResource("?s");
			
			property = factory.getOWLObjectProperty(IRI.create(rdfNodeSubject.toString()));
			
			axioms.add(factory.getOWLObjectPropertyRangeAxiom(property, cl));
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	
	private Set<OWLAxiom> retrieveAxiomsForClass(OWLClass cl, boolean retrieveABoxAxioms){
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		axioms.addAll(retrieveSubClassAxiomsForClass(cl));
		axioms.addAll(retrievePropertyDomainAxiomsForClass(cl));
		axioms.addAll(retrievePropertyRangeAxiomsForClass(cl));
		if(retrieveABoxAxioms){
			axioms.addAll(retrieveClassAssertionAxiomsForClass(cl, AXIOM_COUNT));
		}
		return axioms;
	}
	
	private Set<OWLAxiom> retrieveAxiomsForClassSingleQuery(OWLClass cl, boolean retrieveABoxAxioms){
		logger.info("Retrieving axioms for class " + cl);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		queryMonitor.start();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("{").append("<").append(cl.toStringID()).append("> <").append(OWLVocabulary.RDFS_SUBCLASS_OF).append("> ?superClass.").append("FILTER regex(?superClass,\"http://dbpedia.org/ontology/\", \"i\")}");
		sb.append("UNION");
		sb.append("{").append("?subClass ").append("<").append(OWLVocabulary.RDFS_SUBCLASS_OF).append("> <").append(cl.toStringID()).append(">.").append("FILTER regex(?subClass,\"http://dbpedia.org/ontology/\", \"i\")}");
		sb.append("UNION");
		sb.append("{").append("?domainProperty ").append("<").append(OWLVocabulary.RDFS_domain).append("> <").append(cl.toStringID()).append(">.").append("FILTER regex(?domainProperty,\"http://dbpedia.org/ontology/\", \"i\")}");
		sb.append("UNION");
		sb.append("{").append("?rangeProperty ").append("<").append(OWLVocabulary.RDFS_range).append("> <").append(cl.toStringID()).append(">.").append("FILTER regex(?rangeProperty,\"http://dbpedia.org/ontology/\", \"i\")}");
		if(retrieveABoxAxioms){
			sb.append("UNION");
			sb.append("{").append("?instance ").append("<").append(OWLVocabulary.RDF_TYPE).append("> <").append(cl.toStringID()).append(">}");
		}
		sb.append("}");
//		sb.append("LIMIT ").append(RESULT_LIMIT);
		
		Query query = QueryFactory.create(sb.toString());
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpointURI, query, defaultGraphURI);
		ResultSet results = queryExec.execSelect();
		QuerySolution solution;
		RDFNode rdfNode;
		while(results.hasNext()){
			solution = results.nextSolution();
			if(solution.varNames().next().equals("subClass")){
				rdfNode = solution.getResource("?subClass");
				axioms.add(factory.getOWLSubClassOfAxiom(factory.getOWLClass(IRI.create(rdfNode.toString())), cl));
			} else if(solution.varNames().next().equals("superClass")){
				rdfNode = solution.getResource("?superClass");
				axioms.add(factory.getOWLSubClassOfAxiom(cl, factory.getOWLClass(IRI.create(rdfNode.toString()))));
			} else if(solution.varNames().next().equals("domainProperty")){
				rdfNode = solution.getResource("?domainProperty");
				axioms.add(factory.getOWLObjectPropertyDomainAxiom(factory.getOWLObjectProperty(IRI.create(rdfNode.toString())), cl));
			} else if(solution.varNames().next().equals("rangeProperty")){
				rdfNode = solution.getResource("?rangeProperty");
				axioms.add(factory.getOWLObjectPropertyRangeAxiom(factory.getOWLObjectProperty(IRI.create(rdfNode.toString())), cl));
			} else if(solution.varNames().next().equals("instance")){
				rdfNode = solution.getResource("?instance");
				axioms.add(factory.getOWLClassAssertionAxiom(cl, factory.getOWLNamedIndividual(IRI.create(rdfNode.toString()))));
			} 
			
		}
		queryMonitor.stop();
		logger.info("Found " + axioms.size() + " axioms in " + queryMonitor.getLastValue() + " ms");
		return axioms;
	}
	
	private Set<OWLAxiom> getAxiomsFromLinkedDataSource(IRI iri){
		logger.info("Trying to get informations from linked data uri " + iri);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		try{
			OWLObjectProperty prop;
			for(OWLAxiom ax : manager.loadOntology(iri).getLogicalAxioms()){
				if(ax instanceof OWLObjectPropertyDomainAxiom){
					prop = ((OWLObjectPropertyDomainAxiom)ax).getProperty().asOWLObjectProperty();
					if(!isObjectProperty(prop.toStringID())){
						axioms.add(factory.getOWLDataPropertyDomainAxiom(
								factory.getOWLDataProperty(IRI.create(prop.toStringID())),
								((OWLObjectPropertyDomainAxiom)ax).getDomain()));
					} else {
						axioms.add(ax);
					}
				} else {
					axioms.add(ax);
				}
			}
		} catch (Exception e){
			logger.error(e);
			logger.info("No linked data retrieved.");
		}
		logger.info("Got " + axioms.size() + " logical axioms from linked data source");
		return axioms;
	}
	
	private String getLabel(String uri){
		String label = "";
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE {");
		sb.append("<").append(uri).append("> ").append("<").append(RDFS.label).append("> ").append("?label");
		sb.append("}");
		sb.append(" LIMIT 1");
		
		QueryEngineHTTP sparqlQueryExec = new QueryEngineHTTP(endpointURI, sb.toString());
		sparqlQueryExec.addDefaultGraph(defaultGraphURI);
		ResultSet sparqlResults = sparqlQueryExec.execSelect();
		
		while(sparqlResults.hasNext()){
			label = sparqlResults.nextSolution().get("?label").toString();
		}
		return label;
	}
	
	
	
	private void showStats(){
		logger.info("###########################STATS###########################");
		logger.info(ontology);
		logger.info("Overall execution time: " + overallMonitor.getTotal() + " ms");
		logger.info("Overall query time: " + queryMonitor.getTotal() + " ms (" + (int)(queryMonitor.getTotal()/overallMonitor.getTotal()*100) + "%)");
		logger.info("Number of queries sent: " + (int)queryMonitor.getHits());
		logger.info("Average query time: " + queryMonitor.getAvg() + " ms");
		logger.info("Longest query time: " + queryMonitor.getMax() + " ms");
		logger.info("Shortest query time: " + queryMonitor.getMin() + " ms");
		logger.info("Overall reasoning time: " + reasonerMonitor.getTotal() + " ms (" + (int)(reasonerMonitor.getTotal()/overallMonitor.getTotal()*100) + "%)");
		logger.info("Number of reasoner calls: " + (int)reasonerMonitor.getHits());
		logger.info("Average reasoning time: " + reasonerMonitor.getAvg() + " ms");
		logger.info("Longest reasoning time: " + reasonerMonitor.getMax() + " ms");
		logger.info("Shortest reasoning time: " + reasonerMonitor.getMin() + " ms");
	}
	
	
	private Query createSimpleSelectSPARQLQuery(String subject, String predicate, String object, String filter, int limit){
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT * WHERE {");
		if(!subject.startsWith("?")){
			sb.append("<").append(subject).append(">");
		} else {
			sb.append(subject);
		}
		sb.append(" ");
		if(!predicate.startsWith("?")){
			sb.append("<").append(predicate).append(">");
		} else {
			sb.append(predicate);
		}
		sb.append(" ");
		if(!object.startsWith("?")){
			sb.append("<").append(object).append(">");
		} else {
			sb.append(object);
		}
//		if(filter != null){
//			sb.append(" FILTER ");
//			sb.append(filter);
//		}
		sb.append("}");
		sb.append(" LIMIT ");
		sb.append(limit);
		Query query = QueryFactory.create(sb.toString());
		return query;
	}
	
	public static void main(String[] args) throws OWLOntologyCreationException, IOException{
		PelletExplanation.setup();
		IncrementalInconsistencyFinder incFinder = new IncrementalInconsistencyFinder();
//		incFinder.checkForUnsatisfiableClasses(ENDPOINT_URL);
//		incFinder.checkForInconsistency(ENDPOINT_URL);
		incFinder.run(ENDPOINT_URL, DEFAULT_GRAPH_URI);
		
//		String queryString = "CONSTRUCT { ?x <" + RDFS.subClassOf + "> ?y } WHERE { ?x <" + RDFS.subClassOf + "> ?y } ORDER BY ?x LIMIT 100 ";
//		Query query = QueryFactory.create(queryString) ;
//		QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT_URL, query);
//		Model resultModel = qexec.execConstruct() ;
//		qexec.close() ;
//		resultModel.write(System.out);
		
		
	}
	
	

}