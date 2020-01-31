package com.networknt.metrics;

import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.Metric;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.MetricRegistry.MetricBuilder;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

public class JVMMetricsUtil {
	
	public static void trackAllJVMMetrics(final MetricRegistry registry, final Map<String, String> commonTags) {
		 //JVM Metrics
		MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
		track("mem.heap_mem", memBean.getHeapMemoryUsage(), registry, commonTags);
		track("mem.nonheap_mem", memBean.getNonHeapMemoryUsage(), registry, commonTags);
		
		double hmu = ((Long)memBean.getHeapMemoryUsage().getUsed()).doubleValue();
		double hmc = ((Long)memBean.getHeapMemoryUsage().getMax()).doubleValue();
		if (hmc == -1) {
			hmc = ((Long)memBean.getHeapMemoryUsage().getCommitted()).doubleValue();
		}
		double nhmu = ((Long)memBean.getNonHeapMemoryUsage().getUsed()).doubleValue();
		double nhmc = ((Long)memBean.getNonHeapMemoryUsage().getMax()).doubleValue();
		if (nhmc == -1) {
			nhmc = ((Long)memBean.getNonHeapMemoryUsage().getCommitted()).doubleValue();	
		}
		
		track("mem.heap_usage", hmu / hmc, registry, commonTags);
		track("mem.nonheap_usage", nhmu / nhmc, registry, commonTags);
		
		
		MBeanServer beans = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName os = new ObjectName("java.lang:type=OperatingSystem");
			Double sysCpuLoad = (Double)beans.getAttribute(os, "SystemCpuLoad");
			Double processCpuLoad = (Double)beans.getAttribute(os, "ProcessCpuLoad");
			
			double totalPMemory = ((Long)beans.getAttribute(os, "TotalPhysicalMemorySize")).doubleValue();
			double freePMemory = ((Long)beans.getAttribute(os, "FreePhysicalMemorySize")).doubleValue();
			
			track("os.sys_cpu_load", sysCpuLoad, registry, commonTags);
			track("os.process_cpu_load", processCpuLoad, registry, commonTags);
			track("os.mem_usage", (totalPMemory-freePMemory)/totalPMemory, registry, commonTags);
		} catch (InstanceNotFoundException | AttributeNotFoundException | MalformedObjectNameException
				| ReflectionException | MBeanException e) {
			e.printStackTrace();
		}
		
		track("thread.count", ManagementFactory.getThreadMXBean().getThreadCount(), registry, commonTags);
	}
	
	private static void track(String name, MemoryUsage m, final MetricRegistry registry, final Map<String, String> commonTags) {
		MetricName mName = MetricRegistry.name("jvm", name).tagged(commonTags);
		registry.remove(mName.resolve("used"));
		registry.getOrAdd(mName.resolve("used"), createGaugeMetricBuilder(m.getUsed()));
		registry.remove(mName.resolve("init"));
		registry.getOrAdd(mName.resolve("init"), createGaugeMetricBuilder(m.getInit()));
		registry.remove(mName.resolve("max"));
		registry.getOrAdd(mName.resolve("max"), createGaugeMetricBuilder(m.getMax()));
		registry.remove(mName.resolve("committed"));
		registry.getOrAdd(mName.resolve("committed"), createGaugeMetricBuilder(m.getCommitted()));
	}
	private static void track(String name, Long value, final MetricRegistry registry, final Map<String, String> commonTags) {
		MetricName mName = MetricRegistry.name("jvm", name).tagged(commonTags);
		registry.remove(mName);
		registry.getOrAdd(mName, createGaugeMetricBuilder(value));
	}
	private static void track(String name, Double value, final MetricRegistry registry, final Map<String, String> commonTags) {
		MetricName mName = MetricRegistry.name("jvm", name).tagged(commonTags);
		registry.remove(mName);
		registry.getOrAdd(mName, createGaugeMetricBuilder(value));
	}
	
	private static void track(String name, int value, final MetricRegistry registry, final Map<String, String> commonTags) {
		MetricName mName = MetricRegistry.name("jvm", name).tagged(commonTags);
		registry.remove(mName);
		registry.getOrAdd(mName, createGaugeMetricBuilder(value));
	}
	
	private static MetricBuilder<Gauge<Long>> createGaugeMetricBuilder(long value){
		return new MetricBuilder<Gauge<Long>>() {
	        @Override
	        public Gauge<Long> newMetric() {
	            return () -> Long.valueOf(value);
	        }

	        @Override
	        public boolean isInstance(Metric metric) {
	            return Gauge.class.isInstance(metric);
	        }
	    };
	}
	
	private static MetricBuilder<Gauge<Double>> createGaugeMetricBuilder(Double value){
		return new MetricBuilder<Gauge<Double>>() {
	        @Override
	        public Gauge<Double> newMetric() {
	            return () -> value;
	        }

	        @Override
	        public boolean isInstance(Metric metric) {
	            return Gauge.class.isInstance(metric);
	        }
	    };
	}
	
	private static MetricBuilder<Gauge<Integer>> createGaugeMetricBuilder(int value){
		return new MetricBuilder<Gauge<Integer>>() {
	        @Override
	        public Gauge<Integer> newMetric() {
	            return () -> Integer.valueOf(value);
	        }

	        @Override
	        public boolean isInstance(Metric metric) {
	            return Gauge.class.isInstance(metric);
	        }
	    };
	}
	
}
