package org.dllearner.gui;

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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.*;

import org.dllearner.core.ReasonerComponent;

/**
 * ReasonerPanel
 * 
 * @author Tilo Hielscher
 * 
 */
public class ReasonerPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = -7678275020058043937L;

    private List<Class<? extends ReasonerComponent>> reasoners;
    private JPanel choosePanel = new JPanel();
    private JPanel initPanel = new JPanel();
    private JButton initButton;
    private Config config;
    private String[] cbItems = {};
    private JComboBox cb = new JComboBox(cbItems);
    private int choosenClassIndex;

    ReasonerPanel(final Config config) {
	super(new BorderLayout());

	this.config = config;

	initButton = new JButton("Init Reasoner");
	initButton.addActionListener(this);
	initPanel.add(initButton);

	choosePanel.add(cb);

	add(choosePanel, BorderLayout.PAGE_START);
	add(initPanel, BorderLayout.PAGE_END);

	// add into comboBox
	reasoners = config.getComponentManager().getReasonerComponents();
	for (int i = 0; i < reasoners.size(); i++) {
	    // cb.addItem(reasoners.get(i).getSimpleName());
	    cb.addItem(config.getComponentManager().getComponentName(
		    reasoners.get(i)));
	}

    }

    public void actionPerformed(ActionEvent e) {
	// read selected Class
	choosenClassIndex = cb.getSelectedIndex();

	if (e.getSource() == initButton && config.getKnowledgeSource() != null) {
	    // set reasoner
	    config.setReasoner(config.getComponentManager().reasoner(
		    reasoners.get(choosenClassIndex),
		    config.getKnowledgeSource()));
	    config.getReasoner().init();

	    // set ReasoningService
	    config.setReasoningService(config.getComponentManager()
		    .reasoningService(config.getReasoner()));
	}
    }
}
