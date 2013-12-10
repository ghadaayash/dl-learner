/**
 * 
 */
package org.dllearner.algorithms.isle.index;

import com.google.common.collect.ComparisonChain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Lorenz Buehmann
 *
 */
public class Token implements Comparable<Token>, Serializable{
	
	private String rawForm;
	private String stemmedForm;
	private String posTag;
	private boolean isPunctuation;
	private boolean isStopWord;
	private boolean isHead;
    /// for storing alternative forms of this token, e.g., generated by WordNet synonyms
    private HashMap<String, Double> alternativeForms;

	
	public Token(String rawForm) {
		this.rawForm = rawForm;
	}
	
	public Token(String rawForm, String stemmedForm, String posTag, boolean isPunctuation, boolean isStopWord) {
		this.rawForm = rawForm;
		this.stemmedForm = stemmedForm;
		this.posTag = posTag;
		this.isPunctuation = isPunctuation;
		this.isStopWord = isStopWord;
        this.alternativeForms = new HashMap<>();
	}
	
	/**
	 * @return the rawForm
	 */
	public String getRawForm() {
		return rawForm;
	}
	
	/**
	 * @return the stemmedForm
	 */
	public String getStemmedForm() {
		return stemmedForm;
	}
	
	/**
	 * @return the posTag
	 */
	public String getPOSTag() {
		return posTag;
	}

    /**
     * Returns the unmodifiable list of alternative surface forms for this token. These alternative forms might be
     * generated by, e.g., WordNet synonym expansion.
     *
     * @return unmodifiable set of alternative surface forms for this token
     */
    public Set<String> getAlternativeForms() {
        return Collections.unmodifiableSet(alternativeForms.keySet());
    }

    /**
     * Returns the map storing the scored alternative forms of this token.
     */
    public Map<String, Double> getScoredAlternativeForms() {
        return Collections.unmodifiableMap(alternativeForms);
    }

    /**
     * Adds a new surface form to the alternative forms of this token. Alternative forms are included in comparison of
     * two tokens when using the {@link #equalsWithAlternativeForms}.
     */
    public void addAlternativeForm(String alternativeForm, Double score) {
        this.alternativeForms.put(alternativeForm, score);
    }

    /**
	 * @return the isPunctuation
	 */
	public boolean isPunctuation() {
		return isPunctuation;
	}
	
	/**
	 * @return the isStopWord
	 */
	public boolean isStopWord() {
		return isStopWord;
	}
	
	/**
	 * @param stemmedForm the stemmedForm to set
	 */
	public void setStemmedForm(String stemmedForm) {
		this.stemmedForm = stemmedForm;
	}
	
	/**
	 * @param posTag the posTag to set
	 */
	public void setPOSTag(String posTag) {
		this.posTag = posTag;
	}
	
	/**
	 * @param isPunctuation the isPunctuation to set
	 */
	public void setIsPunctuation(boolean isPunctuation) {
		this.isPunctuation = isPunctuation;
	}
	
	/**
	 * @param isStopWord the isStopWord to set
	 */
	public void setIsStopWord(boolean isStopWord) {
		this.isStopWord = isStopWord;
	}
	
	/**
	 * @param isHead the token is the head of the containg sequence of tokens
	 */
	public void setIsHead(boolean isHead) {
		this.isHead = isHead;
	}
	
	/**
	 * @return the isHead
	 */
	public boolean isHead() {
		return isHead;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[Word: " + rawForm + " | Stemmed word: " + stemmedForm + " | POS tag: " + posTag + " | Alternatives: " + alternativeForms.toString() + "]";
	}

    /**
     * Compares the given token to this one including alternative forms. This means that tokens are considered to be
     * equal iff the POS tags is the same and if the intersection of all surface forms (stemmed forms + alternative
     * forms) is not empty.
     *
     * @param other    token to compare this token to
     * @return true if tokens are equal considering alternative forms, otherwise false
     */
    public boolean equalsWithAlternativeForms(Token other) {
        if (this == other) {
            return true;
        }

        if (!posTag.equals(other.posTag)) {
            return false;
        }

        if (other.stemmedForm.equals(stemmedForm) || other.alternativeForms.containsKey(stemmedForm) ||
                alternativeForms.containsKey(other.stemmedForm)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Token token = (Token) o;

        if (!WordTypeComparator.sameWordType(posTag, token.posTag)) {
            return false;
        }
        if (!stemmedForm.equals(token.stemmedForm)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = stemmedForm.hashCode();
        result = 31 * result + WordTypeComparator.hashCode(posTag);
        return result;
    }

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Token other) {
		return ComparisonChain.start()
				.compare(this.rawForm, other.rawForm)
				.compare(this.posTag, other.posTag)
				.result();
	}
}
