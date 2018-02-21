package org.jitsi.videobridge.optimizer;

public class PSNRHelp {
	
	private double c1 = 3.1364082475;
	private double c2 = 18.297567189;
	private int qp;
	
	public PSNRHelp(int qp){
		this.qp = 20;
	}
	
	public double toPSNR(double rate){
		
		double PSNR = c1*Math.log(rate) + c2; 
		
		return PSNR*10000.0;//*1000 allows to keep the problem in the integer domain
		
	}
	
	public double toRate(double PSNR){
		
		double PSNRApp = PSNR/10000.0;//reversed the processing done before
		
		double rate = Math.exp((PSNRApp - c2)/c1);
		
		return rate;
		
	}
	
//	private double c1 = -0.000001647;
//	private double c2 = 0.0090977444;
//	private double c3 = 29.5492302184;
//	private int qp;
//	
//	public PSNRHelp(int qp){
//		this.qp = 20;
//	}
//	
//	public double toPSNR(double rate){
//		
//		double PSNR = c1*Math.pow(rate, 2.0) + c2*rate + c3; 
//		
//		return PSNR*10000.0;//*1000 allows to keep the problem in the integer domain
//		
//	}
//	
//	public double toRate(double PSNR){
//		
//		double PSNRApp = PSNR/10000.0;//reversed the processing done before
//		
//		double rate = (-c2 + Math.sqrt(Math.pow(c2, 2.0) -4*c1*(c3-PSNRApp)))/(2*c1);
//		
//		return rate;
//		
//	}
	
//	Based on paper "Low-Complexity No-Reference PSNR Estimation for H.264/AVC Encoded Video"
	
//	private double c1 = 74.791;
//	private double c2 = -2.215;
//	private double c3 = -0.975;
//	
//	public double toPSNR(double rate){
//		
//		double PSNR = c1 + c2*Math.log(rate) + c3*this.qp;
//		
//		return -PSNR*1000;//"-1" allows to keep the problem as a maximization; *1000 allows to keep the problem in the integer domain
//		
//	}
//	
//	public double toRate(double PSNR){
//		
//		double PSNRApp = -PSNR/1000;//reversed the processing done before
//		
//		double rate = Math.exp((PSNRApp-c1-c3*this.qp)/c2);
//		
//		return rate;
//		
//	}

}
