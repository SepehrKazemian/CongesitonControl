package net.floodlightcontroller.congestionControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
import net.floodlightcontroller.topology.ITopologyService;

public class CongestionControl implements IOFMessageListener, IFloodlightModule {

	protected IStaticEntryPusherService flowPusher;
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected boolean enable;
	protected IOFSwitchService switchService;
	protected IDeviceService deviceService;
	protected OFPort OFPort;
	protected ITopologyService topologyProvider;

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
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
	{
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IOFSwitchService.class);
		l.add(IDeviceService.class);
		l.add(ITopologyService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		macAddresses = new ConcurrentSkipListSet<Long>();
		logger = (Logger) LoggerFactory.getLogger(CongestionControl.class);
		flowPusher = context.getServiceImpl(IStaticEntryPusherService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
		topologyProvider = context.getServiceImpl(ITopologyService.class);
		enable=true;
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		//switchService.addOFSwitchListener((IOFSwitchListener) this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx)
	{

		switch (msg.getType())
		{
		case PACKET_IN:
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			MacAddress srcMac = eth.getSourceMACAddress();
			MacAddress desMac = eth.getDestinationMACAddress();
			VlanVid vlanId = VlanVid.ofVlan(eth.getVlanID());

			if (eth.getEtherType() == EthType.IPv4)
			{

				IPv4 ipv4 = (IPv4) eth.getPayload();

				byte[] ipOptions = ipv4.getOptions();
				IPv4Address dstIp = ipv4.getDestinationAddress();
				IPv4Address srcIP = ipv4.getSourceAddress();

				//				if (ipv4.getProtocol() == IpProtocol.TCP)
				//				{
				//
				//					TCP tcp = (TCP) ipv4.getPayload();
				//					TransportPort srcPort = tcp.getSourcePort();
				//					TransportPort dstPort = tcp.getDestinationPort();
				//					short flags = tcp.getFlags();
				//
				//					/* Your logic here! */
				//				}

				if (ipv4.getProtocol().equals(IpProtocol.UDP))
				{

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
						sb.append(s1 + "\t");
					}

					System.out.println(sb);

				}


			}
			break;

		default:
			break;

		}

		return Command.CONTINUE;

	}
}