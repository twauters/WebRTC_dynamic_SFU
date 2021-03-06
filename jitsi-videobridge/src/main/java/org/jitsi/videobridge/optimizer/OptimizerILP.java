package org.jitsi.videobridge.optimizer;

public class OptimizerILP extends Optimizer {

	public OptimizerILP(int numLayers, int minEncoding, int maxEncoding, int numEncodings, String type, String domain) {
		super(numLayers, minEncoding, maxEncoding, numEncodings, type, domain);
	}

	@Override
	public int[] runOptimization(int[] bandwidth) {
				
		ILPModel model;
		switch(solverType){
		case "ilp":		model = new ILPMinimizeDistance(bandwidth, encodings, numLayers, domain); break;
		case "ilpmin":	model = new ILPMinimizeDistanceMinAssigned(bandwidth, encodings, numLayers, domain); break;
		default:		model = new ILPMinimizeDistanceMinAssigned(bandwidth, encodings, numLayers, domain); break;
		}
		
		model.createModel();
		this.execTime = model.solveModel();
		int [] bitRates = null;
		bitRates = model.getSolution();
		
		bitRates = fillGaps(bitRates);
		
		model.stopModel();
		
		return bitRates;
	}

}
