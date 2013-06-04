package com.vmware.vhadoop.vhm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;

import com.vmware.vhadoop.api.vhm.ClusterMap;
import com.vmware.vhadoop.api.vhm.events.EventConsumer;
import com.vmware.vhadoop.api.vhm.events.EventProducer;
import com.vmware.vhadoop.model.scenarios.BasicScenario;
import com.vmware.vhadoop.model.scenarios.Serengeti;
import com.vmware.vhadoop.model.scenarios.Serengeti.Master;
import com.vmware.vhadoop.util.ThreadLocalCompoundStatus;
import com.vmware.vhadoop.vhm.model.vcenter.Host;
import com.vmware.vhadoop.vhm.model.vcenter.VirtualCenter;
import com.vmware.vhadoop.vhm.vc.VcVlsi;

abstract public class ModelTestBase<T extends Serengeti, M extends Serengeti.Master> extends AbstractClusterMapReader implements EventProducer {
   /* front load the cost of the springframework initialization */
   static final VcVlsi _VCVLSI = new VcVlsi();

   /** Set this to "true" to disable the test timeouts */
   public final static String DISABLE_TIMEOUT = "disable.timeout";

   protected Logger _log;
   protected VHM _vhm;
   protected VirtualCenter _vCenter;
   protected T _serengeti;

   BootstrapMain _bootstrap;
   EventConsumer _consumer;

   long startTime;
   /** default timeout is two decision cycles plus warm up/cool down */
   long timeout = (2 * LIMIT_CYCLE_TIME) + TEST_WARM_UP_TIME + TEST_COOLDOWN_TIME;

   static int TEST_WARM_UP_TIME = 20000;
   static int TEST_COOLDOWN_TIME = 10000;
   static int LIMIT_CYCLE_TIME = 5000;

   public ModelTestBase(Logger logger) {
      _log = logger;
   }

   public ModelTestBase() {
      _log = Logger.getLogger(this.getClass().getName());
   }


   @After
   @Before
   public void resetSingletons() {
      MultipleReaderSingleWriterClusterMapAccess.destroy();
   }

   VHM init() {
      _bootstrap = new ModelController(null, null, _serengeti);
      return _bootstrap.initVHM(new ThreadLocalCompoundStatus());
   }

   protected void startVHM() {
      _vhm = init();
      _vhm.registerEventProducer(this);
      _vhm.start();
   }

   protected void setTimeout(long millis) {
      timeout = millis;
   }

   protected long timeout() {
      if (startTime == 0) {
         startTime = System.currentTimeMillis();
         return timeout;
      } else {
         boolean disableTimeout = Boolean.valueOf(System.getProperty(DISABLE_TIMEOUT, "false"));
         if (disableTimeout) {
            /** return an hour, every time we're asked */
            return 60 * 60 * 1000;
         } else {
            return startTime - System.currentTimeMillis() + timeout;
         }
      }
   }

   @Override
   public void registerEventConsumer(EventConsumer vhm) {
      _consumer = vhm;
   }

   @Override
   public void start(EventProducerStoppingCallback callback) {
      /* noop */
   }

   @Override
   public void stop() {
      /* noop */
   }

   /**
    * Will create a single host for the cluster to deploy on if setup's not been called with a different number.
    * Creates a symmetrical deployment of compute nodes on the available hosts
    * @param clusterName
    * @param computeNodesPerHost
    * @return
    */
   protected M createCluster(String clusterName, int computeNodesPerHost) {
      int numberOfComputeNodes = 0;

      if (_serengeti == null) {
         setup(1);
      }

      /* create a cluster to work with */
      @SuppressWarnings("unchecked")
      M cluster = (M)_serengeti.createCluster(clusterName);
      String clusterId = cluster.getClusterId();

      @SuppressWarnings("unchecked")
      List<Host> hosts = (List<Host>) _vCenter.get(Host.class);
      for (Host host : hosts) {
         if (cluster.getHost() == null) {
            /* master VMs need a host too */
            host.add(cluster);
         } else {
            cluster.createComputeNodes(computeNodesPerHost, host);
            numberOfComputeNodes+= computeNodesPerHost;
         }
      }

      /* register the cluster as an event producer */
      _vhm.registerEventProducer(cluster);

      /* power on the master node */
      cluster.powerOn();

      assertScaleStrategySet("wait for scale strategy to be determined", clusterId, timeout());

      /* wait for VHM to register the VMs */
      assertClusterMapVMsInPowerState("register VMs in cluster map", clusterId, numberOfComputeNodes, false, timeout());

      return cluster;
   }

   /**
    * Sub-classes MUST over-ride this method to create a serengeti of the desired type
    * @param numberOfHosts
    * @return
    */
   protected abstract T createSerengeti(String name, VirtualCenter vCenter);

   protected T setup(int numberOfHosts) {
      /* perform the basic test setup that ModelTestBase depends on */
      _vCenter = BasicScenario.getVCenter(numberOfHosts + 1, 0);
      _serengeti = createSerengeti(getClass().getName()+"-vApp", _vCenter);

      /* start the system */
      startVHM();

      return _serengeti;
   }

   @After
   public void cleanup() {
      if (_vhm != null) {
         _vhm.stop(true);

         _vhm = null;
      }

      if (_vCenter != null) {
         @SuppressWarnings("unchecked")
         List<Host> hosts = (List<Host>) _vCenter.get(Host.class);
         for (Host host : hosts) {
            host.powerOff();
         }

         _vCenter = null;
      }

      _serengeti = null;
   }

   public void assertActualVMsInPowerState(String msg, Master master, int number, boolean power, long timeout) {
      long deadline = System.currentTimeMillis() + timeout;
      _log.info(msg+" - waiting for VMs to power "+(power?"on":"off")+" in cluster "+master.getClusterId());
      while (master.numberComputeNodesInPowerState(power) < number && System.currentTimeMillis() < deadline) {
         _vCenter.waitForConfigurationUpdate(timeout());
      }

      assertEquals(msg+" - not enough powered "+(power ? "on" : "off")+" in cluster "+master.getClusterId(), number, master.numberComputeNodesInPowerState(power));
      _log.info(msg+" - VMs powered "+(power?"on":"off")+" in cluster "+master.getClusterId());
   }

   public void assertActualVMsInPowerState(String msg, Master master, int number, boolean power) {
      assertActualVMsInPowerState(msg, master, number, power, timeout());
   }

   /**
    * This inspects the cluster map for VMs in the specified state
    * @param clusterId
    * @param number
    * @param power
    * @param timeout
    */
   public void assertClusterMapVMsInPowerState(String msg, String clusterId, int number, boolean power, long timeout) {
      long deadline = System.currentTimeMillis() + timeout;
      boolean firstTime = true;

      _log.info("Waiting for VMs to power "+(power?"on":"off")+" in cluster map "+clusterId+", timeout "+(timeout/1000));
      Set<String> vms;
      do {
         if (!firstTime) {
            try {
               Thread.sleep(500);
            } catch (InterruptedException e) {}
            firstTime = false;
         }

         ClusterMap map = getAndReadLockClusterMap();
         /* we really care about number of VMs in the cluster but we know that they're starting powered off at this point */
         vms = map.listComputeVMsForClusterAndPowerState(clusterId, power);
         unlockClusterMap(map);
      } while ((vms == null || vms.size() < number) && System.currentTimeMillis() < deadline);

      assertEquals(msg+" - not enough powered "+(power ? "on" : "off")+" in cluster "+clusterId , number, vms != null ? vms.size() : 0);
      _log.info(msg+" - "+number+" VMs powered "+(power?"on":"off")+" in cluster map for cluster"+clusterId);
   }

   public void assertClusterMapVMsInPowerState(String msg, String clusterId, int number, boolean power) {
      assertClusterMapVMsInPowerState(msg, clusterId, number, power, timeout());
   }

   /**
    * This inspects the cluster map for VMs in the specified state
    * @param clusterId
    * @param number
    * @param power
    * @param timeout
    */
   public void assertScaleStrategySet(String msg, String clusterId, long timeout) {
      long deadline = System.currentTimeMillis() + timeout;
      boolean firstTime = true;

      _log.info("Waiting for scale strategy to be set in cluster map for "+clusterId+", timeout "+(timeout/1000));
      String strategy = null;
      do {
         if (!firstTime) {
            try {
               Thread.sleep(500);
            } catch (InterruptedException e) {}
            firstTime = false;
         }

         ClusterMap map = getAndReadLockClusterMap();
         strategy = map.getScaleStrategyKey(clusterId);
         unlockClusterMap(map);
      } while (strategy == null && System.currentTimeMillis() < deadline);

      assertNotNull(msg+" - scale strategy wasn't registered for cluster "+clusterId, strategy);
      _log.info("scale strategy "+strategy+" registered in cluster map for cluster"+clusterId);
   }

   public void assertScaleStrategySet(String msg, String clusterId) {
      assertScaleStrategySet(msg, clusterId, timeout());
   }
}
