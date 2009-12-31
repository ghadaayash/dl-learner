package org.dllearner.scripts.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.ComponentManager;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.LearningProblemUnsupportedException;
import org.dllearner.core.ReasonerComponent;
import org.dllearner.core.configurators.CELOEConfigurator;
import org.dllearner.core.owl.Axiom;
import org.dllearner.core.owl.EquivalentClassesAxiom;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.SubClassAxiom;
import org.dllearner.core.owl.Thing;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.reasoning.FastInstanceChecker;
import org.dllearner.reasoning.OWLAPIReasoner;
import org.mindswap.pellet.utils.SetUtils;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;


public class EvaluationComputingScript {
	
	private ReasonerComponent reasoner;
	private OWLFile ks;
	private CELOE celoe;
	private ClassLearningProblem lp;
	
	private String baseURI;
	private Map<String, String> prefixes;
	
	private static double minAccuracy = 0.85;
	
	private static double noisePercent = 5.0;

	private static int minInstanceCount = 5;

	private static int algorithmRuntimeInSeconds = 10;
	
	private static int allListsComputingCount = 5;

	private static DecimalFormat df = new DecimalFormat();

	private static boolean useApproximations = false;
	
	private Set<NamedClass> equivalentReducedClassesSet;
	private Set<NamedClass> superReducedClassesSet;
	
	private URI ontologyURI;
	
	
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastEquivalenceStandardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastEquivalenceFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastEquivalencePredaccMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastEquivalenceGenFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastEquivalenceJaccardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastSuperStandardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastSuperFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastSuperPredaccMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastSuperGenFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> fastSuperJaccardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlEquivalenceStandardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlEquivalenceFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlEquivalencePredaccMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlEquivalenceGenFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlEquivalenceJaccardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	
	
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlSuperStandardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlSuperFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlSuperPredaccMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlSuperGenFMeasureMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> owlSuperJaccardMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	
	
	private Map<NamedClass, List<EvaluatedDescriptionClass>> defaultEquivalenceMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();
	private Map<NamedClass, List<EvaluatedDescriptionClass>> defaultSuperMap = new HashMap<NamedClass, List<EvaluatedDescriptionClass>>();

	private Map<NamedClass, Set<OWLDescription>> assertedEquivalentClasses = new HashMap<NamedClass, Set<OWLDescription>>();
	private Map<NamedClass, Set<OWLDescription>> assertedSuperClasses = new HashMap<NamedClass, Set<OWLDescription>>();
	
	
	public EvaluationComputingScript(URL fileURL) throws ComponentInitException, MalformedURLException, LearningProblemUnsupportedException, URISyntaxException{
		loadOntology(fileURL);
		computeWithApproximation();
		computeSuggestions();
		computeGenFMeasureWithoutDefaultNegation();
		evaluateInconsistencies();
		saveResults();
		
	}
	
	
	private void evaluateInconsistencies(){
		List<Map<NamedClass, List<EvaluatedDescriptionClass>>> superMapList = new ArrayList<Map<NamedClass,List<EvaluatedDescriptionClass>>>();
		List<Map<NamedClass, List<EvaluatedDescriptionClass>>> equivalenceMapList = new ArrayList<Map<NamedClass,List<EvaluatedDescriptionClass>>>();
		
		equivalenceMapList.add(defaultEquivalenceMap);
		equivalenceMapList.add(owlEquivalenceFMeasureMap);
		equivalenceMapList.add(owlEquivalenceGenFMeasureMap);
		equivalenceMapList.add(owlEquivalenceJaccardMap);
		equivalenceMapList.add(owlEquivalencePredaccMap);
		equivalenceMapList.add(owlEquivalenceStandardMap);
		equivalenceMapList.add(fastEquivalenceFMeasureMap);
		equivalenceMapList.add(fastEquivalenceGenFMeasureMap);
		equivalenceMapList.add(fastEquivalenceJaccardMap);
		equivalenceMapList.add(fastEquivalencePredaccMap);
		equivalenceMapList.add(fastEquivalenceStandardMap);
		
		superMapList.add(defaultSuperMap);
		superMapList.add(owlSuperFMeasureMap);
		superMapList.add(owlSuperGenFMeasureMap);
		superMapList.add(owlSuperJaccardMap);
		superMapList.add(owlSuperPredaccMap);
		superMapList.add(owlSuperStandardMap);
		superMapList.add(fastSuperFMeasureMap);
		superMapList.add(fastSuperGenFMeasureMap);
		superMapList.add(fastSuperJaccardMap);
		superMapList.add(fastSuperPredaccMap);
		superMapList.add(fastSuperStandardMap);
		
		Axiom axiom;
		NamedClass nc;
		for(Map<NamedClass, List<EvaluatedDescriptionClass>> map : equivalenceMapList){
			for(Entry<NamedClass, List<EvaluatedDescriptionClass>> entry : map.entrySet()){
				nc = entry.getKey();
				for(EvaluatedDescriptionClass cl : entry.getValue()){
					axiom = new EquivalentClassesAxiom(nc, cl.getDescription());
					boolean followsFromKB = reasoner.isEquivalentClass(cl.getDescription(), nc);
					boolean isConsistent = followsFromKB || reasoner.remainsSatisfiable(axiom);
					cl.setConsistent(isConsistent);
					cl.setFollowsFromKB(followsFromKB);
				}
				
			}
		}
		for(Map<NamedClass, List<EvaluatedDescriptionClass>> map : superMapList){
			for(Entry<NamedClass, List<EvaluatedDescriptionClass>> entry : map.entrySet()){
				nc = entry.getKey();
				for(EvaluatedDescriptionClass cl : entry.getValue()){
					axiom = new SubClassAxiom(nc, cl.getDescription());
					boolean followsFromKB = reasoner.isSuperClassOf(cl.getDescription(), nc);
					boolean isConsistent = followsFromKB || reasoner.remainsSatisfiable(axiom);
					cl.setConsistent(isConsistent);
					cl.setFollowsFromKB(followsFromKB);
				}
				
			}
		}
	}
	
	private void getAssertedAxioms(){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLDataFactory fac = man.getOWLDataFactory();
			OWLOntology ont = man.loadOntology(ontologyURI);
			OWLClass cl;
			for(NamedClass nc : SetUtils.union(defaultEquivalenceMap.keySet(), defaultSuperMap.keySet())){
				cl = fac.getOWLClass(URI.create(nc.getName()));
				assertedEquivalentClasses.put(nc, cl.getEquivalentClasses(ont));
				assertedSuperClasses.put(nc, cl.getSuperClasses(ont));
			}
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void loadOntology(URL fileURL) throws ComponentInitException, URISyntaxException{
		ontologyURI = fileURL.toURI();
		ComponentManager cm = ComponentManager.getInstance();
		// initialize KnowledgeSource
		ks = cm.knowledgeSource(OWLFile.class);
		ks.getConfigurator().setUrl(fileURL);
		ks.init();
			
		System.out.println("Loaded ontology " + fileURL + ".");
	}

	private void saveResults() {
		OutputStream fos = null;
		File old = new File(ontologyURI.getPath());
		int index = old.getName().lastIndexOf('.');
		String fileName = "test.res";
	    if (index > 0) {
	    	  fileName = old.getName().substring(0, index) + ".res";
	    }  
		File file = new File(fileName);
		try {
			fos = new FileOutputStream(file);
			ObjectOutputStream o = new ObjectOutputStream(fos);
			
			o.writeObject(owlEquivalenceStandardMap);
			o.writeObject(owlEquivalenceFMeasureMap);
			o.writeObject(owlEquivalencePredaccMap);
			o.writeObject(owlEquivalenceJaccardMap);
			o.writeObject(owlEquivalenceGenFMeasureMap);
			
			o.writeObject(owlSuperStandardMap);
			o.writeObject(owlSuperFMeasureMap);
			o.writeObject(owlSuperPredaccMap);
			o.writeObject(owlSuperJaccardMap);
			o.writeObject(owlSuperGenFMeasureMap);
			
			o.writeObject(fastEquivalenceStandardMap);
			o.writeObject(fastEquivalenceFMeasureMap);
			o.writeObject(fastEquivalencePredaccMap);
			o.writeObject(fastEquivalenceJaccardMap);
			o.writeObject(fastEquivalenceGenFMeasureMap);
			
			o.writeObject(fastSuperStandardMap);
			o.writeObject(fastSuperFMeasureMap);
			o.writeObject(fastSuperPredaccMap);
			o.writeObject(fastSuperJaccardMap);
			o.writeObject(fastSuperGenFMeasureMap);
			
			o.writeObject(defaultEquivalenceMap);
			o.writeObject(defaultSuperMap);
			
			o.writeObject(baseURI);
			o.writeObject(prefixes);
			
			o.writeObject(assertedEquivalentClasses);
			o.writeObject(assertedSuperClasses);
			
			o.flush();
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}
	
	private void computeSuggestions() throws ComponentInitException, MalformedURLException,
			LearningProblemUnsupportedException {
		ComponentManager cm = ComponentManager.getInstance();
		ClassLearningProblem lp = null;
		CELOE celoe = null;
		CELOEConfigurator cf = null;
		TreeSet<EvaluatedDescriptionClass> suggestions;
		for (int i = 0; i <= 1; i++) {
			if (i == 0) {
				reasoner = null;
				reasoner = cm.reasoner(OWLAPIReasoner.class, ks);
				System.out.println("using OWLAPI-Reasoner");
			} else {
				reasoner = null;
				reasoner = cm.reasoner(FastInstanceChecker.class, ks);
				System.out.println("using FastInstanceChecker");
			}

			reasoner.init();
			baseURI = reasoner.getBaseURI();
			prefixes = reasoner.getPrefixes();

			// loop through all classes
			for (NamedClass nc : SetUtils.union(equivalentReducedClassesSet, superReducedClassesSet)) {
					System.out.println("\nlearning axioms for class " + nc.toManchesterSyntaxString(baseURI, prefixes)
							);
//					ClassLearningProblem lp = cm.learningProblem(ClassLearningProblem.class, reasoner);
//					lp.getConfigurator().setClassToDescribe(nc.getURI().toURL());
					for (int j = 0; j <= 1; j++) {
						if (j == 0) {
							if(!equivalentReducedClassesSet.contains(nc)){
								continue;
							}
						} else {
							if(!superReducedClassesSet.contains(nc)){
								continue;
							}
						}
						for (int k = 0; k <= 3; k++) {
							lp = cm.learningProblem(ClassLearningProblem.class, reasoner);
							lp.getConfigurator().setClassToDescribe(nc.getURI().toURL());
							lp.getConfigurator().setCheckConsistency(false);
							if (j == 0) {
								lp.getConfigurator().setType("equivalence");
								System.out.println("Learning equivalentClass expressions");
							} else {
								lp.getConfigurator().setType("superClass");
								System.out.println("Learning superClass expressions");
							}
							if (k == 0) {
								lp.getConfigurator().setAccuracyMethod("standard");
								System.out.println("Using accuracy method: standard");
							} else if (k == 1) {
								lp.getConfigurator().setAccuracyMethod("fmeasure");
								System.out.println("Using accuracy method: F-Measure");
							} else if (k == 2) {
								lp.getConfigurator().setAccuracyMethod("pred_acc");
								System.out.println("Using accuracy method: Predictive accuracy");
							} else if (k == 3) {
								lp.getConfigurator().setAccuracyMethod("jaccard");
								System.out.println("Using accuracy method: Jaccard");
							} else {
								lp.getConfigurator().setAccuracyMethod("generalised_fmeasure");
								System.out.println("Using accuracy method: Generalised F-Measure");
							}
							lp.getConfigurator().setUseApproximations(useApproximations);
							lp.init();
							celoe = cm.learningAlgorithm(CELOE.class, lp, reasoner);
							cf = celoe.getConfigurator();
							cf.setUseNegation(false);
							cf.setValueFrequencyThreshold(3);
							cf.setMaxExecutionTimeInSeconds(algorithmRuntimeInSeconds);
							cf.setNoisePercentage(noisePercent);
							cf.setMaxNrOfResults(10);
							celoe.init();

							celoe.start();
							suggestions = (TreeSet<EvaluatedDescriptionClass>) celoe
							.getCurrentlyBestEvaluatedDescriptions();
							List<EvaluatedDescriptionClass> suggestionsList = new LinkedList<EvaluatedDescriptionClass>(
									suggestions.descendingSet());

							if (i == 0) {
								if (j == 0) {
									if (k == 0) {
										owlEquivalenceStandardMap.put(nc, suggestionsList);
									} else if (k == 1) {
										owlEquivalenceFMeasureMap.put(nc, suggestionsList);
									} else if (k == 2) {
										owlEquivalencePredaccMap.put(nc, suggestionsList);
									} else if (k == 3) {
										owlEquivalenceJaccardMap.put(nc, suggestionsList);
									} else {
										owlEquivalenceGenFMeasureMap.put(nc, suggestionsList);
									}
								} else {
									if (k == 0) {
										owlSuperStandardMap.put(nc, suggestionsList);
									} else if (k == 1) {
										owlSuperFMeasureMap.put(nc, suggestionsList);
									} else if (k == 2) {
										owlSuperPredaccMap.put(nc, suggestionsList);
									} else if (k == 3) {
										owlSuperJaccardMap.put(nc, suggestionsList);
									} else {
										owlSuperGenFMeasureMap.put(nc, suggestionsList);
									}
								}
							} else {
								if (j == 0) {
									if (k == 0) {
										fastEquivalenceStandardMap.put(nc, suggestionsList);
									} else if (k == 1) {
										fastEquivalenceFMeasureMap.put(nc, suggestionsList);
									} else if (k == 2) {
										fastEquivalencePredaccMap.put(nc, suggestionsList);
									} else if (k == 3) {
										fastEquivalenceJaccardMap.put(nc, suggestionsList);
									} else {
										fastEquivalenceGenFMeasureMap.put(nc, suggestionsList);
									}
								} else {
									if (k == 0) {
										fastSuperStandardMap.put(nc, suggestionsList);
									} else if (k == 1) {
										fastSuperFMeasureMap.put(nc, suggestionsList);
									} else if (k == 2) {
										fastSuperPredaccMap.put(nc, suggestionsList);
									} else if (k == 3) {
										fastSuperJaccardMap.put(nc, suggestionsList);
									} else {
										fastSuperGenFMeasureMap.put(nc, suggestionsList);
									}
								}
							}
							// }
							cm.freeComponent(celoe);
							 cm.freeComponent(lp);
						}
					}
			}

			cm.freeComponent(reasoner);
		}
		cm.freeComponent(reasoner);
		cm.freeComponent(lp);
		cm.freeComponent(celoe);
	}
	
	
	/**
	 * Computing results for accuracy method 'Generalised F-Measure'. This is done separate because
	 * for FastInstanceChecker option useDefaultNegation is set to 'false'.
	 * @throws ComponentInitException
	 * @throws MalformedURLException
	 * @throws LearningProblemUnsupportedException
	 */
	private void computeGenFMeasureWithoutDefaultNegation() throws ComponentInitException, MalformedURLException,
			LearningProblemUnsupportedException {
		ComponentManager cm = ComponentManager.getInstance();
		ClassLearningProblem lp = null;
		CELOE celoe = null;
		CELOEConfigurator cf = null;
		TreeSet<EvaluatedDescriptionClass> suggestions;
		for (int i = 0; i <= 1; i++) {
			if (i == 0) {
				reasoner = null;
				reasoner = cm.reasoner(OWLAPIReasoner.class, ks);
				System.out.println("using OWLAPI-Reasoner");
			} else {
				reasoner = null;
				reasoner = cm.reasoner(FastInstanceChecker.class, ks);
				((FastInstanceChecker) reasoner).getConfigurator().setDefaultNegation(false);
				System.out.println("using FastInstanceChecker");
			}

			reasoner.init();
			baseURI = reasoner.getBaseURI();
			prefixes = reasoner.getPrefixes();

			for (NamedClass nc : SetUtils.union(equivalentReducedClassesSet, superReducedClassesSet)) {
					System.out.println("\nlearning axioms for class " + nc.toManchesterSyntaxString(baseURI, prefixes)
							);
					
					for (int j = 0; j <= 1; j++) {
						lp = cm.learningProblem(ClassLearningProblem.class, reasoner);
						lp.getConfigurator().setClassToDescribe(nc.getURI().toURL());
						lp.getConfigurator().setCheckConsistency(false);
						if (j == 0) {
							if(!equivalentReducedClassesSet.contains(nc)){
								continue;
							}
							lp.getConfigurator().setType("equivalence");
							System.out.println("Learning equivalentClass expressions");
						} else {
							if(!superReducedClassesSet.contains(nc)){
								continue;
							}
							lp.getConfigurator().setType("superClass");
							System.out.println("Learning superClass expressions");
						}
						lp.getConfigurator().setAccuracyMethod("generalised_fmeasure");
						System.out.println("Using accuracy method: Generalised F-Measure");
						lp.getConfigurator().setUseApproximations(useApproximations);
						lp.init();
						celoe = cm.learningAlgorithm(CELOE.class, lp, reasoner);
						cf = celoe.getConfigurator();
						cf.setUseNegation(false);
						cf.setValueFrequencyThreshold(3);
						cf.setMaxExecutionTimeInSeconds(algorithmRuntimeInSeconds);
						cf.setNoisePercentage(noisePercent);
						cf.setMaxNrOfResults(10);
						celoe.init();

						celoe.start();

						suggestions = (TreeSet<EvaluatedDescriptionClass>) celoe
						.getCurrentlyBestEvaluatedDescriptions();
						List<EvaluatedDescriptionClass> suggestionsList = new LinkedList<EvaluatedDescriptionClass>(
								suggestions.descendingSet());

						if (i == 0) {
							if (j == 0) {
								owlEquivalenceGenFMeasureMap.put(nc, suggestionsList);
							} else {
								owlSuperGenFMeasureMap.put(nc, suggestionsList);
							}

						} else {
							if (j == 0) {
								fastEquivalenceGenFMeasureMap.put(nc, suggestionsList);
							} else {
								fastSuperGenFMeasureMap.put(nc, suggestionsList);
							}

						}
						cm.freeComponent(celoe);
						cm.freeComponent(lp);

					}
					
			}
			cm.freeComponent(reasoner);
		}
//		cm.freeComponent(reasoner);
		cm.freeComponent(lp);
		cm.freeComponent(celoe);
	}
	
	private void computeWithApproximation() throws ComponentInitException, MalformedURLException, LearningProblemUnsupportedException {
		ComponentManager cm = ComponentManager.getInstance();
		TreeSet<EvaluatedDescriptionClass> suggestions;
		reasoner = null;
		reasoner = cm.reasoner(FastInstanceChecker.class, ks);
		reasoner.init();
		baseURI = reasoner.getBaseURI();
		prefixes = reasoner.getPrefixes();

		// loop through all classes
		Set<NamedClass> classes = new TreeSet<NamedClass>(reasoner.getNamedClasses());
		classes.remove(new NamedClass("http://www.w3.org/2002/07/owl#Thing"));
		// reduce number of classes for testing purposes
		// shrinkSet(classes, 20);
		ClassLearningProblem lp = null;
		CELOE celoe = null;
		CELOEConfigurator cf = null;
		for (NamedClass nc : classes) {
			// check whether the class has sufficient instances
			int instanceCount = reasoner.getIndividuals(nc).size();
			if (instanceCount < minInstanceCount) {
				System.out.println("class " + nc.toManchesterSyntaxString(baseURI, prefixes) + " has only "
						+ instanceCount + " instances (minimum: " + minInstanceCount + ") - skipping");
			} else {
				System.out.println("\nlearning axioms for class " + nc.toManchesterSyntaxString(baseURI, prefixes)
						+ " with " + instanceCount + " instances");
				
				for (int i = 0; i <= 1; i++) {
					lp = cm.learningProblem(ClassLearningProblem.class, reasoner);
					lp.getConfigurator().setClassToDescribe(nc.getURI().toURL());
					lp.getConfigurator().setCheckConsistency(false);
					if (i == 0) {
						lp.getConfigurator().setType("equivalence");
						System.out.println("Learning equivalentClass expressions");
					} else {
						lp.getConfigurator().setType("superClass");
						System.out.println("Learning superClass expressions");
					}
					lp.getConfigurator().setUseApproximations(true);
					lp.init();
					celoe = cm.learningAlgorithm(CELOE.class, lp, reasoner);
					cf = celoe.getConfigurator();
					cf.setUseNegation(false);
					cf.setValueFrequencyThreshold(3);
					cf.setMaxExecutionTimeInSeconds(algorithmRuntimeInSeconds);
					cf.setNoisePercentage(noisePercent);
					cf.setMaxNrOfResults(10);
					celoe.init();

					celoe.start();

					// test whether a solution above the threshold was found
					EvaluatedDescription best = celoe.getCurrentlyBestEvaluatedDescription();
					double bestAcc = best.getAccuracy();

					if (bestAcc < minAccuracy || (best.getDescription() instanceof Thing)) {
						System.out
								.println("The algorithm did not find a suggestion with an accuracy above the threshold of "
										+ (100 * minAccuracy)
										+ "% or the best description is not appropriate. (The best one was \""
										+ best.getDescription().toManchesterSyntaxString(baseURI, prefixes)
										+ "\" with an accuracy of " + df.format(bestAcc) + ".) - skipping");
					} else {

						suggestions = (TreeSet<EvaluatedDescriptionClass>) celoe
								.getCurrentlyBestEvaluatedDescriptions();
						List<EvaluatedDescriptionClass> suggestionsList = new LinkedList<EvaluatedDescriptionClass>(
								suggestions.descendingSet());
						if (i == 0) {
							defaultEquivalenceMap.put(nc, suggestionsList);
						} else {
							defaultSuperMap.put(nc, suggestionsList);
						}
					}
					cm.freeComponent(celoe);
					cm.freeComponent(lp);
				}

				

			}
		}
		equivalentReducedClassesSet = getShrinkedSet(defaultEquivalenceMap.keySet(), allListsComputingCount);
		superReducedClassesSet = getShrinkedSet(defaultSuperMap.keySet(), allListsComputingCount);
	}

	private Set<NamedClass> getShrinkedSet(Set<NamedClass> set, int count){
		Set<NamedClass> reducedSet = new HashSet<NamedClass>();
		int i = 0;
		for(NamedClass nc : set){
			if(i % count == 0){
				reducedSet.add(nc);
			}
			i++;
		}
		return reducedSet;
		
	}
	/**
	 * @param args
	 * @throws MalformedURLException 
	 * @throws LearningProblemUnsupportedException 
	 * @throws ComponentInitException 
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws MalformedURLException, ComponentInitException, LearningProblemUnsupportedException, URISyntaxException {
//		Logger.getRootLogger().setLevel(Level.WARN);
		if (args.length == 0) {
			System.out.println("You need to give an OWL file as argument.");
			System.exit(0);
		}
		URL fileURL = null;
		if(args[0].startsWith("http")){
			fileURL = new URL(args[0]);
		} else {
			fileURL = new File(new URL(args[0]).toURI()).toURI().toURL();
		}
		long startTime = System.currentTimeMillis();
		new EvaluationComputingScript(fileURL);
		System.out.println("Overall computing time: " + (System.currentTimeMillis() - startTime)/1000 +" s");
	}

}