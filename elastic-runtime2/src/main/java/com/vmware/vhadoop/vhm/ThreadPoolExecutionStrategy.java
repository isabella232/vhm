package com.vmware.vhadoop.vhm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import java.util.*;

import com.vmware.vhadoop.api.vhm.ExecutionStrategy;
import com.vmware.vhadoop.api.vhm.events.ClusterScaleCompletionEvent;
import com.vmware.vhadoop.api.vhm.events.ClusterScaleEvent;
import com.vmware.vhadoop.api.vhm.events.EventConsumer;
import com.vmware.vhadoop.api.vhm.events.EventProducer;
import com.vmware.vhadoop.api.vhm.strategy.ScaleStrategy;

public class ThreadPoolExecutionStrategy implements ExecutionStrategy, EventProducer {
   ExecutorService _threadPool;
   Map<Set<ClusterScaleEvent>, Future<ClusterScaleCompletionEvent>> _runningTasks;
   static int _threadCounter = 0;
   EventConsumer _consumer;

   private static final Logger _log = Logger.getLogger(ThreadPoolExecutionStrategy.class.getName());

   public ThreadPoolExecutionStrategy() {
      _threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
         @Override
         public Thread newThread(Runnable r) {
            return new Thread(r, "Cluster_Thread_"+(_threadCounter++));
         }
      });
      _runningTasks = new HashMap<Set<ClusterScaleEvent>, Future<ClusterScaleCompletionEvent>>();
   }
   
   @Override
   public void handleClusterScaleEvents(ScaleStrategy scaleStrategy, Set<ClusterScaleEvent> events) {
      Future<ClusterScaleCompletionEvent> task = _threadPool.submit(scaleStrategy.getCallable(events));
      synchronized(_runningTasks) {
         _runningTasks.put(events, task);
      }
   }

   @Override
   public void registerEventConsumer(EventConsumer consumer) {
      _consumer = consumer;
   }

   @Override
   public void start() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            List<Set<ClusterScaleEvent>> toRemove = new ArrayList<Set<ClusterScaleEvent>>();
            synchronized(_runningTasks) {
               while (true) {
                  for (Set<ClusterScaleEvent> key : _runningTasks.keySet()) {
                     Future<ClusterScaleCompletionEvent> task = _runningTasks.get(key);
                     if (task.isDone()) {
                        try {
                           ClusterScaleCompletionEvent completionEvent = task.get();
                           if (completionEvent != null) {
                              _log.info("Found completed task for cluster "+completionEvent.getClusterId());
                              _consumer.placeEventOnQueue(completionEvent);
                           }
                        } catch (InterruptedException e) {
                           _log.warning("Cluster thread interrupted");
                           e.printStackTrace();
                        } catch (ExecutionException e) {
                           _log.warning("ExecutionException in cluster thread");
                           e.printStackTrace();
                        }
                        toRemove.add(key);
                     }
                  }
                  for (Set<ClusterScaleEvent> key : toRemove) {
                     _runningTasks.remove(key);
                  }
                  toRemove.clear();
                  try {
                     _runningTasks.wait(1000);
                  } catch (InterruptedException e) {}
               }
            }
         }
      }, "ScaleStrategyCompletionListener").start();
   }

}
