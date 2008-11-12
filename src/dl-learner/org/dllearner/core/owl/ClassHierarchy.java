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
package org.dllearner.core.owl;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.dllearner.utilities.owl.ConceptComparator;

/**
 * Represents a subsumption hierarchy (ignoring equivalent concepts).
 * 
 * @author Jens Lehmann
 * 
 */
public class ClassHierarchy {

	public static Logger logger = Logger.getLogger(ClassHierarchy.class);
	
	ConceptComparator conceptComparator = new ConceptComparator();
	TreeMap<Description, SortedSet<Description>> subsumptionHierarchyUp;
	TreeMap<Description, SortedSet<Description>> subsumptionHierarchyDown;
	
	/**
	 * The arguments specify the superclasses and subclasses of each class. This
	 * is used to build the subsumption hierarchy. 
	 * @param subsumptionHierarchyUp Contains super classes for each class.
	 * @param subsumptionHierarchyDown Contains sub classes for each class.
	 */
	public ClassHierarchy(
			TreeMap<Description, SortedSet<Description>> subsumptionHierarchyUp,
			TreeMap<Description, SortedSet<Description>> subsumptionHierarchyDown) {
		
		this.subsumptionHierarchyUp = subsumptionHierarchyUp;
		this.subsumptionHierarchyDown = subsumptionHierarchyDown;
		
	}

	public SortedSet<Description> getSuperClasses(Description concept) {
		SortedSet<Description> result =  subsumptionHierarchyUp.get(concept);
		if(result == null) {
			logger.error("Query for super class of " + concept + " in subsumption hierarchy, but the class is not contained in the (upward) hierarchy");
		}
		
		// we copy all concepts before returning them such that they cannot be
		// modified externally
		return new TreeSet<Description>(result);
	}

	public SortedSet<Description> getSubClasses(Description concept) {
		SortedSet<Description> result =  subsumptionHierarchyDown.get(concept);
		if(result == null) {
			logger.error("Query for sub class of " + concept + " in subsumption hierarchy, but the class is not contained in the (downward) hierarchy");
		}
		
		return new TreeSet<Description>(result);		
		
		// commented out, because these hacks just worked around a problem
//		if (subsumptionHierarchyDown == null) {
//			return new TreeSet<Description>();
//		} else if (subsumptionHierarchyDown.get(concept) == null) {
//			return new TreeSet<Description>();
//		} else {
//			return (TreeSet<Description>) subsumptionHierarchyDown.get(concept).clone();
//		}
	}

	/**
	 * This method modifies the subsumption hierarchy such that for each class,
	 * there is only a single path to reach it via upward and downward
	 * refinement respectively.
	 */
	public void thinOutSubsumptionHierarchy() {
		TreeMap<Description, SortedSet<Description>> hierarchyDownNew = new TreeMap<Description, SortedSet<Description>>(
				conceptComparator);
		TreeMap<Description, SortedSet<Description>> hierarchyUpNew = new TreeMap<Description, SortedSet<Description>>(
				conceptComparator);

		Set<Description> conceptsInSubsumptionHierarchy = new TreeSet<Description>(conceptComparator);
		conceptsInSubsumptionHierarchy.addAll(subsumptionHierarchyUp.keySet());
		conceptsInSubsumptionHierarchy.addAll(subsumptionHierarchyDown.keySet());
		
		// add empty sets for each concept
		for (Description c : conceptsInSubsumptionHierarchy) {
			hierarchyDownNew.put(c, new TreeSet<Description>(conceptComparator));
			hierarchyUpNew.put(c, new TreeSet<Description>(conceptComparator));
		}

		for (Description c : conceptsInSubsumptionHierarchy) {
			// look whether there are more general concepts
			// (if yes, pick the first one)
			SortedSet<Description> moreGeneral = subsumptionHierarchyUp.get(c);
			if (moreGeneral != null && moreGeneral.size() != 0) {
				Description chosenParent = moreGeneral.first();
				hierarchyDownNew.get(chosenParent).add(c);
			}
		}

		for (Description c : conceptsInSubsumptionHierarchy) {
			SortedSet<Description> moreSpecial = subsumptionHierarchyDown.get(c);
			if (moreSpecial != null && moreSpecial.size() != 0) {
				Description chosenParent = moreSpecial.first();
				hierarchyUpNew.get(chosenParent).add(c);
			}
		}

		subsumptionHierarchyDown = hierarchyDownNew;
		subsumptionHierarchyUp = hierarchyUpNew;
	}

	/**
	 * Implements a subsumption check using the hierarchy (no further reasoning
	 * checks are used).
	 * 
	 * @param subClass
	 *            The (supposedly) more special class.
	 * @param superClass
	 *            The (supposedly) more general class.
	 * @return True if <code>subClass</code> is a subclass of
	 *         <code>superclass</code>.
	 */
	public boolean isSubclassOf(NamedClass subClass, NamedClass superClass) {
		if (subClass.equals(superClass)) {
			return true;
		} else {
			for (Description moreGeneralClass : subsumptionHierarchyUp.get(subClass)) {
				// search the upper classes of the subclass
				if (moreGeneralClass instanceof NamedClass) {
					if (isSubclassOf((NamedClass) moreGeneralClass, superClass)) {
						return true;
					}
					// we reached top, so we can return false (if top is a
					// direct upper
					// class, then no other upper classes can exist)
				} else {
					return false;
				}
			}
			// we cannot reach the class via any of the upper classes,
			// so it is not a super class
			return false;
		}
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean showUpwardHierarchy) {
		if (showUpwardHierarchy) {
			String str = "downward subsumption:\n";
			str += toString(subsumptionHierarchyDown, new Thing(), 0);
			str += "upward subsumption:\n";
			str += toString(subsumptionHierarchyUp, new Nothing(), 0);
			return str;
		} else {
			return toString(subsumptionHierarchyDown, new Thing(), 0);
		}
	}

	private String toString(TreeMap<Description, SortedSet<Description>> hierarchy,
			Description concept, int depth) {
		String str = "";
		for (int i = 0; i < depth; i++)
			str += "  ";
		str += concept.toString() + "\n";
		Set<Description> tmp = hierarchy.get(concept);
		if (tmp != null) {
			for (Description c : tmp)
				str += toString(hierarchy, c, depth + 1);
		}
		return str;
	}

	/**
	 * The method computes a new class hierarchy, which is a copy of this
	 * one, but only the specified classes are allowed to occur. For instance,
	 * if we have subclass relationships between 1sYearStudent, Student, and
	 * Person, but Student is not allowed, then there a is a subclass relationship
	 * between 1stYearStudent and Person.
	 * Currently, owl:Thing and owl:Nothing are always allowed for technical
	 * reasons.
	 * @param allowedClasses The classes, which are allowed to occur in the new
	 * class hierarchy.
	 * @return A copy of this hierarchy, which is restricted to a certain set
	 * of classes.
	 */
	public ClassHierarchy cloneAndRestrict(Set<NamedClass> allowedClasses) {
		// currently TOP and BOTTOM are always allowed
		// (TODO would be easier if Thing/Nothing were declared as named classes)
		Set<Description> allowed = new TreeSet<Description>(conceptComparator);
		allowed.addAll(allowedClasses);
		allowed.add(Thing.instance);
		allowed.add(Nothing.instance);
		
		// create new maps
		TreeMap<Description, SortedSet<Description>> subsumptionHierarchyUpNew
		= new TreeMap<Description, SortedSet<Description>>(conceptComparator);
		TreeMap<Description, SortedSet<Description>> subsumptionHierarchyDownNew 
		= new TreeMap<Description, SortedSet<Description>>(conceptComparator);
		
		for(Entry<Description, SortedSet<Description>> entry : subsumptionHierarchyUp.entrySet()) {
			Description key = entry.getKey();
			// we only store mappings for allowed classes
			if(allowed.contains(key)) {
				// copy the set of all super classes (we consume them until
				// they are empty)
				TreeSet<Description> superClasses = new TreeSet<Description>(entry.getValue());
				// storage for new super classes
				TreeSet<Description> newSuperClasses = new TreeSet<Description>(entry.getValue());
				
				while(!superClasses.isEmpty()) {
					// pick and remove the first element
					Description d = superClasses.pollFirst();
					// case 1: it is allowed, so we add it
					if(allowed.contains(d)) {
						newSuperClasses.add(d);
					// case 2: it is not allowed, so we try its super classes
					} else {
						superClasses.addAll(subsumptionHierarchyUp.get(d));
					}
				}
				
				subsumptionHierarchyUpNew.put(key, newSuperClasses);
			}
		}
		
		// downward case is analogous
		for(Entry<Description, SortedSet<Description>> entry : subsumptionHierarchyDown.entrySet()) {
			Description key = entry.getKey();
			if(allowed.contains(key)) {
				TreeSet<Description> subClasses = new TreeSet<Description>(entry.getValue());
				TreeSet<Description> newSubClasses = new TreeSet<Description>(entry.getValue());
				
				while(!subClasses.isEmpty()) {
					Description d = subClasses.pollFirst();
					if(allowed.contains(d)) {
						newSubClasses.add(d);
					} else {
						subClasses.addAll(subsumptionHierarchyDown.get(d));
					}
				}
				
				subsumptionHierarchyDownNew.put(key, newSubClasses);
			}
		}		
		
		return new ClassHierarchy(subsumptionHierarchyUpNew, subsumptionHierarchyDownNew);
	}
}
