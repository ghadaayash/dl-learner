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
package org.dllearner.algorithms.el;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.AbstractCELA;
import org.dllearner.core.AbstractLearningProblem;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.configurators.Configurator;
import org.dllearner.core.configurators.ELLearningAlgorithmConfigurator;
import org.dllearner.core.options.CommonConfigOptions;
import org.dllearner.core.options.ConfigOption;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.Thing;
import org.dllearner.learningproblems.EvaluatedDescriptionPosNeg;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.ScorePosNeg;
import org.dllearner.refinementoperators.ELDown2;
import org.dllearner.utilities.owl.EvaluatedDescriptionSet;

/**
 * A learning algorithm for EL, which will be based on a (hopefully)
 * ideal refinement operator.
 * 
 * TODO redundancy check
 * 
 * @author Jens Lehmann
 *
 */
public class ELLearningAlgorithm extends AbstractCELA {

	private static Logger logger = Logger.getLogger(ELLearningAlgorithm.class);	
	private ELLearningAlgorithmConfigurator configurator;
	
	private ELDown2 operator;
	
	private boolean isRunning = false;
	private boolean stop = false;
	
	private double treeSearchTimeSeconds = 1.0;
	private long treeStartTime;
	
	// a set with limited size (currently the ordering is defined in the class itself)
	private EvaluatedDescriptionSet bestEvaluatedDescriptions = new EvaluatedDescriptionSet(AbstractCELA.MAX_NR_OF_RESULTS);

	private SearchTreeNode startNode;
	private ELHeuristic heuristic;
	private TreeSet<SearchTreeNode> candidates;
	
	public ELLearningAlgorithm(PosNegLP problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
		configurator = new ELLearningAlgorithmConfigurator(this);
	}
	
	public static String getName() {
		return "standard EL learning algorithm";
	}	
	
	public static Collection<Class<? extends AbstractLearningProblem>> supportedLearningProblems() {
		Collection<Class<? extends AbstractLearningProblem>> problems = new LinkedList<Class<? extends AbstractLearningProblem>>();
		problems.add(PosNegLP.class);
		return problems;
	}
	
	// we can assume a PosNegLP, because it is the only supported one
	private PosNegLP getLearningProblem() {
		return (PosNegLP) learningProblem;
	}
	
	@Override
	public ELLearningAlgorithmConfigurator getConfigurator() {
		return configurator;
	}	
	
	public static Collection<ConfigOption<?>> createConfigOptions() {
		Collection<ConfigOption<?>> options = new LinkedList<ConfigOption<?>>();
//		options.add(CommonConfigOptions.getNoisePercentage());
//		options.add(new StringConfigOption("startClass", "the named class which should be used to start the algorithm (GUI: needs a widget for selecting a class)"));
		options.add(CommonConfigOptions.getInstanceBasedDisjoints());
		return options;
	}		
	
	@Override
	public void init() throws ComponentInitException {
		// currently we use the stable heuristic
		heuristic = new StableHeuristic();
		candidates = new TreeSet<SearchTreeNode>(heuristic);
		
		operator = new ELDown2(reasoner, configurator.getInstanceBasedDisjoints());
	}	
	
	@Override
	public void start() {
		stop = false;
		isRunning = true;
		reset();
		treeStartTime = System.nanoTime();
		
		// create start node
		ELDescriptionTree top = new ELDescriptionTree(reasoner, Thing.instance);
		addDescriptionTree(top, null);
		
		// main loop
		int loop = 0;
		while(!stop && !stoppingCriteriaSatisfied()) {
			// pick the best candidate according to the heuristic
			SearchTreeNode best = candidates.pollLast();
			// apply operator
			List<ELDescriptionTree> refinements = operator.refine(best.getDescriptionTree());
			// add all refinements to search tree, candidates, best descriptions
			for(ELDescriptionTree refinement : refinements) {
//				System.out.println("refinement: " + refinement);
				addDescriptionTree(refinement, best);
			}
			loop++;
			// logging
			if(logger.isTraceEnabled()) {
				logger.trace("Choosen node " + best);
				logger.trace(startNode.getTreeString());
				logger.trace("Loop " + loop + " completed.");
			}
		}
		
		// print solution(s)
		logger.info("solution : " + bestEvaluatedDescriptions.getBest());
		
		isRunning = false;
	}

	// evaluates a description in tree form
	private void addDescriptionTree(ELDescriptionTree descriptionTree, SearchTreeNode parentNode) {
		
		// create search tree node
		SearchTreeNode node = new SearchTreeNode(descriptionTree);
		
		// convert tree to standard description
		Description description = descriptionTree.transformToDescription();
		
//		double accuracy = getLearningProblem().getAccuracyOrTooWeak(description, 0);
		int negCovers = getLearningProblem().coveredNegativeExamplesOrTooWeak(description);
		if(negCovers == -1) {
//		if(accuracy == -1) {
			node.setTooWeak();
		} else {
			node.setCoveredNegatives(negCovers);
		}
		
		// link to parent (unless start node)
		if(parentNode == null) {
			startNode = node;
		} else {
			parentNode.addChild(node);
		}
		
//		System.out.println("TEST");
		
		if(!node.isTooWeak()) {
			// add as candidate
			candidates.add(node);
		
//			System.out.println("TEST2");
			
			// check whether we want to add it to the best evaluated descriptions;
			// to do this we pick the worst considered evaluated description
			// (remember that the set has limited size, so it's likely not the worst overall);
			// the description has a chance to make it in the set if it has
			// at least as high accuracy - if not we can save the reasoner calls
			// for fully computing the evaluated description
			if(bestEvaluatedDescriptions.size() == 0 || ((EvaluatedDescriptionPosNeg)bestEvaluatedDescriptions.getWorst()).getCoveredNegatives().size() >= node.getCoveredNegatives()) {
				ScorePosNeg score = (ScorePosNeg) learningProblem.computeScore(description);
				EvaluatedDescriptionPosNeg ed = new EvaluatedDescriptionPosNeg(description, score);
				bestEvaluatedDescriptions.add(ed);
			}
			
		}
		
	}
	
	private boolean stoppingCriteriaSatisfied() {
		// in some cases, there could be no candidate left ...
		if(candidates.isEmpty()) {
//			System.out.println("EMPTY");
			return true;
		}
		
		// stop when max time is reached
		long runTime = System.nanoTime() - treeStartTime;
		double runTimeSeconds = runTime / (double) 1000000000;
		
		if(runTimeSeconds >= treeSearchTimeSeconds) {
			return true;
		}
		
		// stop if we have a node covering all positives and none of the negatives
		SearchTreeNode bestNode = candidates.last();
		return (bestNode.getCoveredNegatives() == 0);
	}
	
	private void reset() {
		// set all values back to their default values (used for running
		// the algorithm more than once)
		candidates.clear();
		bestEvaluatedDescriptions.getSet().clear();
	}
	
	@Override
	public void stop() {
		stop = true;
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}	
	
	@Override
	public Description getCurrentlyBestDescription() {
		return getCurrentlyBestEvaluatedDescription().getDescription();
	}

	@Override
	public List<Description> getCurrentlyBestDescriptions() {
		return bestEvaluatedDescriptions.toDescriptionList();
	}
	
	@Override
	public EvaluatedDescription getCurrentlyBestEvaluatedDescription() {
		return bestEvaluatedDescriptions.getSet().last();
	}	
	
	@Override
	public TreeSet<? extends EvaluatedDescription> getCurrentlyBestEvaluatedDescriptions() {
		return bestEvaluatedDescriptions.getSet();
	}		
	
	/**
	 * @return the startNode
	 */
	public SearchTreeNode getStartNode() {
		return startNode;
	}	

}
