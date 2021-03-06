/***************************************************************************
* Copyright (c) 2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.vhadoop.api.vhm.events;

/* High level interface that works with EventConsumer
 * Most of the main components that plug into VHM are EventProducers. The events produced then trigger responses in the VHM */
public interface EventProducer {

   public interface EventProducerStartStopCallback {
      public void notifyFailed(EventProducer thisProducer);
      
      public void notifyStopped(EventProducer thisProducer);
      
      public void notifyStarted(EventProducer thisProducer);
   }
   
   public void registerEventConsumer(EventConsumer vhm);

   public void start(EventProducerStartStopCallback callback);

   public void stop();
   
   public boolean isStopped();
}
