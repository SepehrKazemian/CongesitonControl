package net.floodlightcontroller.statistics;

<<<<<<< HEAD
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;
=======
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFQueueStatsEntry;
import org.projectfloodlight.openflow.protocol.OFQueueStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.ver13.OFMeterSerializerVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;

>>>>>>> Completed project
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
<<<<<<< HEAD
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.statistics.web.SwitchStatisticsWebRoutable;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.ver13.OFMeterSerializerVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.State;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
=======
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.statistics.web.SwitchStatisticsWebRoutable;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.NodePortTuple;
>>>>>>> Completed project

public class StatisticsCollector implements IFloodlightModule, IStatisticsService {
	private static final Logger log = LoggerFactory.getLogger(StatisticsCollector.class);

<<<<<<< HEAD
=======
	private static OFStatsType StatisticType;

>>>>>>> Completed project
	private static IOFSwitchService switchService;
	private static IThreadPoolService threadPoolService;
	private static IRestApiService restApiService;

	private static boolean isEnabled = false;
<<<<<<< HEAD
	
	private static int portStatsInterval = 10; /* could be set by REST API, so not final */
=======

	private static int portStatsInterval = 3; /* could be set by REST API, so not final */
>>>>>>> Completed project
	private static ScheduledFuture<?> portStatsCollector;

	private static final long BITS_PER_BYTE = 8;
	private static final long MILLIS_PER_SEC = 1000;
<<<<<<< HEAD
	
=======

>>>>>>> Completed project
	private static final String INTERVAL_PORT_STATS_STR = "collectionIntervalPortStatsSeconds";
	private static final String ENABLED_STR = "enable";

	private static final HashMap<NodePortTuple, SwitchPortBandwidth> portStats = new HashMap<NodePortTuple, SwitchPortBandwidth>();
	private static final HashMap<NodePortTuple, SwitchPortBandwidth> tentativePortStats = new HashMap<NodePortTuple, SwitchPortBandwidth>();

<<<<<<< HEAD
=======
	private static final HashMap<NodePortTuple, SwitchQueues> QueueStats = new HashMap<NodePortTuple, SwitchQueues>();
	private static final HashMap<NodePortTuple, SwitchQueues> tentativeQueueStats = new HashMap<NodePortTuple, SwitchQueues>();

>>>>>>> Completed project
	/**
	 * Run periodically to collect all port statistics. This only collects
	 * bandwidth stats right now, but it could be expanded to record other
	 * information as well. The difference between the most recent and the
	 * current RX/TX bytes is used to determine the "elapsed" bytes. A 
	 * timestamp is saved each time stats results are saved to compute the
	 * bits per second over the elapsed time. There isn't a better way to
	 * compute the precise bandwidth unless the switch were to include a
	 * timestamp in the stats reply message, which would be nice but isn't
	 * likely to happen. It would be even better if the switch recorded 
	 * bandwidth and reported bandwidth directly.
	 * 
	 * Stats are not reported unless at least two iterations have occurred
	 * for a single switch's reply. This must happen to compare the byte 
	 * counts and to get an elapsed time.
	 * 
	 * @author Ryan Izard, ryan.izard@bigswitch.com, rizard@g.clemson.edu
	 *
	 */
	private class PortStatsCollector implements Runnable {

		@Override
		public void run() {
			Map<DatapathId, List<OFStatsReply>> replies = getSwitchStatistics(switchService.getAllSwitchDpids(), OFStatsType.PORT);
<<<<<<< HEAD
			for (Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
				for (OFStatsReply r : e.getValue()) {
					OFPortStatsReply psr = (OFPortStatsReply) r;
					for (OFPortStatsEntry pse : psr.getEntries()) {
						NodePortTuple npt = new NodePortTuple(e.getKey(), pse.getPortNo());
						SwitchPortBandwidth spb;
						if (portStats.containsKey(npt) || tentativePortStats.containsKey(npt)) {
							if (portStats.containsKey(npt)) { /* update */
								spb = portStats.get(npt);
							} else if (tentativePortStats.containsKey(npt)) { /* finish */
								spb = tentativePortStats.get(npt);
								tentativePortStats.remove(npt);
							} else {
								log.error("Inconsistent state between tentative and official port stats lists.");
								return;
							}

							/* Get counted bytes over the elapsed period. Check for counter overflow. */
							U64 rxBytesCounted;
							U64 txBytesCounted;
							if (spb.getPriorByteValueRx().compareTo(pse.getRxBytes()) > 0) { /* overflow */
								U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueRx());
								U64 lower = pse.getRxBytes();
								rxBytesCounted = upper.add(lower);
							} else {
								rxBytesCounted = pse.getRxBytes().subtract(spb.getPriorByteValueRx());
							}
							if (spb.getPriorByteValueTx().compareTo(pse.getTxBytes()) > 0) { /* overflow */
								U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueTx());
								U64 lower = pse.getTxBytes();
								txBytesCounted = upper.add(lower);
							} else {
								txBytesCounted = pse.getTxBytes().subtract(spb.getPriorByteValueTx());
							}
							IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
							long speed = 0;
							if (sw != null) { /* could have disconnected; we'll assume zero-speed then */
								speed = sw.getPort(npt.getPortId()).getCurrSpeed();
							}
							long timeDifSec = (System.currentTimeMillis() - spb.getUpdateTime()) / MILLIS_PER_SEC;
							portStats.put(npt, SwitchPortBandwidth.of(npt.getNodeId(), npt.getPortId(), 
									U64.ofRaw(speed),
									U64.ofRaw((rxBytesCounted.getValue() * BITS_PER_BYTE) / timeDifSec), 
									U64.ofRaw((txBytesCounted.getValue() * BITS_PER_BYTE) / timeDifSec), 
									pse.getRxBytes(), pse.getTxBytes())
									);
							
						} else { /* initialize */
							tentativePortStats.put(npt, SwitchPortBandwidth.of(npt.getNodeId(), npt.getPortId(), U64.ZERO, U64.ZERO, U64.ZERO, pse.getRxBytes(), pse.getTxBytes()));
						}
					}
				}
			}
		}
	}

=======
			switch (StatisticType)
			{
			case QUEUE:
				for (Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
					for (OFStatsReply r : e.getValue()) {
						OFQueueStatsReply psr = (OFQueueStatsReply) r;
						for (OFQueueStatsEntry pse : psr.getEntries()) {
							NodePortTuple npt = new NodePortTuple(e.getKey(), pse.getQueueId() , pse.getPortNo());
							//							System.out.println("queue: " + pse.getQueueId());
							//							System.out.println("psr: "+ psr.getEntries());
							//							System.out.println("e: " + e.getKey());
							//							System.out.println("pse: " + pse.getPortNo());
							System.out.println("npttt: "+ npt);
							//							System.out.println("containQueue: " + QueueStats.containsKey(npt));
							//							System.out.println("containTentative: " + tentativeQueueStats.containsKey(npt));
							SwitchQueues spb;
							if (QueueStats.containsKey(npt) || tentativeQueueStats.containsKey(npt)) {
								if (QueueStats.containsKey(npt)) { /* update */
									spb = QueueStats.get(npt);
									System.out.println("npttttttt: " + npt);
									//									System.out.println("spb: " + spb);
								} else if (tentativeQueueStats.containsKey(npt)) { /* finish */
									spb = tentativeQueueStats.get(npt);
									//									System.out.println("adfasdsdf-------------------------------------");
									//									System.out.println("spb: " + spb);
									//									System.out.println("adfasdsdf+++++++++++++++++++++++++++++++++++++");
									tentativeQueueStats.remove(npt);
								} else {
									log.error("Inconsistent state between tentative and official port stats lists.");
									return;
								}

								/* Get counted bytes over the elapsed period. Check for counter overflow. */
								U64 txBytesCounted;
								//								if (spb.getPriorByteValueRx().compareTo(pse.getRxBytes()) > 0) { /* overflow */
								//									U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueRx());
								//									U64 lower = pse.getRxBytes();
								//									rxBytesCounted = upper.add(lower);
								//								} else {
								//									rxBytesCounted = pse.getRxBytes().subtract(spb.getPriorByteValueRx());
								//								}
								if (spb.getPriorByteValueTx().compareTo(pse.getTxBytes()) > 0) { /* overflow */
									U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueTx());
									U64 lower = pse.getTxBytes();
									txBytesCounted = upper.add(lower);
								} 
								else 
								{
									txBytesCounted = pse.getTxBytes().subtract(spb.getPriorByteValueTx());
								}
								IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
								System.out.println("npt: " + npt.getQueueId());

								System.out.println(QueueStats.keySet());

								System.out.println("npt: " + npt.getQueueId());

								QueueStats.put(npt, 
										SwitchQueues.of(npt.getNodeId(), 
												npt.getPortId(), npt.getQueueId(), 
												U64.ZERO, pse.getTxBytes()));
								System.out.println("qqqqqq: " +QueueStats);

							} 
							else 
							{ /* initialize */
								System.out.println("npt:" + npt + " getNodeId " + npt.getNodeId() + " getPortId " + npt.getPortId() + " getQueueId " + npt.getQueueId() + " U64 " + U64.ZERO + " txByte " + pse.getTxBytes());
								tentativeQueueStats.put(npt, 
										SwitchQueues.of(npt.getNodeId(), 
												npt.getPortId(), npt.getQueueId(), 
												U64.ZERO, pse.getTxBytes()));
								System.out.println("blah blah " + tentativeQueueStats);

							}
						}
					}
				}
				break;

			case PORT:
				for (Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
					for (OFStatsReply r : e.getValue()) {
						OFPortStatsReply psr = (OFPortStatsReply) r;
						for (OFPortStatsEntry pse : psr.getEntries()) {
							NodePortTuple npt = new NodePortTuple(e.getKey(), pse.getPortNo());
							SwitchPortBandwidth spb;
							if (portStats.containsKey(npt) || tentativePortStats.containsKey(npt)) {
								if (portStats.containsKey(npt)) { /* update */
									spb = portStats.get(npt);
								} else if (tentativePortStats.containsKey(npt)) { /* finish */
									spb = tentativePortStats.get(npt);
									tentativePortStats.remove(npt);
								} else {
									log.error("Inconsistent state between tentative and official port stats lists.");
									return;
								}

								/* Get counted bytes over the elapsed period. Check for counter overflow. */
								U64 rxBytesCounted;
								U64 txBytesCounted;
								if (spb.getPriorByteValueRx().compareTo(pse.getRxBytes()) > 0) { /* overflow */
									U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueRx());
									U64 lower = pse.getRxBytes();
									rxBytesCounted = upper.add(lower);
								} else {
									rxBytesCounted = pse.getRxBytes().subtract(spb.getPriorByteValueRx());
								}
								if (spb.getPriorByteValueTx().compareTo(pse.getTxBytes()) > 0) { /* overflow */
									U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueTx());
									U64 lower = pse.getTxBytes();
									txBytesCounted = upper.add(lower);
								} else {
									txBytesCounted = pse.getTxBytes().subtract(spb.getPriorByteValueTx());
								}
								IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
								long speed = 0;
								if (sw != null) { /* could have disconnected; we'll assume zero-speed then */
									speed = sw.getPort(npt.getPortId()).getCurrSpeed();
								}
								long timeDifSec = (System.currentTimeMillis() - spb.getUpdateTime()) / MILLIS_PER_SEC;
								portStats.put(npt, SwitchPortBandwidth.of(npt.getNodeId(), npt.getPortId(), 
										U64.ofRaw((rxBytesCounted.getValue() * BITS_PER_BYTE) / timeDifSec), 
										U64.ofRaw((txBytesCounted.getValue() * BITS_PER_BYTE) / timeDifSec), 
										pse.getRxBytes(), pse.getTxBytes())
										);

							} else { /* initialize */
								try{
									System.out.println("adfasdfsdf");
									tentativePortStats.put(npt, SwitchPortBandwidth.of(npt.getNodeId(), npt.getPortId(), U64.ZERO, U64.ZERO, pse.getRxBytes(), pse.getTxBytes()));
								}
								catch (Exception bb) {
									bb.printStackTrace();
									// TODO: handle exception
								}
							}
						}
					}
				}
				break;
			}

		}
	}
>>>>>>> Completed project
	/**
	 * Single thread for collecting switch statistics and
	 * containing the reply.
	 * 
	 * @author Ryan Izard, ryan.izard@bigswitch.com, rizard@g.clemson.edu
	 *
	 */
	private class GetStatisticsThread extends Thread {
		private List<OFStatsReply> statsReply;
		private DatapathId switchId;
		private OFStatsType statType;

		public GetStatisticsThread(DatapathId switchId, OFStatsType statType) {
			this.switchId = switchId;
			this.statType = statType;
			this.statsReply = null;
		}

		public List<OFStatsReply> getStatisticsReply() {
			return statsReply;
		}

		public DatapathId getSwitchId() {
			return switchId;
		}

		@Override
		public void run() {
			statsReply = getSwitchStatistics(switchId, statType);
		}
	}
<<<<<<< HEAD
	
	/*
	 * IFloodlightModule implementation
	 */
	
=======

	/*
	 * IFloodlightModule implementation
	 */

>>>>>>> Completed project
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IStatisticsService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IStatisticsService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		l.add(IThreadPoolService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		switchService = context.getServiceImpl(IOFSwitchService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);

		Map<String, String> config = context.getConfigParams(this);
		if (config.containsKey(ENABLED_STR)) {
			try {
				isEnabled = Boolean.parseBoolean(config.get(ENABLED_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", ENABLED_STR, isEnabled);
			}
		}
		log.info("Statistics collection {}", isEnabled ? "enabled" : "disabled");

		if (config.containsKey(INTERVAL_PORT_STATS_STR)) {
			try {
				portStatsInterval = Integer.parseInt(config.get(INTERVAL_PORT_STATS_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", INTERVAL_PORT_STATS_STR, portStatsInterval);
			}
		}
		log.info("Port statistics collection interval set to {}s", portStatsInterval);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApiService.addRestletRoutable(new SwitchStatisticsWebRoutable());
		if (isEnabled) {
			startStatisticsCollection();
		}
	}

	/*
	 * IStatisticsService implementation
	 */
<<<<<<< HEAD
	
=======

>>>>>>> Completed project
	@Override
	public SwitchPortBandwidth getBandwidthConsumption(DatapathId dpid, OFPort p) {
		return portStats.get(new NodePortTuple(dpid, p));
	}
<<<<<<< HEAD
	
=======
	@Override
	public SwitchQueues getQueueStats(DatapathId dpid, long qId, OFPort p) {
		return QueueStats.get(new NodePortTuple(dpid, qId, p));
	}

>>>>>>> Completed project

	@Override
	public Map<NodePortTuple, SwitchPortBandwidth> getBandwidthConsumption() {
		return Collections.unmodifiableMap(portStats);
	}

	@Override
<<<<<<< HEAD
=======
	public Map<NodePortTuple, SwitchQueues> getQueueStats() {
		System.out.println("QQQQ" + QueueStats);
		return Collections.unmodifiableMap(QueueStats);
	}

	@Override
>>>>>>> Completed project
	public synchronized void collectStatistics(boolean collect) {
		if (collect && !isEnabled) {
			startStatisticsCollection();
			isEnabled = true;
		} else if (!collect && isEnabled) {
			stopStatisticsCollection();
			isEnabled = false;
		} 
		/* otherwise, state is not changing; no-op */
	}
<<<<<<< HEAD
	
	/*
	 * Helper functions
	 */
	
=======

	/*
	 * Helper functions
	 */

>>>>>>> Completed project
	/**
	 * Start all stats threads.
	 */
	private void startStatisticsCollection() {
		portStatsCollector = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new PortStatsCollector(), portStatsInterval, portStatsInterval, TimeUnit.SECONDS);
		tentativePortStats.clear(); /* must clear out, otherwise might have huge BW result if present and wait a long time before re-enabling stats */
<<<<<<< HEAD
		log.warn("Statistics collection thread(s) started");
	}
	
=======
		tentativeQueueStats.clear();
		log.warn("Statistics collection thread(s) started");
	}

>>>>>>> Completed project
	/**
	 * Stop all stats threads.
	 */
	private void stopStatisticsCollection() {
		if (!portStatsCollector.cancel(false)) {
			log.error("Could not cancel port stats thread");
		} else {
			log.warn("Statistics collection thread(s) stopped");
		}
	}

	/**
	 * Retrieve the statistics from all switches in parallel.
	 * @param dpids
	 * @param statsType
	 * @return
	 */
<<<<<<< HEAD
	private Map<DatapathId, List<OFStatsReply>> getSwitchStatistics(Set<DatapathId> dpids, OFStatsType statsType) {
=======
	private Map<DatapathId, List<OFStatsReply>> getSwitchStatistics(Set<DatapathId> dpids, OFStatsType statsType) 
	{
>>>>>>> Completed project
		HashMap<DatapathId, List<OFStatsReply>> model = new HashMap<DatapathId, List<OFStatsReply>>();

		List<GetStatisticsThread> activeThreads = new ArrayList<GetStatisticsThread>(dpids.size());
		List<GetStatisticsThread> pendingRemovalThreads = new ArrayList<GetStatisticsThread>();
		GetStatisticsThread t;
<<<<<<< HEAD
		for (DatapathId d : dpids) {
=======
		for (DatapathId d : dpids) 
		{
>>>>>>> Completed project
			t = new GetStatisticsThread(d, statsType);
			activeThreads.add(t);
			t.start();
		}

		/* Join all the threads after the timeout. Set a hard timeout
		 * of 12 seconds for the threads to finish. If the thread has not
		 * finished the switch has not replied yet and therefore we won't
		 * add the switch's stats to the reply.
		 */
<<<<<<< HEAD
		for (int iSleepCycles = 0; iSleepCycles < portStatsInterval; iSleepCycles++) {
			for (GetStatisticsThread curThread : activeThreads) {
				if (curThread.getState() == State.TERMINATED) {
=======
		for (int iSleepCycles = 0; iSleepCycles < portStatsInterval; iSleepCycles++) 
		{
			for (GetStatisticsThread curThread : activeThreads) 
			{
				if (curThread.getState() == State.TERMINATED) 
				{
>>>>>>> Completed project
					model.put(curThread.getSwitchId(), curThread.getStatisticsReply());
					pendingRemovalThreads.add(curThread);
				}
			}

			/* remove the threads that have completed the queries to the switches */
<<<<<<< HEAD
			for (GetStatisticsThread curThread : pendingRemovalThreads) {
				activeThreads.remove(curThread);
			}
			
=======
			for (GetStatisticsThread curThread : pendingRemovalThreads) 
			{
				activeThreads.remove(curThread);
			}

>>>>>>> Completed project
			/* clear the list so we don't try to double remove them */
			pendingRemovalThreads.clear();

			/* if we are done finish early */
			if (activeThreads.isEmpty()) {
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting for statistics", e);
			}
		}

		return model;
	}

	/**
	 * Get statistics from a switch.
	 * @param switchId
	 * @param statsType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<OFStatsReply> getSwitchStatistics(DatapathId switchId, OFStatsType statsType) {
		IOFSwitch sw = switchService.getSwitch(switchId);
		ListenableFuture<?> future;
		List<OFStatsReply> values = null;
		Match match;
		if (sw != null) {
			OFStatsRequest<?> req = null;
<<<<<<< HEAD
=======
			StatisticType = statsType;
>>>>>>> Completed project
			switch (statsType) {
			case FLOW:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildFlowStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case AGGREGATE:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildAggregateStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case PORT:
				req = sw.getOFFactory().buildPortStatsRequest()
				.setPortNo(OFPort.ANY)
				.build();
				break;
			case QUEUE:
				req = sw.getOFFactory().buildQueueStatsRequest()
				.setPortNo(OFPort.ANY)
				.setQueueId(UnsignedLong.MAX_VALUE.longValue())
				.build();
				break;
			case DESC:
				req = sw.getOFFactory().buildDescStatsRequest()
				.build();
				break;
			case GROUP:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupStatsRequest()				
							.build();
				}
				break;

			case METER:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterStatsRequest()
							.setMeterId(OFMeterSerializerVer13.ALL_VAL)
							.build();
				}
				break;

			case GROUP_DESC:			
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupDescStatsRequest()			
							.build();
				}
				break;

			case GROUP_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupFeaturesStatsRequest()
							.build();
				}
				break;

			case METER_CONFIG:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterConfigStatsRequest()
							.build();
				}
				break;

			case METER_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterFeaturesStatsRequest()
							.build();
				}
				break;

			case TABLE:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableStatsRequest()
							.build();
				}
				break;

			case TABLE_FEATURES:	
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableFeaturesStatsRequest()
							.build();		
				}
				break;
			case PORT_DESC:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildPortDescStatsRequest()
							.build();
				}
				break;
			case EXPERIMENTER:		
			default:
				log.error("Stats Request Type {} not implemented yet", statsType.name());
				break;
			}

			try {
				if (req != null) {
<<<<<<< HEAD
					future = sw.writeStatsRequest(req); 
					values = (List<OFStatsReply>) future.get(portStatsInterval / 2, TimeUnit.SECONDS);
=======
					//					System.out.println("req: " + req);
					future = sw.writeStatsRequest(req); 
					values = (List<OFStatsReply>) future.get(portStatsInterval / 2, TimeUnit.SECONDS);
					//					System.out.println("values: " + values);
>>>>>>> Completed project
				}
			} catch (Exception e) {
				log.error("Failure retrieving statistics from switch {}. {}", sw, e);
			}
		}
		return values;
	}
}