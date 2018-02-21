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

package org.jitsi.impl.neomedia;

import org.jitsi.util.*;
import java.util.*;
import com.google.common.eventbus.*;

public class SenderBitrateController
{
	private static final Logger logger = Logger.getLogger(SenderBitrateController.class);
	private HashMap<Long, Long> bitrates;	
	public SenderBitrateController(){
		bitrates = new HashMap<>();
	}

	public long getLatestBitrate(long ssrc){
		return containsSSRC(ssrc) ? bitrates.get(ssrc) : -1;
	}

	public long getSSRCForBitrate(long bitrate){
		for (Map.Entry<Long,Long> entry : bitrates.entrySet()){
			if(entry.getValue() == bitrate){
				return entry.getKey();
			}
		}
		return -1;
	}

	public void setBitrate(long ssrc,long bitrate){
		logger.debug("Setting bitrate for sender SSRC: " + ssrc);
		bitrates.put(ssrc,bitrate);
	}

	public boolean containsSSRC(long ssrc){
		return bitrates.containsKey(ssrc);
	}
}


