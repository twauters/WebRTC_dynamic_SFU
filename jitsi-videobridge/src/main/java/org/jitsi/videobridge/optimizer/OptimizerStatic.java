package org.jitsi.videobridge.optimizer;

public class OptimizerStatic extends Optimizer {

	public OptimizerStatic(int numLayers, int minEncoding, int maxEncoding, int numEncodings, String type, String domain) {
		super(numLayers, minEncoding, maxEncoding, numEncodings, type, domain);
	}

	@Override
	public int[] runOptimization(int[] bandwidth) {
		
		int[] bitRates;
		
		if (this.domain.equals("rate")){
			bitRates = staticLayersRate(minEncoding, maxEncoding, numLayers);
		}
		else{
			bitRates = staticLayersPSNR(minEncoding, maxEncoding, numLayers);
		}
		
		return bitRates;
	}
	
	int[] staticLayersRate(int minEncoding, int maxEncoding, int numLayers){
		
		int[] bitRates = new int[numLayers];
		
		int step = (maxEncoding - minEncoding)/(numLayers-1);
		for(int i = 0; i < numLayers; i++){
			bitRates[i] = minEncoding + step*i;
		}
		
		return bitRates;
	}
	
	int[] staticLayersPSNR(int minEncoding, int maxEncoding, int numLayers){
		
		int[] bitRates = new int[numLayers];
		
		int minPSNR = (int) this.psnr.toPSNR(minEncoding);
		int maxPSNR = (int) this.psnr.toPSNR(maxEncoding);
		
		int step = (maxPSNR - minPSNR)/(numLayers-1);
		for(int i = 0; i < numLayers; i++){
			bitRates[i] = (int) this.psnr.toRate(minPSNR + step*i);
		}
		
		return bitRates;
	}

}
