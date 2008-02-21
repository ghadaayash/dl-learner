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
import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import org.dllearner.core.ComponentInitException;
import org.dllearner.core.LearningAlgorithm;
import org.dllearner.core.LearningProblemUnsupportedException;

/**
 * LearningAlgorithmPanel, tab 3. Choose LearningAlgorithm, change Options and
 * final initiate LearningAlgorithm.
 * 
 * @author Tilo Hielscher
 */
public class LearningAlgorithmPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 8721490771860452959L;

	private Config config;
	private StartGUI startGUI;
	private List<Class<? extends LearningAlgorithm>> learner;
	private JPanel choosePanel = new JPanel();
	private OptionPanel optionPanel;
	private JPanel initPanel = new JPanel();
	private JButton initButton, autoInitButton;
	private String[] cbItems = {};
	private JComboBox cb = new JComboBox(cbItems);
	private int choosenClassIndex;

	LearningAlgorithmPanel(Config config, StartGUI startGUI) {
		super(new BorderLayout());

		this.config = config;
		this.startGUI = startGUI;
		learner = config.getComponentManager().getLearningAlgorithms();

		initButton = new JButton("Init LearingAlgorithm");
		initButton.addActionListener(this);
		initPanel.add(initButton);
		initButton.setEnabled(true);
		autoInitButton = new JButton("Set");
		autoInitButton.addActionListener(this);

		// add into comboBox
		for (int i = 0; i < learner.size(); i++) {
			cb.addItem(config.getComponentManager().getComponentName(learner.get(i)));
		}

		choosePanel.add(cb);
		choosePanel.add(autoInitButton);
		cb.addActionListener(this);

		optionPanel = new OptionPanel(config, config.getLearningAlgorithm(), config
				.getOldLearningAlgorithm(), learner.get(choosenClassIndex));

		add(choosePanel, BorderLayout.PAGE_START);
		add(optionPanel, BorderLayout.CENTER);
		add(initPanel, BorderLayout.PAGE_END);

		updateInitButtonColor();
	}

	public void actionPerformed(ActionEvent e) {
		// read selected Class
		// choosenClassIndex = cb.getSelectedIndex();
		if (choosenClassIndex != cb.getSelectedIndex()) {
			choosenClassIndex = cb.getSelectedIndex();
			config.setInitLearningAlgorithm(false);
			setLearningAlgorithm();
		}

		if (e.getSource() == autoInitButton)
			setLearningAlgorithm();

		if (e.getSource() == initButton)
			init();
	}

	/**
	 * after this, you can change widgets
	 */
	public void setLearningAlgorithm() {
		if (config.getLearningProblem() != null && config.getReasoningService() != null) {
			try {
				config.setLearningAlgorithm(config.getComponentManager().learningAlgorithm(
						learner.get(choosenClassIndex), config.getLearningProblem(),
						config.getReasoningService()));
				updateOptionPanel();
			} catch (LearningProblemUnsupportedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * after this, next tab can be used
	 */
	public void init() {
		setLearningAlgorithm();
		if (config.getLearningProblem() != null) {
			try {
				config.getLearningAlgorithm().init();
			} catch (ComponentInitException e) {
				e.printStackTrace();
			}
			config.setInitLearningAlgorithm(true);
			System.out.println("init LearningAlgorithm");
			startGUI.updateTabColors();
		}
	}

	/**
	 * updateAll
	 */
	public void updateAll() {
		updateComboBox();
		updateOptionPanel();
		updateInitButtonColor();
	}

	/**
	 * set ComboBox to selected class
	 */
	public void updateComboBox() {
		if (config.getLearningAlgorithm() != null)
			for (int i = 0; i < learner.size(); i++)
				if (config.getLearningAlgorithm().getClass().equals(
						config.getComponentManager().getLearningAlgorithms().get(i))) {
					cb.setSelectedIndex(i);
				}
		this.choosenClassIndex = cb.getSelectedIndex();
	}

	/**
	 * update OptionPanel with new selection
	 */
	public void updateOptionPanel() {
		// update OptionPanel
		optionPanel.update(config.getLearningAlgorithm(), config.getOldLearningAlgorithm(), learner
				.get(choosenClassIndex));
	}

	/**
	 * make init-button red if you have to click
	 */
	public void updateInitButtonColor() {
		if (!config.isInitLearningAlgorithm()) {
			initButton.setForeground(Color.RED);
		} else
			initButton.setForeground(Color.BLACK);
	}
}
