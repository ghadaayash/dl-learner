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

package org.dllearner.tools.ore.ui.wizard.descriptors;

import javax.swing.event.ListSelectionEvent;

import org.dllearner.core.owl.NamedClass;
import org.dllearner.tools.ore.OREManager;
import org.dllearner.tools.ore.ui.wizard.WizardPanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.panels.ClassChoosePanel;



/**
 * Wizard panel descriptor for selecting one of the atomic classes in OWL-ontology that 
 * has to be (re)learned.
 * @author Lorenz Buehmann
 *
 */
public class ClassChoosePanelDescriptor extends WizardPanelDescriptor implements javax.swing.event.ListSelectionListener{
    
	/**
	 * Identification string for class choose panel.
	 */
    public static final String IDENTIFIER = "CLASS_CHOOSE_OWL_PANEL";
    /**
     * Information string for class choose panel.
     */
    public static final String INFORMATION = "In this panel all atomic classes in the ontology are shown in the list above. " 
    										 + "Select one of them which should be (re)learned from, then press \"Next-Button\"";
    
    private ClassChoosePanel owlClassPanel;
    
    /**
     * Constructor creates new panel and adds listener to list.
     */
    public ClassChoosePanelDescriptor() {
        owlClassPanel = new ClassChoosePanel();
        owlClassPanel.addSelectionListener(this);
             
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(owlClassPanel);
      
    }
    
    @Override
	public Object getNextPanelDescriptor() {
        return LearningPanelDescriptor.IDENTIFIER;
    }
    
    @Override
	public Object getBackPanelDescriptor() {
        return KnowledgeSourcePanelDescriptor.IDENTIFIER;
    }
    
    @Override
	public void aboutToDisplayPanel() {
    	getWizard().getInformationField().setText(INFORMATION);
        setNextButtonAccordingToConceptSelected();
    }
    
    /**
     * Method is called when other element in list is selected, and sets next button enabled.
     * @param e ListSelectionEvent
     */
	public void valueChanged(ListSelectionEvent e) {
		setNextButtonAccordingToConceptSelected(); 
		if (!e.getValueIsAdjusting() && owlClassPanel.getClassesTable().getSelectedRow() >= 0) {
			 OREManager.getInstance().setCurrentClass2Learn((NamedClass) owlClassPanel.getClassesTable().getSelectedValue());
		}
	}
	
	private void setNextButtonAccordingToConceptSelected() {
        
    	if (owlClassPanel.getClassesTable().getSelectedRow() >= 0){
    		getWizard().setNextFinishButtonEnabled(true);
    	}else{
    		getWizard().setNextFinishButtonEnabled(false);
    	}
   
    }
	
	/**
	 * Returns the JPanel with the GUI elements.
	 * @return extended JPanel
	 */
	public ClassChoosePanel getOwlClassPanel() {
		return owlClassPanel;
	}
	
	

	
    
   

    
    
}
