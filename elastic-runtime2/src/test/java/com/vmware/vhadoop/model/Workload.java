package com.vmware.vhadoop.model;

abstract public class Workload extends ResourceUsage
{
   /**
    * Basic constructor that takes the workload ID for later use
    * @param id
    */
   public Workload(String id) {
      super(id);
   }

   /**
    * Stops the workload
    * @param b - force stop if true, shutdown cleanly if false
    */
   public void stop(boolean b) {
      setCpuUsage(0);
      setMemoryUsage(0);
   }

   /**
    * Starts the workload running. This commences resource utilization.
    */
   public abstract void start();

}