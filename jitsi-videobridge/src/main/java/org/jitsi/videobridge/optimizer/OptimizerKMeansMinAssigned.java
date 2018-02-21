package org.jitsi.videobridge.optimizer;

import java.util.ArrayList;
import java.util.Arrays;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class OptimizerKMeansMinAssigned extends Optimizer {

	public OptimizerKMeansMinAssigned(int numLayers, int minEncoding, int maxEncoding, int numEncodings, String type, String domain) {
		super(numLayers, minEncoding, maxEncoding, numEncodings, type, domain);
	}

	@Override
	public int[] runOptimization(int[] bandwidth) {
		
		int[] bandApp = new int[bandwidth.length];
		int numActive = 0;
		for (int c = 0; c < bandwidth.length; c++){
			if (bandwidth[c] >= this.encodings[1]){
				bandApp[numActive] = bandwidth[c];
				numActive++;
			}
		}
		int [] band = new int[numActive];
	    	System.arraycopy(bandApp, 0, band, 0, numActive);

		SimpleKMeans kMeans = new SimpleKMeans();
		int [] bitRates = new int[this.numLayers];

		try {

			kMeans.setNumClusters(Math.min(this.numLayers-1, band.length));

			double startTime = System.currentTimeMillis();
			Instances dataset = buildDataset(band);
			kMeans.buildClusterer(dataset);
			this.execTime = System.currentTimeMillis() - startTime;

			Instances centroids = kMeans.getClusterCentroids();
			centroids.sort(0);
			
			bitRates[0] = this.minEncoding;			
			for (int i = 0; i < centroids.numInstances(); i++) {
				
				if (this.domain.equals("rate")){
					bitRates[i+1] = (int)Double.parseDouble(centroids.instance(i).toString());
				}
				else{
					bitRates[i+1] = (int)this.psnr.toRate(Double.parseDouble(centroids.instance(i).toString()));
				}
			}
			
			bitRates = fillGaps(bitRates);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return bitRates;
	}

	Instances buildDataset(int[] array) throws Exception{

		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		atts.add(new Attribute("bandwidth"));

		Instances dataset = new Instances("Rel", atts, 5);

		for (int i = 0; i < array.length; i++){

			Instance entry = new DenseInstance(1);
			
			if (this.domain.equals("rate")){
				entry.setValue(atts.get(0), Math.min(this.maxEncoding, array[i]));
			}
			else{
				entry.setValue(atts.get(0), this.psnr.toPSNR(Math.min(this.maxEncoding, array[i])));
			}

			dataset.add(entry);
		}

		return dataset;
	}

}
