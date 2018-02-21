package org.jitsi.videobridge.optimizer;

import java.util.Arrays;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

public abstract class ILPModel {
	
	protected int[] bandwidth;
	protected int[] encodings;
	protected int numLayers;
	protected String domain;
	protected IloCplex cplex;
	protected IloNumVar[][] var;
	protected IloRange[][] rng;
	
	protected ILPModel(int[] b, int[] en, int nl, String dm){
		
		this.bandwidth = b;
		this.encodings = en;
		this.numLayers = nl;
		this.domain = dm;
	}
	
	abstract void createModel();
	
	double solveModel(){
		
		double executionTime = -1.0;
		
		try {
			cplex.setOut(null);
			if (cplex.solve()) {
				//cplex.output().println("[ILPModel] Solution status = " + cplex.getStatus());
				//cplex.output().println("[ILPModel] Solution value  = " + cplex.getObjValue());
				
				executionTime = cplex.getCplexTime();
				double[] solutions = cplex.getValues(this.var[0]);
				
			}
		} catch (IloException e) {
	         System.err.println("[ILPModel] Concert exception: " + e + " caught");
	      }
		
		return executionTime;
	}

	int[] getSolution(){

		try {

			double[] solutions = cplex.getValues(this.var[0]);
			int[] bitRates = new int[this.numLayers];

			int j = 0;
			for (int i = 0; i < this.encodings.length; i++){
				if (solutions[i + this.bandwidth.length*this.encodings.length] == 1.0){
					bitRates[j] = encodings[i];
					j++;
				}
			}
			
			//System.out.println("[ILPModel] " + Arrays.toString(bitRates));

			return bitRates;

		} catch (UnknownObjectException e) {
			e.printStackTrace();
			return null;
		} catch (IloException e) {
			e.printStackTrace();
			return null;
		}
	}

	void stopModel(){
		
		cplex.end();
	}

}
