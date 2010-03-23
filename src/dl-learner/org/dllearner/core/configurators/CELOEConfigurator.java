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

package org.dllearner.core.configurators;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentManager;
import org.dllearner.core.LearningProblem;
import org.dllearner.core.LearningProblemUnsupportedException;
import org.dllearner.core.ReasonerComponent;
import org.dllearner.core.configurators.RefinementOperatorConfigurator;

/**
* automatically generated, do not edit manually.
* run org.dllearner.scripts.ConfigJavaGenerator to update
**/
@SuppressWarnings("all")
public  class CELOEConfigurator  extends RefinementOperatorConfigurator implements Configurator {

private boolean reinitNecessary = false;
@SuppressWarnings("unused")

private CELOE cELOE;

/**
* @param cELOE see CELOE
**/
public CELOEConfigurator(CELOE cELOE){
this.cELOE = cELOE;
}

/**
* @param reasoningService see reasoningService
* @param learningProblem see learningProblem
* @throws LearningProblemUnsupportedException see 
* @return CELOE
**/
public static CELOE getCELOE(LearningProblem learningProblem, ReasonerComponent reasoningService) throws LearningProblemUnsupportedException{
CELOE component = ComponentManager.getInstance().learningAlgorithm(CELOE.class, learningProblem, reasoningService);
return component;
}

/**
* useAllConstructor specifies whether the universal concept constructor is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: true
* @return boolean 
**/
public boolean getUseAllConstructor() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useAllConstructor") ;
}
/**
* useExistsConstructor specifies whether the existential concept constructor is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: true
* @return boolean 
**/
public boolean getUseExistsConstructor() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useExistsConstructor") ;
}
/**
* useHasValueConstructor specifies whether the hasValue constructor is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: false
* @return boolean 
**/
public boolean getUseHasValueConstructor() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useHasValueConstructor") ;
}
/**
* useDataHasValueConstructor specifies whether the hasValue constructor is used in the learning algorithm in combination with data properties.
* mandatory: false| reinit necessary: true
* default value: false
* @return boolean 
**/
public boolean getUseDataHasValueConstructor() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useDataHasValueConstructor") ;
}
/**
* valueFrequencyThreshold specifies how often an object must occur as value in order to be considered for hasValue restrictions.
* mandatory: false| reinit necessary: true
* default value: 3
* @return int 
**/
public int getValueFrequencyThreshold() {
return (Integer) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "valueFrequencyThreshold") ;
}
/**
* useCardinalityRestrictions specifies whether CardinalityRestrictions is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: true
* @return boolean 
**/
public boolean getUseCardinalityRestrictions() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useCardinalityRestrictions") ;
}
/**
* cardinalityLimit Gives the maximum number used in cardinality restrictions..
* mandatory: false| reinit necessary: true
* default value: 5
* @return int 
**/
public int getCardinalityLimit() {
return (Integer) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "cardinalityLimit") ;
}
/**
* useNegation specifies whether negation is used in the learning algorothm.
* mandatory: false| reinit necessary: true
* default value: false
* @return boolean 
**/
public boolean getUseNegation() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useNegation") ;
}
/**
* useBooleanDatatypes specifies whether boolean datatypes are used in the learning algorothm.
* mandatory: false| reinit necessary: true
* default value: true
* @return boolean 
**/
public boolean getUseBooleanDatatypes() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useBooleanDatatypes") ;
}
/**
* useDoubleDatatypes specifies whether boolean datatypes are used in the learning algorothm.
* mandatory: false| reinit necessary: true
* default value: true
* @return boolean 
**/
public boolean getUseDoubleDatatypes() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "useDoubleDatatypes") ;
}
/**
* maxExecutionTimeInSeconds algorithm will stop after specified seconds.
* mandatory: false| reinit necessary: true
* default value: 10
* @return int 
**/
public int getMaxExecutionTimeInSeconds() {
return (Integer) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "maxExecutionTimeInSeconds") ;
}
/**
* noisePercentage the (approximated) percentage of noise within the examples.
* mandatory: false| reinit necessary: true
* default value: 0.0
* @return double 
**/
public double getNoisePercentage() {
return (Double) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "noisePercentage") ;
}
/**
* maxDepth maximum depth of description.
* mandatory: false| reinit necessary: true
* default value: 7
* @return int 
**/
public int getMaxDepth() {
return (Integer) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "maxDepth") ;
}
/**
* maxNrOfResults Sets the maximum number of results one is interested in. (Setting this to a lower value may increase performance as the learning algorithm has to store/evaluate/beautify less descriptions)..
* mandatory: false| reinit necessary: true
* default value: 10
* @return int 
**/
public int getMaxNrOfResults() {
return (Integer) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "maxNrOfResults") ;
}
/**
* singleSuggestionMode Use this if you are interested in only one suggestion and your learning problem has many (more than 1000) examples..
* mandatory: false| reinit necessary: true
* default value: false
* @return boolean 
**/
public boolean getSingleSuggestionMode() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "singleSuggestionMode") ;
}
/**
* instanceBasedDisjoints Specifies whether to use real disjointness checks or instance based ones (no common instances) in the refinement operator..
* mandatory: false| reinit necessary: true
* default value: true
* @return boolean 
**/
public boolean getInstanceBasedDisjoints() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "instanceBasedDisjoints") ;
}
/**
* filterDescriptionsFollowingFromKB If true, then the results will not contain suggestions, which already follow logically from the knowledge base. Be careful, since this requires a potentially expensive consistency check for candidate solutions..
* mandatory: false| reinit necessary: true
* default value: false
* @return boolean 
**/
public boolean getFilterDescriptionsFollowingFromKB() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "filterDescriptionsFollowingFromKB") ;
}
/**
* reuseExistingDescription If true, the algorithm tries to find a good starting point close to an existing definition/super class of the given class in the knowledge base..
* mandatory: false| reinit necessary: true
* default value: false
* @return boolean 
**/
public boolean getReuseExistingDescription() {
return (Boolean) ComponentManager.getInstance().getConfigOptionValue(cELOE,  "reuseExistingDescription") ;
}

/**
* @param useAllConstructor specifies whether the universal concept constructor is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: true
**/
public void setUseAllConstructor(boolean useAllConstructor) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useAllConstructor", useAllConstructor);
reinitNecessary = true;
}
/**
* @param useExistsConstructor specifies whether the existential concept constructor is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: true
**/
public void setUseExistsConstructor(boolean useExistsConstructor) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useExistsConstructor", useExistsConstructor);
reinitNecessary = true;
}
/**
* @param useHasValueConstructor specifies whether the hasValue constructor is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: false
**/
public void setUseHasValueConstructor(boolean useHasValueConstructor) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useHasValueConstructor", useHasValueConstructor);
reinitNecessary = true;
}
/**
* @param useDataHasValueConstructor specifies whether the hasValue constructor is used in the learning algorithm in combination with data properties.
* mandatory: false| reinit necessary: true
* default value: false
**/
public void setUseDataHasValueConstructor(boolean useDataHasValueConstructor) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useDataHasValueConstructor", useDataHasValueConstructor);
reinitNecessary = true;
}
/**
* @param valueFrequencyThreshold specifies how often an object must occur as value in order to be considered for hasValue restrictions.
* mandatory: false| reinit necessary: true
* default value: 3
**/
public void setValueFrequencyThreshold(int valueFrequencyThreshold) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "valueFrequencyThreshold", valueFrequencyThreshold);
reinitNecessary = true;
}
/**
* @param useCardinalityRestrictions specifies whether CardinalityRestrictions is used in the learning algorithm.
* mandatory: false| reinit necessary: true
* default value: true
**/
public void setUseCardinalityRestrictions(boolean useCardinalityRestrictions) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useCardinalityRestrictions", useCardinalityRestrictions);
reinitNecessary = true;
}
/**
* @param cardinalityLimit Gives the maximum number used in cardinality restrictions..
* mandatory: false| reinit necessary: true
* default value: 5
**/
public void setCardinalityLimit(int cardinalityLimit) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "cardinalityLimit", cardinalityLimit);
reinitNecessary = true;
}
/**
* @param useNegation specifies whether negation is used in the learning algorothm.
* mandatory: false| reinit necessary: true
* default value: false
**/
public void setUseNegation(boolean useNegation) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useNegation", useNegation);
reinitNecessary = true;
}
/**
* @param useBooleanDatatypes specifies whether boolean datatypes are used in the learning algorothm.
* mandatory: false| reinit necessary: true
* default value: true
**/
public void setUseBooleanDatatypes(boolean useBooleanDatatypes) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useBooleanDatatypes", useBooleanDatatypes);
reinitNecessary = true;
}
/**
* @param useDoubleDatatypes specifies whether boolean datatypes are used in the learning algorothm.
* mandatory: false| reinit necessary: true
* default value: true
**/
public void setUseDoubleDatatypes(boolean useDoubleDatatypes) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "useDoubleDatatypes", useDoubleDatatypes);
reinitNecessary = true;
}
/**
* @param maxExecutionTimeInSeconds algorithm will stop after specified seconds.
* mandatory: false| reinit necessary: true
* default value: 10
**/
public void setMaxExecutionTimeInSeconds(int maxExecutionTimeInSeconds) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "maxExecutionTimeInSeconds", maxExecutionTimeInSeconds);
reinitNecessary = true;
}
/**
* @param noisePercentage the (approximated) percentage of noise within the examples.
* mandatory: false| reinit necessary: true
* default value: 0.0
**/
public void setNoisePercentage(double noisePercentage) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "noisePercentage", noisePercentage);
reinitNecessary = true;
}
/**
* @param maxDepth maximum depth of description.
* mandatory: false| reinit necessary: true
* default value: 7
**/
public void setMaxDepth(int maxDepth) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "maxDepth", maxDepth);
reinitNecessary = true;
}
/**
* @param maxNrOfResults Sets the maximum number of results one is interested in. (Setting this to a lower value may increase performance as the learning algorithm has to store/evaluate/beautify less descriptions)..
* mandatory: false| reinit necessary: true
* default value: 10
**/
public void setMaxNrOfResults(int maxNrOfResults) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "maxNrOfResults", maxNrOfResults);
reinitNecessary = true;
}
/**
* @param singleSuggestionMode Use this if you are interested in only one suggestion and your learning problem has many (more than 1000) examples..
* mandatory: false| reinit necessary: true
* default value: false
**/
public void setSingleSuggestionMode(boolean singleSuggestionMode) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "singleSuggestionMode", singleSuggestionMode);
reinitNecessary = true;
}
/**
* @param instanceBasedDisjoints Specifies whether to use real disjointness checks or instance based ones (no common instances) in the refinement operator..
* mandatory: false| reinit necessary: true
* default value: true
**/
public void setInstanceBasedDisjoints(boolean instanceBasedDisjoints) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "instanceBasedDisjoints", instanceBasedDisjoints);
reinitNecessary = true;
}
/**
* @param filterDescriptionsFollowingFromKB If true, then the results will not contain suggestions, which already follow logically from the knowledge base. Be careful, since this requires a potentially expensive consistency check for candidate solutions..
* mandatory: false| reinit necessary: true
* default value: false
**/
public void setFilterDescriptionsFollowingFromKB(boolean filterDescriptionsFollowingFromKB) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "filterDescriptionsFollowingFromKB", filterDescriptionsFollowingFromKB);
reinitNecessary = true;
}
/**
* @param reuseExistingDescription If true, the algorithm tries to find a good starting point close to an existing definition/super class of the given class in the knowledge base..
* mandatory: false| reinit necessary: true
* default value: false
**/
public void setReuseExistingDescription(boolean reuseExistingDescription) {
ComponentManager.getInstance().applyConfigEntry(cELOE, "reuseExistingDescription", reuseExistingDescription);
reinitNecessary = true;
}

/**
* true, if this component needs reinitializsation.
* @return boolean
**/
public boolean isReinitNecessary(){
return reinitNecessary;
}


}
