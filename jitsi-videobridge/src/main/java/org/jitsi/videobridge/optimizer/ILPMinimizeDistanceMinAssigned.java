package org.jitsi.videobridge.optimizer;

import ilog.concert.IloException;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class ILPMinimizeDistanceMinAssigned extends ILPModel {

	public ILPMinimizeDistanceMinAssigned(int[] b, int[] e, int nl, String dm){
		super(b, e, nl, dm);
	}

	@Override
	void createModel() {

		try {
			this.cplex = new IloCplex();

			this.var = new IloNumVar[1][];
			this.rng = new IloRange[1][];

			populateByRow(this.cplex, this.var, this.rng);

		} catch (IloException e) {
			System.err.println("[ILP] Concert exception: " + e + " caught");
		}

	}
	
	void populateByRow(IloMPModeler model, IloNumVar[][] variable, IloRange[][] constraints) throws IloException {
		
		int numClients = this.bandwidth.length;
		int numEncodings = this.encodings.length;
		int numVariables = numClients*numEncodings + numEncodings;
		
		PSNRHelp psnr = new PSNRHelp(20);
		
		IloNumVar[] x = model.numVarArray(numVariables, 0.0, 1.0, IloNumVarType.Bool);
		variable[0] = x;
		
		IloNumExpr distance = model.numExpr();
		for (int c = 0; c < numClients; c++){
			for (int e = 0; e < numEncodings; e++){
				//distance = model.sum(distance, model.diff(this.bandwidth[c], model.prod(this.encodings[e], x[c*numEncodings+e])));
				if (this.domain.equals("rate")){
					distance = model.sum(distance, model.prod(Math.abs(this.bandwidth[c] - this.encodings[e]), x[c*numEncodings+e]));
				}
				else{
					distance = model.sum(distance, model.prod(Math.abs(psnr.toPSNR(this.bandwidth[c]) - psnr.toPSNR(this.encodings[e])), x[c*numEncodings+e]));
				}
			}
		}
		for (int i = 0; i < numEncodings; i++){
			distance = model.sum(distance, model.prod(0.0, x[numClients*numEncodings+i]));
		}
		model.addMinimize(distance);
		
		int numConstraints = 1 + numClients*numEncodings + numClients + numClients + 1;
		constraints[0] = new IloRange[numConstraints];
		
		//Constraint number of layers
		IloNumExpr numLayers = model.numExpr();
		for (int e = 0; e < numEncodings; e++){
			numLayers = model.sum(numLayers, x[numClients*numEncodings + e]);
		}
		constraints[0][0] = model.addLe(numLayers, this.numLayers, "ct_num_layers");
		
		//Constraint on beta and alpha
		for (int c = 0; c < numClients; c++){
			for (int e = 0; e < numEncodings; e++){
				constraints[0][1+c*numEncodings+e] = model.addGe(model.diff(x[numClients*numEncodings + e], x[c*numEncodings+e]), 0.0);
			}
		}
		
		//Constraint on number of qualities per client
		for (int c = 0; c < numClients; c++){
			IloNumExpr numLayersClient = model.numExpr();
			for (int e = 0; e < numEncodings; e++){
				numLayersClient = model.sum(numLayersClient, x[c*numEncodings+e]);
			}
			constraints[0][1+numClients*numEncodings+c] = model.addEq(numLayersClient, 1.0);
		}

		//Constraint on bandwidth
		for (int c = 0; c < numClients; c++){
			IloNumExpr banClient = model.numExpr();
			for (int e = 0; e < numEncodings; e++){
				banClient = model.sum(banClient, model.prod(this.encodings[e], x[c*numEncodings+e]));
			}
			constraints[0][1+numClients*numEncodings+numClients+c] = model.addGe(this.bandwidth[c], banClient);
		}
		
		constraints[0][1 + numClients*numEncodings + numClients + numClients] = model.addEq(x[numClients*numEncodings], 1.0);
		
	}
	
	void populateByRow2(IloMPModeler model, IloNumVar[][] variable, IloRange[][] constraints) throws IloException {
		
		int numClients = this.bandwidth.length;
		int numEncodings = this.encodings.length;
		int numVariables = numClients*numEncodings + numEncodings;
		
		IloNumVar[] x = model.numVarArray(numVariables, 0.0, 1.0, IloNumVarType.Bool);
		variable[0] = x;
		
		//Optimization function
		IloNumExpr distance = model.numExpr();
		for (int c = 0; c < numClients; c++){
			for (int e = 0; e < numEncodings; e++){
				distance = model.sum(distance, model.diff(this.bandwidth[c], model.prod(this.encodings[e], x[c*e+e])));
			}
		}
		model.addMinimize(distance);
		
		int numConstraints = 1 + numClients*numEncodings + numClients + numClients;
		constraints[0] = new IloRange[1];
		
		//Constraint number of layers
		/*IloNumExpr numLayers = this.cplex.numExpr();
		for (int e = 0; e < numEncodings; e++){
			numLayers = this.cplex.sum(numLayers, this.var[0][numClients*numEncodings + e]);
		}
		this.rng[0][0] = this.cplex.addLe(numLayers, this.numLayers, "ct_num_layers");*/

		/*//Constraint on beta and alpha
		for (int c = 0; c < numClients; c++){
			for (int e = 0; e < numEncodings; e++){
				constraints[0][1+c*e+e] = model.addGe(model.diff(x[numClients*numEncodings + e], x[c*e+e]), 0.0);
			}
		}
		
		//Constraint on number of qualities per client
		for (int c = 0; c < numClients; c++){
			IloNumExpr numLayersClient = model.numExpr();
			for (int e = 0; e < numEncodings; e++){
				numLayersClient = model.sum(numLayersClient, x[c*e+e]);
			}
			constraints[0][1+numClients*numEncodings+c] = model.addEq(numLayersClient, 1.0);
		}

		//Constraint on bandwidth
		for (int c = 0; c < numClients; c++){
			IloNumExpr banClient = model.numExpr();
			for (int e = 0; e < numEncodings; e++){
				banClient = model.sum(banClient, model.prod(this.encodings[e], x[c*e+e]));
			}
			constraints[0][1+numClients*numEncodings+numClients+c] = model.addGe(this.bandwidth[c], banClient);
		}*/
		
	}
	
	void populateByRowExample(IloMPModeler model, IloNumVar[][] var, IloRange[][] rng) throws IloException {

		IloNumVar[] x = model.numVarArray(3, 0.0, 1.0, IloNumVarType.Bool);
		var[0] = x;

		double[] objvals = {1.0, 2.0, 3.0};
		model.addMaximize(model.scalProd(x, objvals));

		rng[0] = new IloRange[2];
		rng[0][0] = model.addLe(model.sum(	model.prod(-1.0, x[0]),
				model.prod( 1.0, x[1]),
				model.prod( 1.0, x[2])), 20.0, "c1");
		rng[0][1] = model.addLe(model.sum(	model.prod( 1.0, x[0]),
				model.prod(-3.0, x[1]),
				model.prod( 1.0, x[2])), 30.0, "c2");
	}

	void populateByRowExample2(IloMPModeler model, IloNumVar[][] var, IloRange[][] rng) throws IloException {

		double[] lb = {0.0, 0.0, 0.0};
		double[] ub = {40.0, Double.MAX_VALUE, Double.MAX_VALUE};
		String[] varname = {"x1", "x2", "x3"};
		IloNumVar[] x = model.numVarArray(3, lb, ub, varname);
		var[0] = x;

		double[] objvals = {1.0, 2.0, 3.0};
		model.addMaximize(model.scalProd(x, objvals));

		rng[0] = new IloRange[2];
		rng[0][0] = model.addLe(model.sum(	model.prod(-1.0, x[0]),
				model.prod( 1.0, x[1]),
				model.prod( 1.0, x[2])), 20.0, "c1");
		rng[0][1] = model.addLe(model.sum(	model.prod( 1.0, x[0]),
				model.prod(-3.0, x[1]),
				model.prod( 1.0, x[2])), 30.0, "c2");
	}

}
