package org.dllearner.tools.protege;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Ellipse2D;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.owl.Individual;

public class GraphicalCoveragePanel extends JPanel {

	private static final long serialVersionUID = 855436961912515267L;
	private static final int height =250;
	private static final int width = 250;
	private static final int maxNumberOfIndividualPoints = 20;
	private static final int gap = 20;
	private int shiftXAxis;
	private int distortion;
	private Ellipse2D oldConcept;
	private Ellipse2D newConcept;

	private EvaluatedDescription eval;
	private DLLearnerModel model;
	private String conceptNew;
	private Vector<IndividualPoint> posCovIndVector;
	private Vector<IndividualPoint> posNotCovIndVector;
	private Vector<IndividualPoint> negCovIndVector;
	private JButton allPositiveIndividuals;
	private JButton allNegativeIndividuals;

	/**
	 * This is the constructor for the GraphicalCoveragePanel.
	 */
	public GraphicalCoveragePanel(EvaluatedDescription desc, DLLearnerModel m, String concept, int w, int h) {
		setPreferredSize(new Dimension(width, height));
		setVisible(true);
		setForeground(Color.GREEN);
		repaint();
		eval = desc;
		model = m;
		conceptNew = concept;
		posCovIndVector = new Vector<IndividualPoint>();
		posNotCovIndVector = new Vector<IndividualPoint>();
		negCovIndVector = new Vector<IndividualPoint>();
		this.computeGraphics();
		oldConcept = new Ellipse2D.Float(5, 25, 250, 250);
		newConcept = new Ellipse2D .Float(5+shiftXAxis, 25, width+distortion, height+distortion);
		allPositiveIndividuals = new JButton();
		//immer in der mitte und unten rauszubekommen mittels getMittelpunkt fuer x 
		//und getMaxY fuer y.
		//allPositiveIndividuals.setBounds(arg0, arg1, arg2, arg3);
		allNegativeIndividuals = new JButton();
		//allNegativeIndividuals.setBounds(arg0, arg1, arg2, arg3);
		this.computeIndividualPoints();
	}

	public void drawCoverageForLearnedClassDescription(
			Set<Individual> posCovInd, Set<Individual> posNotCovInd,
			Set<Individual> negCovInd) {
		

	}
	
	protected void paintComponent(Graphics g) {
		g.setColor(Color.RED);
		g.drawOval((5+shiftXAxis), 25, width+distortion, height+distortion);
		g.drawString(conceptNew, 10 + width, 15);
		g.setColor(Color.GREEN);
		g.drawOval(5, 25, 250, 250);
		g.drawString(model.getOldConceptOWLAPI().toString(), 10, 15);
		
		for(int i = 0; i < posCovIndVector.size(); i++) {
			g.setColor(Color.BLACK);
			g.drawString(posCovIndVector.get(i).getPoint(), posCovIndVector.get(i).getXAxis(), posCovIndVector.get(i).getYAxis());
		}
		
		for(int i = 0; i < posNotCovIndVector.size(); i++) {
			g.setColor(Color.BLACK);
			g.drawString(posNotCovIndVector.get(i).getPoint(), posNotCovIndVector.get(i).getXAxis(), posNotCovIndVector.get(i).getYAxis());
		}
		
		for(int i = 0; i < negCovIndVector.size(); i++) {
			g.setColor(Color.BLACK);
			g.drawString(negCovIndVector.get(i).getPoint(), negCovIndVector.get(i).getXAxis(), negCovIndVector.get(i).getYAxis());
		}

	}
	
	private void computeGraphics(){
		int posGes = model.getPosListModel().size();
		int notCovPos = eval.getNotCoveredPositives().size();
		int covNeg = eval.getCoveredNegatives().size();
		int negGes = model.getNegListModel().size();
		double notCov = notCovPos;
		float shift = (float) (width*(notCov/posGes));
		shiftXAxis = Math.round(shift);
		distortion = 0;
		if(shiftXAxis == 0) {
			distortion = Math.round((width*(covNeg/negGes))/4);
		}
		
	}
	
	private void computeIndividualPoints() {
		Set<Individual> posInd = eval.getCoveredPositives();
		int i = 0;
		double x = 20;
		double y = 20;
		for(Individual ind : posInd) {
			if(i<maxNumberOfIndividualPoints) {
				i++;
				if(x >= oldConcept.getMaxX()) {
					x = (int) oldConcept.getMinX();
					y = y + gap;
				}
				
				if(y >= oldConcept.getMaxY()) {
					y = (int) oldConcept.getMinY();
				}
				
				if(x >= newConcept.getMaxX()) {
					x = (int) newConcept.getMinX();
					y = y + gap;
				}
				
				if(y >= newConcept.getMaxY()) {
					y = (int) newConcept.getMinY();
				}
				
				while(x < newConcept.getMaxX()) {
					
					if(newConcept.contains(x, y) && oldConcept.contains(x, y)) {
						posCovIndVector.add(new IndividualPoint("+",(int)x,(int)y,ind.toString()));
						x = x + gap;
						break;
					} else {
						x = x + gap;
					}
				}
			}
		}
		
		Set<Individual> posNotCovInd = eval.getNotCoveredPositives();
		int j = 0;
		for(Individual ind : posNotCovInd) {
			if(j<maxNumberOfIndividualPoints) {
				j++;
				if(x >= oldConcept.getMaxX()) {
					x = (int) oldConcept.getMinX();
					y = y + gap;
				}
				
				if(y >= oldConcept.getMaxY()) {
					y = (int) oldConcept.getMinY();
				}
				
				while(x < oldConcept.getMaxX()) {
					
					if(oldConcept.contains(x, y)&&!newConcept.contains(x, y)) {
						posNotCovIndVector.add(new IndividualPoint("-",(int)x,(int)y,ind.toString()));
						x = x + gap;
						break;
					} else {
						x = x + gap;
					}
				}
			}
		}
		
		Set<Individual> negCovInd = eval.getCoveredNegatives();
		int k = 0;
		for(Individual ind : negCovInd) {
			if(k<maxNumberOfIndividualPoints) {
				k++;
				if(x >= newConcept.getMaxX()) {
					x = (int) newConcept.getMinX();
					y = y + gap;
				}
				
				if(y >= newConcept.getMaxY()) {
					y = (int) newConcept.getMinY();
				}
				
				while(x < newConcept.getMaxX()) {
					if(newConcept.contains(x, y) && !oldConcept.contains(x, y)) {
						negCovIndVector.add(new IndividualPoint("o",(int)x,(int)y,ind.toString()));
						x = x + gap;
						break;
					} else {
						x = x + gap;
					}
				}
			}
		}
	}
	
	
}