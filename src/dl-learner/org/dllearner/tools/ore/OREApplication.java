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

package org.dllearner.tools.ore;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import org.apache.log4j.PropertyConfigurator;
import org.dllearner.tools.ore.ui.wizard.Wizard;
import org.dllearner.tools.ore.ui.wizard.WizardPanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.AutoLearnPanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.ClassChoosePanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.IntroductionPanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.KnowledgeSourcePanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.ManualLearnPanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.RepairPanelDescriptor;
import org.dllearner.tools.ore.ui.wizard.descriptors.SavePanelDescriptor;


/**
 * Main class starting the wizard and registering wizard panels.
 * @author Lorenz Buehmann
 *
 */
public class OREApplication {
	
    
	/**
	 * main method.
	 * @param args possible is to use URI as parameter
	 */
    public static void main(String[] args) {
    	try {
			PropertyConfigurator.configure(new URL("file:src/dl-learner/org/dllearner/tools/ore/log4j.properties"));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
    	
    	
    	try {
//    		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			UIDefaults def = UIManager.getLookAndFeelDefaults();
			Vector<?> vec = new Vector<Object>(def.keySet());
			Collections.sort(vec, new Comparator<Object>() {

			  public int compare(Object arg0, Object arg1) {
			    return arg0.toString().compareTo(arg1.toString());
			  }

			});

			for (Object obj : vec) {
				if(obj.toString().endsWith(".font")){
					FontUIResource fur = (FontUIResource)UIManager.get(obj);
					Font f = new Font("Verdana", Font.PLAIN, fur.getSize());
					UIManager.put(obj, new FontUIResource(f));
				}
			}
			
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Locale.setDefault(Locale.ENGLISH);
        final Wizard wizard = new Wizard();
        wizard.getDialog().setTitle("DL-Learner Ontology Repair and Enrichment");
        Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        wizard.getDialog().setSize(dim);
        
        WizardPanelDescriptor descriptor1 = new IntroductionPanelDescriptor();
        wizard.registerWizardPanel(IntroductionPanelDescriptor.IDENTIFIER, descriptor1);

        WizardPanelDescriptor descriptor2 = new KnowledgeSourcePanelDescriptor();
        wizard.registerWizardPanel(KnowledgeSourcePanelDescriptor.IDENTIFIER, descriptor2);
        
//        WizardPanelDescriptor descriptor3 = new InconsistencyExplanationPanelDescriptor();
//        wizard.registerWizardPanel(InconsistencyExplanationPanelDescriptor.IDENTIFIER, descriptor3);
//        
//        WizardPanelDescriptor descriptor4 = new UnsatisfiableExplanationPanelDescriptor();
//        wizard.registerWizardPanel(UnsatisfiableExplanationPanelDescriptor.IDENTIFIER, descriptor4);

        WizardPanelDescriptor descriptor5 = new ClassChoosePanelDescriptor();
        wizard.registerWizardPanel(ClassChoosePanelDescriptor.IDENTIFIER, descriptor5);
        
        WizardPanelDescriptor descriptor9 = new AutoLearnPanelDescriptor();
        wizard.registerWizardPanel(AutoLearnPanelDescriptor.IDENTIFIER, descriptor9);
        
        WizardPanelDescriptor descriptor6 = new ManualLearnPanelDescriptor();
        wizard.registerWizardPanel(ManualLearnPanelDescriptor.IDENTIFIER, descriptor6);
        
        WizardPanelDescriptor descriptor7 = new RepairPanelDescriptor();
        wizard.registerWizardPanel(RepairPanelDescriptor.IDENTIFIER, descriptor7);
        
        WizardPanelDescriptor descriptor8 = new SavePanelDescriptor();
        wizard.registerWizardPanel(SavePanelDescriptor.IDENTIFIER, descriptor8);
        
        if (!(args.length == 1)){
        	 wizard.setCurrentPanel(IntroductionPanelDescriptor.IDENTIFIER);
        } else{
        	wizard.setCurrentPanel(KnowledgeSourcePanelDescriptor.IDENTIFIER);
        	wizard.setLeftPanel(1);	
        	((KnowledgeSourcePanelDescriptor)wizard.getModel().getPanelHashMap().get(KnowledgeSourcePanelDescriptor.IDENTIFIER)).loadOntology(new File(args[0]).toURI());
        	
        }
			    
        SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				wizard.showModalDialog(); 
			}
		});
        
    }
    
}