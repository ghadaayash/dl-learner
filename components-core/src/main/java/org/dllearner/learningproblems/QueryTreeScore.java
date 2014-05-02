/**
 * 
 */
package org.dllearner.learningproblems;

import java.util.Set;

import org.dllearner.core.Score;
import org.dllearner.core.owl.Individual;

/**
 * @author Lorenz Buehmann
 *
 */
public class QueryTreeScore extends Score {
	
	private double score;
	
	private double coverageScore;
	
	private double specifityScore;
	private int nrOfSpecificNodes;
	
	private Set<Individual> posAsPos;	
    private Set<Individual> posAsNeg;
    private Set<Individual> negAsPos;
    private Set<Individual> negAsNeg;    

	public QueryTreeScore(double score, double coverageScore, 
			Set<Individual> posAsPos, Set<Individual> posAsNeg, Set<Individual> negAsPos, Set<Individual> negAsNeg,
			double specifityScore, int nrOfSpecificNodes) {
		super();
		this.score = score;
		this.coverageScore = coverageScore;
		this.posAsPos = posAsPos;
		this.posAsNeg = posAsNeg;
		this.negAsPos = negAsPos;
		this.negAsNeg = negAsNeg;
		this.specifityScore = specifityScore;
		this.nrOfSpecificNodes = nrOfSpecificNodes;
	}
	
	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.Score#getAccuracy()
	 */
	@Override
	public double getAccuracy() {
		return score;
	}
	
	public Set<Individual> getCoveredNegatives() {
		return negAsPos;
	}

	public Set<Individual> getCoveredPositives() {
		return posAsPos;
	}
	
	public Set<Individual> getNotCoveredPositives() {
		return posAsNeg;
	}
	
	public Set<Individual> getNotCoveredNegatives() {
		return negAsNeg;
	}		
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return score
				 + "(coverage=" + coverageScore 
				 + "(+" + posAsPos.size() + "/" + (posAsPos.size() + posAsNeg.size())
				 + "|-" + negAsPos.size() + "/" + (negAsPos.size() + negAsNeg.size()) + ")|"
				 + "specifity=" + specifityScore + "(" + nrOfSpecificNodes + "))";   
	}

}
