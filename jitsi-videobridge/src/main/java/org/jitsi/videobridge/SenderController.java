/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.videobridge;

import org.jitsi.service.libjitsi.*;
import org.jitsi.util.*;
import org.jitsi.service.neomedia.rtp.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.configuration.*;
import org.jitsi.videobridge.optimizer.*;
import org.jitsi.videobridge.cc.*;

import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SenderController{

	private static final ConfigurationService cfg = LibJitsi.getConfigurationService();

    	private static final int MIN_ENCODING = cfg.getInt("org.jitsi.videobridge.MIN_ENCODING", 50);
	private static final int MAX_ENCODING = cfg.getInt("org.jitsi.videobridge.MAX_ENCODING", 2500);
	private static final int NUM_ENCODINGS = cfg.getInt("org.jitsi.videobridge.NUM_ENCODINGS", 40);
	private static final int OPT_PERIOD = cfg.getInt("org.jitsi.videobridge.OPT_PERIOD", 8000);
	private static final int NUM_ENCODERS = cfg.getInt("org.jitsi.videobridge.NUM_ENCODERS", 3);
	private static final String OPT_ALG = cfg.getString("org.jitsi.videobridge.OPT_ALG", "ilpmin");
	private static final String DOMAIN = cfg.getString("org.jitsi.videobridge.DOMAIN", "rate");
	private static final String FOLDER = cfg.getString("org.jitsi.videobridge.RESULT_FOLDER", "./logs");
	private static final int TIMER_LOGGING = cfg.getInt("org.jitsi.videobridge.PADDING_PERIOD_MS", 1000);

	private static final Logger logger = Logger.getLogger(SenderController.class);

	private final Conference conference;		
	private List<VideoChannel> videoChannels;
	private HashMap<Long, Integer> sendingRates;
	private HashMap<String, Integer> clientsBandwidth;
	private Boolean optStarted = false;
	private Optimizer opt;
	private int numClients = 0;
	private Writer opt_logger;
	
	public SenderController(Conference conference){

		this.conference = conference;
		videoChannels=new ArrayList<>();
		this.sendingRates = new HashMap<Long, Integer>();
		this.clientsBandwidth = new HashMap<String, Integer>();

		switch(OPT_ALG){
			case "static": 			opt = new OptimizerStatic(NUM_ENCODERS, MIN_ENCODING, MAX_ENCODING, NUM_ENCODINGS, null, DOMAIN); break;
			case "ilp": case "ilpmin":	opt = new OptimizerILP(NUM_ENCODERS, MIN_ENCODING, MAX_ENCODING, NUM_ENCODINGS, OPT_ALG, DOMAIN); break;
			case "kmeansmin":		opt = new OptimizerKMeansMinAssigned(NUM_ENCODERS, MIN_ENCODING, MAX_ENCODING, NUM_ENCODINGS, OPT_ALG, DOMAIN); break;
			default: 			opt = new OptimizerILP(NUM_ENCODERS, MIN_ENCODING, MAX_ENCODING, NUM_ENCODINGS, "ilpmin", DOMAIN); break;
		}

		this.opt_logger = null;
		try {
			this.opt_logger = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FOLDER + "/opt_logger.txt"), "utf-8"));
		}
		catch(FileNotFoundException | UnsupportedEncodingException ex){
			System.out.println("[Error] Impossible to create file: " + ex);
		}
	}

	

	private javax.swing.Timer timer_optimization = new javax.swing.Timer(OPT_PERIOD, new ActionListener(){

	@Override
	public void actionPerformed(ActionEvent e){

		System.out.println("[SenderController] Optimization period has elapsed. Start new optimization.");

		//Creating int[] containing client bandwidth estimations (HashMap<String, Integer> --> Integer[] --> int[])
		Integer[] bandInteger = clientsBandwidth.values().toArray(new Integer[clientsBandwidth.size()]);
		int[] band = Arrays.stream(bandInteger).mapToInt(Integer::intValue).toArray();
		System.out.println("[SenderController] Client bandwidth estimations: " + Arrays.toString(band));

		double startTime = System.currentTimeMillis();
		int[] solution = opt.runOptimization(band);
		double execTime = System.currentTimeMillis() - startTime;		

		System.out.println("[SenderController] Solution is: " + Arrays.toString(solution) + "; found in " + execTime + "");
		
		//Creating ordered list of the sending rates, to improve final assignment
		Comparator<Map.Entry<Long, Integer>> byMapValues = new Comparator<Map.Entry<Long, Integer>>() {
        		@Override
        		public int compare(Map.Entry<Long, Integer> left, Map.Entry<Long, Integer> right) {
            			return left.getValue().compareTo(right.getValue());
        		}
    		};
		List<Map.Entry<Long, Integer>> rates = new ArrayList<Map.Entry<Long, Integer>>();
    		rates.addAll(sendingRates.entrySet());
    		Collections.sort(rates, byMapValues);
		
		for (int i = 0; i < rates.size(); i++){
			setSendingBitrateForSSRC(rates.get(i).getKey(), solution[i]*1000);
		}

		for (Map.Entry<String, Integer> entry : clientsBandwidth.entrySet()) {
			VideoChannel channel = findVideoChannelForID(entry.getKey());
			if (channel.getProbing().getAvgError() > 0)
				estimatedBandwidthChanged(entry.getKey(), 1000*entry.getValue());
		}

		//Logging
		double avg_error = 0.0;
		double avg_rate = 0.0;
		double avg_psnr = 0.0;
		double count = 0.0;
		for(VideoChannel channel : videoChannels){
			double err = channel.getProbing().getAvgError();
			double rate = channel.getProbing().getAvgPlayedRate();
			double psnr = channel.getProbing().getAvgPSNR();
			if (err > 0){
				avg_error += err;
				avg_rate += rate;
				avg_psnr += psnr;
				count++;
			}
		}
		System.out.println("[SenderController] Num clients: " + count + "; Avg group error: " + (avg_error/count) + "; Avg group played rate: " + (avg_rate/count) + "; Avg group PSNR: " + (avg_psnr/count));

		try{
			opt_logger.write(System.currentTimeMillis() + ";" + Arrays.toString(solution) + ";" + execTime + ";" + count + ";" + (avg_error/count) + ";" + (avg_rate/count) + ";" + (avg_psnr/count) + "\n");
			opt_logger.flush();
		}
		catch(IOException ex){
			System.out.println("[Error] Impossible to write on file: " + ex);
		}
		
	}

	});

	public void newVideoChannel(VideoChannel videoChannel){

		videoChannels.add(videoChannel);
		System.out.println("[SenderController] VideoChannel added: " + videoChannel.getID());

		if(videoChannels.size() > NUM_ENCODERS){

			final VideoChannel ch = videoChannel;
			final String clientID = videoChannel.getID();
			final BandwidthProbing probing = videoChannel.getProbing();
			
			Writer writer_tmp = null;
			try {
				writer_tmp = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FOLDER + "/client_id" + this.numClients + ".txt"), "utf-8"));
			}
			catch(FileNotFoundException | UnsupportedEncodingException ex){
				System.out.println("[Error] Impossible to create file: " + ex);
			}
			final Writer writer = writer_tmp;

			this.numClients++;
			setSenderForClient(getSendingChannels().get(0).getID(), clientID, 0);

			javax.swing.Timer t_bwe = new javax.swing.Timer(500, new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent e){

						VideoMediaStreamImpl destStream	= (VideoMediaStreamImpl) ch.getStream();
						BandwidthEstimator bwe = destStream == null ? null : destStream.getOrCreateBandwidthEstimator();

						long bweBps = bwe.getLatestEstimate();
						if(bweBps != -1){
							estimatedBandwidthChanged(clientID, bweBps);
						}
					}

				});

			t_bwe.start();

			javax.swing.Timer timer_logging = new javax.swing.Timer(TIMER_LOGGING, new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent e){
						
						try{
							writer.write(System.currentTimeMillis() + ";" + probing.getBandwidth() + ";" + probing.getPlayedRate() + ";" + probing.getPSNR() +"\n");
							writer.flush();
						}
						catch(IOException ex){
							System.out.println("[Error] Impossible to write on file: " + ex);
						}
					}

				});

			timer_logging.start();
			
			if (!this.optStarted){
				
				System.out.println("[SenderController] Starting optimization");
				this.optStarted = true;
				timer_optimization.start();
			}
		}

		System.out.println("[SenderController] Number of VideoChannels: " + videoChannels.size() + " - sending channels: " + getSendingChannels().size());
	}
	
	public void setSendingBitrates(int bitrate){

		for(VideoChannel channel : videoChannels){

			int[] ssrcs = channel.getReceiveSSRCs();
			if(channel.getLastN() == 0 && ssrcs.length > 0){
				setSendingBitrateForSSRC(ssrcs[0] & 0xFFFFFFFFL, bitrate);
			}
		}

	}

	public void setSendingBitrateForSSRC(long ssrc, int bitrate){
		
		VideoChannel channel = findVideoChannelForSSRC(ssrc);
		
		if (channel.getLastN() == 0){
			System.out.println("[SenderController] Setting ssrc " + (ssrc & 0xFFFFFFFFL) + " to bitrate " + bitrate);
			channel.getMediaService().getSenderBitrateController().setBitrate(ssrc, bitrate);
			this.sendingRates.put(ssrc, bitrate);
		}
	}

	//This method is only called at the beginning, to fix the initial bitrates
	public void setSendingBitrateForSSRC(long ssrc){

		int rateStep = (MAX_ENCODING - MIN_ENCODING)/(NUM_ENCODERS-1);
		int bitrate = (MIN_ENCODING + rateStep * sendingRates.size())*1000;
		
		if (bitrate <= MAX_ENCODING*1000)
			setSendingBitrateForSSRC(ssrc, bitrate);

	}

	public void estimatedBandwidthChanged(String clientID, long bandwidth){

		this.clientsBandwidth.put(clientID, (int)(Math.max(bandwidth/1000, MIN_ENCODING)));

		VideoChannel channel = findVideoChannelForID(clientID);
		
		if(channel.getLastN() == 1){
			setSenderForClient(getSenderIDForBitrate(bandwidth), clientID, bandwidth);
		}
	}

	private String getSenderIDForBitrate(long bandwidth){
		
		long ssrc = 0;
		int bitrate = 0;
		
		long min_ssrc = 0;
		int min_bitrate = 1000*MAX_ENCODING;

		for (Map.Entry<Long, Integer> entry : this.sendingRates.entrySet()) {
    			Long key = entry.getKey();
    			Integer value = entry.getValue();
			if (bandwidth > value && value > bitrate){
				ssrc = key;
				bitrate = value;
			}
			if (value < min_bitrate){
				min_ssrc = key;
				min_bitrate = value;
			}
		}

		if (bitrate == 0){
			ssrc = min_ssrc;
			bitrate = min_bitrate;
		}
		
		return findVideoChannelIDForSSRC(ssrc);
	}
	
	public VideoChannel findVideoChannelForSSRC(long ssrc){

		for(VideoChannel channel : videoChannels){
			if(channel.getReceiveSSRCs().length > 0){
				long channelssrc = 0xFFFFFFFFL & channel.getReceiveSSRCs()[0];
				if(channelssrc == ssrc){
					return channel;			
				}
			}
		}
		return null;

	}
	
	public String findVideoChannelIDForSSRC(long ssrc){

		for(VideoChannel channel : videoChannels){
			long channelssrc = 0xFFFFFFFFL & channel.getReceiveSSRCs()[0];
			if(channelssrc == ssrc){
				return channel.getID();			
			}
		}
		return null;

	}

	public VideoChannel findVideoChannelForID(String id){

		for(VideoChannel channel : videoChannels){
			String i = channel.getID();
			if(i.equals(id)){
				return channel;			
			}
		}
		return null;

	}

	public void setSenderForClient(String idSender, String idClient, long bandwidth){

		VideoChannel channelClient = findVideoChannelForID(idClient);
		VideoChannel channelSender = findVideoChannelForID(idSender);

		if(channelClient != null && channelSender != null){

			Endpoint sender = channelSender.getEndpoint();
			Endpoint forwarded = channelClient.getBitrateController().getForwarded();

			if(forwarded == null || sender != forwarded){

				channelClient.getBitrateController().setForwardedEndpoint(channelSender.getEndpoint());
				long channelssrc = 0xFFFFFFFFL & channelSender.getReceiveSSRCs()[0];

				System.out.println("[SenderController] Assigning client " + idClient + " with bandwidth " + bandwidth + " to sender " + idSender + " with target bitrate " + this.sendingRates.get(0xFFFFFFFFL & findVideoChannelForID(idSender).getReceiveSSRCs()[0]));

				channelClient.getBitrateController().update(null, -1);

			}

		}
	}

	public List<VideoChannel> getSendingChannels(){
		List<VideoChannel> sending = new ArrayList<>();		
		for(VideoChannel channel : videoChannels){
			if(channel.getLastN() == 0 && channel.getReceiveSSRCs().length > 0){
				sending.add(channel);
			}
		}
		return sending;
	}
} 
