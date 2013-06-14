/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.vhadoop.api.vhm;

import java.util.Map;

import com.vmware.vhadoop.util.CompoundStatus;

/* Represents actions which can be invoked on the Hadoop subsystem */
public interface HadoopActions {

   public class HadoopClusterInfo {
      private String _clusterId;
      private String _jobTrackerAddr;
      private Integer _jobTrackerPort;
      
      public HadoopClusterInfo(String clusterId, String jobTrackerAddr, Integer jobTrackerPort) {
         _clusterId = clusterId;
         _jobTrackerAddr = jobTrackerAddr;
         _jobTrackerPort = jobTrackerPort;
      }
      
      public String getClusterId() {
         return _clusterId;
      }
      
      public String getJobTrackerAddr() {
         return _jobTrackerAddr;
      }

      public Integer getJobTrackerPort() {
         return _jobTrackerPort;
      }
   }

   public class JTConfigInfo {
      String _hadoopHomePath;
      String _excludeTTPath;

      public JTConfigInfo(String hadoopHomePath, String excludeTTPath) {
         _hadoopHomePath = hadoopHomePath;
         _excludeTTPath = excludeTTPath;
      }
      
      public String getHadoopHomePath() {
         return _hadoopHomePath;
      }
      
      public String getExcludeTTPath() {
         return _excludeTTPath;
      }
   }

   /** 
	 * 	Decommission a given set of TaskTrackers from a JobTracker 
	 *	@param TaskTrackers that need to be decommissioned
	 *  @return SUCCESS/FAIL
	 */
		public CompoundStatus decommissionTTs(Map<String, String> tts, HadoopClusterInfo cluster);

	/** 
	 * 	Recommission a given set of TaskTrackers to a JobTracker 
	 *	@param TaskTrackers that need to be recommissioned/added
	 *  @return SUCCESS/FAIL
	 */
		public CompoundStatus recommissionTTs(Map<String, String> tts, HadoopClusterInfo cluster);

		
		public CompoundStatus checkTargetTTsSuccess(String opType, Map<String, String> tts, int totalTargetEnabled, HadoopClusterInfo cluster);
		
	   public String[] getActiveTTs(HadoopClusterInfo cluster, int totalTargetEnabled, CompoundStatus status);
}
