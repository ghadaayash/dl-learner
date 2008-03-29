package org.dllearner.tools.ore;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class LearningPanel extends JPanel{



	private javax.swing.JList conceptList;
	
	private JPanel contentPanel;
	
	private DefaultListModel model;
	private JLabel result;
	private JButton startButton;
	private JButton stopButton;
	
	public LearningPanel() {
		
		super();
		model = new DefaultListModel();
		result = new JLabel();
		JPanel buttonPanel = new JPanel();
		startButton = new JButton("Start");
		
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		
		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);
		contentPanel = getContentPanel();
		setLayout(new java.awt.BorderLayout());
		
		add(buttonPanel, BorderLayout.EAST);
		add(contentPanel,BorderLayout.CENTER);
		add(result, BorderLayout.SOUTH);
	}

	private JPanel getContentPanel() {
		
		JPanel contentPanel1 = new JPanel();
		JScrollPane scroll = new JScrollPane();
		
			
		conceptList = new JList(model);
		scroll.setSize(100,100);
		scroll.setViewportView(conceptList);
				
		contentPanel1.add(scroll);
		
		

		return contentPanel1;
	}
	
	public void addStartButtonListener(ActionListener a){
		startButton.addActionListener(a);
	}
	
	public void addStopButtonListener(ActionListener a){
		stopButton.addActionListener(a);
	}
	
	public void setResult(String resultStr){
		result.setText(resultStr);
	}

	public JButton getStartButton() {
		return startButton;
	}

	public JButton getStopButton() {
		return stopButton;
	}
	
	
	
	
	
}  
    
 


	

