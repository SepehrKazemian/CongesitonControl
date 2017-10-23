package net.floodlightcontroller.congestionControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketQueue;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFQueueGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFQueueGetConfigRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import ch.qos.logback.classic.Logger;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFMessageWriter;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.SwitchMessagePair;
import net.floodlightcontroller.core.web.SwitchStatisticsResource;
import net.floodlightcontroller.debugcounter.IDebugCounter;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;

public class CongestionControl implements CongestionControlService, IOFMessageListener, IFloodlightModule, IOFSwitchListener 
{

	protected IStaticFlowEntryPusherService flowPusher;
	protected IFloodlightProviderService floodlightProvider;
	protected ILinkDiscoveryService linkDiscoverer;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected boolean enable;
	protected IOFSwitchService switchService;
	protected IOFSwitchListener switchListener;
	protected IDeviceService deviceService;
	protected OFPort OFPort;
	protected ITopologyService topologyProvider;
	protected IStatisticsService statisticsServ;
	protected ConcurrentCircularBuffer<SwitchMessagePair> buffer;
	protected IRestApiService restApi;
	protected IOFMessageWriter msgWriter;
	protected SwitchStatisticsResource statistics;
	protected IDebugCounterService debugCounterService;
	private IDebugCounter counterPacketOut;
	protected IRoutingService routingEngineService;
	protected int PTPriority = 0;
	private IThreadPoolService threadPool;
	// more flow-mod defaults
	protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	protected static short FLOWMOD_PRIORITY = 100;

	SwitchPort[] sp= null;
	DatapathId srcDPID = null;
	DatapathId dstDPID = null;
	OFPort srcSwtichPort = null;
	OFPort dstSwitchPort = null;

	boolean delay_ping = false;

	protected boolean threads = true;
	protected int LinkOutQ1;
	protected int LinkOutQ2;
	protected int LinkIn;

	long AggTX;
	long AggTXVal;

	long inportVal_rx = 0;
	long inportVal_now = 0;
	long inportVal_pre = 0;

	long outportVal_tx = 0;
	long outportVal_now = 0;
	long outportVal_pre = 0;

	protected static long queue_buffer = 9999999;

	long BandWidth_Kb = 10 ;
	long queue_pkt = 30;


	long pktSize = 0;


	int counter = 0 ;

	// for managing our map sizes
	protected static final int MAX_MACS_PER_SWITCH  = 1000;


	private Map<NodePortTuple, Set<Link>> switchPortLinks;


	private Set<NodePortTuple> portSet;

	protected Map<NodePortTuple,IsCongested> portCondition;
	int threshold;
	List<OFPortStatsReply> statsReply;

	public class IsCongested
	{

		boolean isCongested;

		public IsCongested(boolean isCongested)
		{
			this.isCongested = isCongested;
		}
	}

	@Override
	public String getName()
	{
		return CongestionControl.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name)
	{
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices()
	{
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(CongestionControlService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
	{
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(CongestionControlService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
	{
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IOFSwitchService.class);
		l.add(IDeviceService.class);
		l.add(IRestApiService.class);
		l.add(ITopologyService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		//		switchListener = context.getServiceImpl(IOFSwitchListener.class);
		//		floodlightProvider.addOFSwitchListener(this);
		statisticsServ = context.getServiceImpl(IStatisticsService.class);
		linkDiscoverer = context.getServiceImpl(ILinkDiscoveryService.class);
		threadPool = context.getServiceImpl(IThreadPoolService.class);
		//		this.linkDiscoverer.addListener((ILinkDiscoveryListener) this);
		macAddresses = new ConcurrentSkipListSet<Long>();
		logger = (Logger) LoggerFactory.getLogger(CongestionControl.class);
		flowPusher = context.getServiceImpl(IStaticFlowEntryPusherService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
		topologyProvider = context.getServiceImpl(ITopologyService.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		//msgWriter = context.getServiceImpl(IOFMessageWriter.class);

		buffer = new ConcurrentCircularBuffer<SwitchMessagePair>(SwitchMessagePair.class, 100);
		enable=true;
		portCondition = new ConcurrentHashMap<NodePortTuple,IsCongested>();

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApi.addRestletRoutable(new CongestionControlWebRoutable());
		switchService.addOFSwitchListener(this);
		statisticsServ.collectStatistics(enable);
		statisticsServ.getQueueStats();
		queue_buffer = 9999999;


		//		new Thread(){
		//
		//
		//			@Override
		//			public void run() {
		//
		//				while(true)
		//				{
		//					try {
		//						sleep(1000);
		//					} catch (InterruptedException e) {
		//						// TODsO Auto-generated catch block
		//						e.printStackTrace();
		//					}
		//					System.out.println(statisticsServ.getQueueStats().toString());
		//
		//				}
		//
		//			};
		//
		//		}.start();
	}



	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx)
	{

		switch (msg.getType())
		{

		case PACKET_IN:
			buffer.add(new SwitchMessagePair(sw, msg));
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			MacAddress srcMac = eth.getSourceMACAddress();
			MacAddress desMac = eth.getDestinationMACAddress();
			VlanVid vlanId = VlanVid.ofVlan(eth.getVlanID());
			IPacket ethData = eth.getPayload();
			byte[] ethArr = eth.serialize();
			//			System.out.println("ethData: " + eth.toString());

			StringBuilder strBui = new StringBuilder();

			if (eth.getEtherType() == EthType.IPv4)
			{

				IPv4 ipv4 = (IPv4) eth.getPayload();

				byte[] ipOptions = ipv4.getOptions();
				IPv4Address dstIp = ipv4.getDestinationAddress();
				IPv4Address srcIP = ipv4.getSourceAddress();


				if (ipv4.getProtocol().equals(IpProtocol.UDP))
				{

					delay_ping = true;
					System.out.println("hi");

					UDP udp = (UDP) ipv4.getPayload();
					TransportPort srcPort = udp.getSourcePort();
					TransportPort dstPort = udp.getDestinationPort();


					System.out.println("srcPot: " + srcPort);
					System.out.println("dstPot: " + dstPort);



					Data dataPkt = (Data) udp.getPayload();
					byte[] arr = dataPkt.getData();
					StringBuilder sb = new StringBuilder();

					for (byte b : arr)
					{
						String s1 = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ','0');
						sb.append(s1);
					}


					System.out.println("Version: " + RTPStaticPT.RTPVersionRecognizer(sb).Version);
					System.out.println("Marker: " + RTPStaticPT.RTPVersionRecognizer(sb).Marker);
					System.out.println("PT: " + RTPStaticPT.RTPVersionRecognizer(sb).PT);
					System.out.println("CC: " + RTPStaticPT.RTPVersionRecognizer(sb).CC);
					System.out.println("Extension: " + RTPStaticPT.RTPVersionRecognizer(sb).Extension);
					System.out.println("Padding: " + RTPStaticPT.RTPVersionRecognizer(sb).Padding);
										System.out.println("seqNumber: " + RTPStaticPT.RTPVersionRecognizer(sb).SeqNumb);
					//					System.out.println("timeStamp: " + RTPStaticPT.RTPVersionRecognizer(sb).timeStamp);


					//					//					statisticsServ.collectStatistics(true);
					//					//					System.out.println("stats: " + statisticsServ.getBandwidthConsumption());
					//
					//					System.out.println(sb);

					if (RTPStaticPT.RTPVersionRecognizer(sb).PT .equals("audio"))
					{
						PTPriority = 5;
					}

					else
					{
						PTPriority = 10;
					}

					UDPdirectFlow(sb, PTPriority, srcMac, desMac, srcIP, dstIp, srcPort, dstPort);

					//	getSwitchesStatus();

					//statisticsServ.getBandwidthConsumption();



				}


				else if(ipv4.getProtocol().equals(IpProtocol.TCP))
				{

					TCP tcp = (TCP) ipv4.getPayload();
					TransportPort srcPort = tcp.getSourcePort();
					TransportPort dstPort = tcp.getDestinationPort();
					short flags = tcp.getFlags();
					PTPriority = 3;

					TCPdirectFlow(srcMac, desMac, srcIP, dstIp, srcPort, dstPort);

				}

				else if((ipv4.getProtocol().equals(IpProtocol.ICMP)) && (delay_ping == true))
				{

					PTPriority = 1;
					System.out.println("salaaaam");

					ICMPdirectFlow(PTPriority, srcMac, desMac, srcIP, dstIp);

				}

			}
			break;

		default:
			break;

		}

		return Command.CONTINUE;

	}






	private void ICMPdirectFlow(int PTPriority, MacAddress srcMAC, MacAddress dstMAC, IPv4Address srcIP, IPv4Address dstIP) 
	{
		if(srcIP.toString().equals("10.0.0.1") ) 
		{

			System.out.println("request");

			OFFactory factory = switchService.getSwitch(DatapathId.of(1)).getOFFactory();

			Match match = factory.buildMatch()
					.setExact(MatchField.IN_PORT, OFPort.of(1))
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
					.build();

			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
			OFActions actions = factory.actions();
			OFOxms oxms = factory.oxms();

			OFActionOutput acOutput = actions.buildOutput()
					.setPort(OFPort.of(2))
					.build();
			actionList.add(acOutput);

			OFFlowAdd addFlow = factory.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match)
					.setActions(actionList)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(5)
					.build();

			IOFSwitch OFSwitch = switchService.getSwitch(DatapathId.of(1));
			OFSwitch.write(addFlow);
			//			System.out.println("areeeeee");


			OFFactory factory2 = switchService.getSwitch(DatapathId.of(2)).getOFFactory();

			Match match2 = factory2.buildMatch()
					.setExact(MatchField.IN_PORT, OFPort.of(1))
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
					.build();

			ArrayList<OFAction> actionList2 = new ArrayList<OFAction>();
			OFActions actions2 = factory2.actions();
			OFOxms oxms2 = factory2.oxms();

			OFActionOutput acOutput2 = actions2.buildOutput()
					.setPort(OFPort.of(4))
					.build();
			actionList2.add(acOutput2);

			OFFlowAdd addFlow2 = factory2.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match2)
					.setActions(actionList2)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(5)
					.build();

			IOFSwitch OFSwitch2 = switchService.getSwitch(DatapathId.of(2));
			OFSwitch2.write(addFlow2);
		}


		if(srcIP.toString().equals("10.0.0.2") ) 
		{

			System.out.println("reply");

			OFFactory factory = switchService.getSwitch(DatapathId.of(2)).getOFFactory();

			Match match = factory.buildMatch()
					.setExact(MatchField.IN_PORT, OFPort.of(4))
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
					.build();

			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
			OFActions actions = factory.actions();
			OFOxms oxms = factory.oxms();

			OFActionOutput acOutput = actions.buildOutput()
					.setPort(OFPort.of(2))
					.build();
			actionList.add(acOutput);

			OFFlowAdd addFlow = factory.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match)
					.setActions(actionList)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(5)
					.build();

			IOFSwitch OFSwitch = switchService.getSwitch(DatapathId.of(2));
			OFSwitch.write(addFlow);
			//			System.out.println("areeeeee");


			OFFactory factory2 = switchService.getSwitch(DatapathId.of(1)).getOFFactory();

			Match match2 = factory2.buildMatch()
					.setExact(MatchField.IN_PORT, OFPort.of(3))
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
					.build();

			ArrayList<OFAction> actionList2 = new ArrayList<OFAction>();
			OFActions actions2 = factory2.actions();
			OFOxms oxms2 = factory2.oxms();

			OFActionOutput acOutput2 = actions2.buildOutput()
					.setPort(OFPort.of(1))
					.build();
			actionList2.add(acOutput2);

			OFFlowAdd addFlow2 = factory2.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match2)
					.setActions(actionList2)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(5)
					.build();

			IOFSwitch OFSwitch2 = switchService.getSwitch(DatapathId.of(1));
			OFSwitch2.write(addFlow2);
		}

	}

	private void UDPdirectFlow(StringBuilder sb, int PTPriority, MacAddress srcMAC, MacAddress dstMAC, IPv4Address srcIP,IPv4Address dstIP, TransportPort srcPort, TransportPort dstPort)
	{


		System.out.println("dst port: " + dstPort);

		long q = 1;

		//first we will got a collection of hosts and then we use a loop to check all of these devices
		//once it was not empty we will got all of its IP addresses and we will check them with our
		//src and dst IP s.
		//if they matched, so we will get its device attachments. ok then, we have access to its switches
		//then we will get its switches Datapath ID and its switch port
		Iterable<? extends IDevice> hosts = deviceService.getAllDevices();
		for (IDevice device : hosts)
		{
			System.out.println("device : " + device);
			System.out.println("hosts : " + hosts);

			if (device != null)
			{
				IPv4Address[] IPs = device.getIPv4Addresses();
				System.out.println("blahblah");


				for (IPv4Address n : IPs)
				{

					if (n.equals(srcIP))
					{
						sp = device.getAttachmentPoints();
						for (SwitchPort x : sp)
						{
							srcDPID = x.getSwitchDPID();
							System.out.println("srcDPID: " + srcDPID);
							srcSwtichPort = x.getPort();
							System.out.println("srcSwitchPort: " + srcSwtichPort);
						}
					}

					if (n.equals(dstIP))
					{
						sp = device.getAttachmentPoints();
						for (SwitchPort x : sp)
						{
							dstDPID = x.getSwitchDPID();
							System.out.println("dstDPID: " + dstDPID);
							dstSwitchPort = x.getPort();					
							System.out.println("dstSwitchPort: " + dstSwitchPort);
						}
					}
				}
			}
		}


		if(threads == true)
		{
			System.out.println("hoooy threaddddddddddddddddddddddddddddd");
			queue_buffer = 9999999; 
			threads = false;
			new Thread(){

				@Override
				public void run() 
				{

					while(true)
					{
						try {
							sleep(3000);
						} catch (InterruptedException e) {
							// TODsO Auto-generated catch block
							e.printStackTrace();
						}


						//						System.out.println(statisticsServ.getBandwidthConsumption().get(new NodePortTuple(dstDPID, dstSwitchPort)).toStringVars().tx);
						inportVal_now = statisticsServ.getBandwidthConsumption().get(new NodePortTuple(DatapathId.of(1), OFPort.of(1))).toStringVars().rxValue;
						inportVal_rx = inportVal_now - inportVal_pre;
						inportVal_pre = inportVal_now;
						System.out.println("inportVal: " + inportVal_rx);
						//						System.out.println("rxVal: " + statisticsServ.getBandwidthConsumption().get(new NodePortTuple(DatapathId.of(1), OFPort.of(1))).toStringVars().rxValue);

						outportVal_now = statisticsServ.getBandwidthConsumption().get(new NodePortTuple(DatapathId.of(1), OFPort.of(2))).toStringVars().txValue;
						outportVal_tx = outportVal_now - outportVal_pre;
						outportVal_pre = outportVal_now;
						System.out.println("outportVal: " + outportVal_tx);


						queue_buffer = queue_buffer - (inportVal_rx - outportVal_tx);

						System.out.println("queue buffer: " + queue_buffer);


						//						System.out.println("r=txVal: " + statisticsServ.getBandwidthConsumption().get(new NodePortTuple(DatapathId.of(1), OFPort.of(2))).toStringVars().txValue);

						//						System.out.println(statisticsServ.getBandwidthConsumption().get(new NodePortTuple(dstDPID, dstSwitchPort)).toStringVars().txValue);
						//						System.out.println(statisticsServ.getBandwidthConsumption().get(new NodePortTuple(srcDPID, srcSwtichPort)).toStringVars().rx);
						//						System.out.println(statisticsServ.getBandwidthConsumption().get(new NodePortTuple(srcDPID, srcSwtichPort)).toStringVars().rxValue);

						System.out.println("blah blah blah");
						//						AggTXVal += statisticsServ.getBandwidthConsumption().get(new NodePortTuple(dstDPID, dstSwitchPort)).toStringVars().txValue;
						//						AggTX += statisticsServ.getBandwidthConsumption().get(new NodePortTuple(dstDPID, dstSwitchPort)).toStringVars().tx;
						//						System.out.println("aggregateTX"+AggTX);
					}

				};

			}.start();
		}






		//here the only thing that we should consider is our situations which we will encounter
		if (srcDPID != dstDPID)
		{
			OFFactory factory = switchService.getSwitch(DatapathId.of(srcDPID.toString())).getOFFactory();

			Match match = factory.buildMatch()
					.setExact(MatchField.IN_PORT, srcSwtichPort)
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_DST, dstPort)
					.setExact(MatchField.UDP_SRC, srcPort)
					.build();

			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
			OFActions actions = factory.actions();
			OFOxms oxms = factory.oxms();

			OFActionOutput acOutput = actions.buildOutput()
					.setPort(OFPort.of(2))
					.build();
			actionList.add(acOutput);

			OFFlowAdd addFlow = factory.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match)
					.setActions(actionList)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					.build();

			IOFSwitch OFSwitch = switchService.getSwitch(DatapathId.of(srcDPID.toString()));
			OFSwitch.write(addFlow);

			OFFactory factory2 = switchService.getSwitch(DatapathId.of(dstDPID.toString())).getOFFactory();

			Match match2 = factory2.buildMatch()
					.setExact(MatchField.IN_PORT, OFPort.of(1))
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_DST, dstPort)
					.setExact(MatchField.UDP_SRC, srcPort)
					.build();

			ArrayList<OFAction> actionList2 = new ArrayList<OFAction>();
			OFActions actions2 = factory2.actions();
			OFOxms oxms2 = factory2.oxms();

			OFActionOutput acOutput2 = actions2.buildOutput()
					.setPort(OFPort.of(4))
					.build();
			actionList2.add(acOutput2);

			OFFlowAdd addFlow2 = factory2.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match2)
					.setActions(actionList2)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					.build();

			IOFSwitch OFSwitch2 = switchService.getSwitch(DatapathId.of(dstDPID.toString()));
			OFSwitch2.write(addFlow2);
		}


	}


	private void TCPdirectFlow(MacAddress srcMAC, MacAddress dstMAC, IPv4Address srcIP,IPv4Address dstIP, TransportPort srcPort, TransportPort dstPort)
	{


		System.out.println("dst port: " + dstPort);

		long q = 1;

		//first we will got a collection of hosts and then we use a loop to check all of these devices
		//once it was not empty we will got all of its IP addresses and we will check them with our
		//src and dst IP s.
		//if they matched, so we will get its device attachments. ok then, we have access to its switches
		//then we will get its switches Datapath ID and its switch port
		Iterable<? extends IDevice> hosts = deviceService.getAllDevices();
		for (IDevice device : hosts)
		{
			System.out.println("device : " + device);
			System.out.println("hosts : " + hosts);

			if (device != null)
			{
				IPv4Address[] IPs = device.getIPv4Addresses();
				System.out.println("blahblah");


				for (IPv4Address n : IPs)
				{

					if (n.equals(srcIP))
					{
						sp = device.getAttachmentPoints();
						for (SwitchPort x : sp)
						{
							srcDPID = x.getSwitchDPID();
							System.out.println("srcDPID: " + srcDPID);
							srcSwtichPort = x.getPort();
							System.out.println("srcSwitchPort: " + srcSwtichPort);
						}
					}

					if (n.equals(dstIP))
					{
						sp = device.getAttachmentPoints();
						for (SwitchPort x : sp)
						{
							dstDPID = x.getSwitchDPID();
							System.out.println("dstDPID: " + dstDPID);
							dstSwitchPort = x.getPort();					
							System.out.println("dstSwitchPort: " + dstSwitchPort);
						}
					}
				}
			}
		}


		//here the only thing that we should consider is our situations which we will encounter
		if (srcDPID != dstDPID)
		{
			OFFactory factory = switchService.getSwitch(DatapathId.of(srcDPID.toString())).getOFFactory();

			Match match = factory.buildMatch()
					.setExact(MatchField.IN_PORT, srcSwtichPort)
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_DST, dstPort)
					.setExact(MatchField.UDP_SRC, srcPort)
					.build();

			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
			OFActions actions = factory.actions();
			OFOxms oxms = factory.oxms();

			OFActionOutput acOutput = actions.buildOutput()
					.setPort(OFPort.of(4))
					.build();
			actionList.add(acOutput);

			OFFlowAdd addFlow = factory.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match)
					.setActions(actionList)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					.build();

			IOFSwitch OFSwitch = switchService.getSwitch(DatapathId.of(srcDPID.toString()));
			OFSwitch.write(addFlow);

			OFFactory factory2 = switchService.getSwitch(DatapathId.of(dstDPID.toString())).getOFFactory();

			Match match2 = factory2.buildMatch()
					.setExact(MatchField.IN_PORT, OFPort.of(3))
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.ETH_DST, dstMAC)
					.setExact(MatchField.ETH_SRC, srcMAC)
					.setExact(MatchField.IPV4_SRC, srcIP)
					.setExact(MatchField.IPV4_DST, dstIP)
					.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_DST, dstPort)
					.setExact(MatchField.UDP_SRC, srcPort)
					.build();

			ArrayList<OFAction> actionList2 = new ArrayList<OFAction>();
			OFActions actions2 = factory2.actions();
			OFOxms oxms2 = factory2.oxms();

			OFActionOutput acOutput2 = actions2.buildOutput()
					.setPort(OFPort.of(4))
					.build();
			actionList2.add(acOutput2);

			OFFlowAdd addFlow2 = factory2.buildFlowAdd()
					.setPriority(PTPriority)
					.setMatch(match2)
					.setActions(actionList2)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					.build();

			IOFSwitch OFSwitch2 = switchService.getSwitch(DatapathId.of(dstDPID.toString()));
			OFSwitch2.write(addFlow2);
		}


	}



	public void getSwitchesStatus()
	{

		OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
		OFQueueGetConfigRequest cr = factory.buildQueueGetConfigRequest().setPort(OFPort.ANY).build();
		ListenableFuture<OFQueueGetConfigReply> future = 
				switchService.getSwitch(DatapathId.of(1))
				.writeRequest(cr);
		//		request = flowFactory.buildFlowStatsRequest();
		try {

			@SuppressWarnings("unchecked")
			OFQueueGetConfigReply reply = future.get(10, TimeUnit.SECONDS);

			// Get statistics of interest
			for (OFPacketQueue q : reply.getQueues()) 
			{
				OFPort p = q.getPort(); /* The switch port the queue is on */
				long id = q.getQueueId();
				System.out.println("port: " + p);
				System.out.println("id: " + id);
			}
		}

		catch (Exception e) 
		{
			logger.warn("*** FlowMonitor: An error occurred while polling switch: -- ");
		}
	}


	@Override
	public ConcurrentCircularBuffer<SwitchMessagePair> getBuffer() {
		return buffer;
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// TODO Auto-generated method stub


	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub

	}



}

