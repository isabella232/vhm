package com.vmware.vhadoop.api.vhm;

import com.vmware.vhadoop.api.vhm.events.ClusterScaleEvent;
import com.vmware.vhadoop.api.vhm.strategy.ScaleStrategy;

public interface ExecutionStrategy {

   void handleClusterScaleEvent(ScaleStrategy scaleStrategy, ClusterScaleEvent event);

   void waitForClusterScaleCompletion();

}
