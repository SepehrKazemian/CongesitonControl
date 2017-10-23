package net.floodlightcontroller.statistics;

import java.util.Date;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.statistics.web.SwitchPortBandwidthSerializer;

@JsonSerialize(using=SwitchPortBandwidthSerializer.class)
public class SwitchPortBandwidth {
	private DatapathId id;
	private OFPort pt;
<<<<<<< HEAD
	private U64 speed;
=======
>>>>>>> Completed project
	private U64 rx;
	private U64 tx;
	private Date time;
	private U64 rxValue;
	private U64 txValue;
<<<<<<< HEAD
	
	private SwitchPortBandwidth() {}
	private SwitchPortBandwidth(DatapathId d, OFPort p, U64 s, U64 rx, U64 tx, U64 rxValue, U64 txValue) {
		id = d;
		pt = p;
		speed = s;
=======
	SwitchPortVariables returnVars = new SwitchPortVariables();

	private SwitchPortBandwidth() {}
	private SwitchPortBandwidth(DatapathId d, OFPort p, U64 rx, U64 tx, U64 rxValue, U64 txValue) {
		id = d;
		pt = p;
>>>>>>> Completed project
		this.rx = rx;
		this.tx = tx;
		time = new Date();
		this.rxValue = rxValue;
		this.txValue = txValue;
	}
<<<<<<< HEAD
	
	public static SwitchPortBandwidth of(DatapathId d, OFPort p, U64 s, U64 rx, U64 tx, U64 rxValue, U64 txValue) {
=======

	public static SwitchPortBandwidth of(DatapathId d, OFPort p, U64 rx, U64 tx, U64 rxValue, U64 txValue) {
>>>>>>> Completed project
		if (d == null) {
			throw new IllegalArgumentException("Datapath ID cannot be null");
		}
		if (p == null) {
			throw new IllegalArgumentException("Port cannot be null");
		}
<<<<<<< HEAD
		if (s == null) {
			throw new IllegalArgumentException("Link speed cannot be null");
		}
=======
>>>>>>> Completed project
		if (rx == null) {
			throw new IllegalArgumentException("RX bandwidth cannot be null");
		}
		if (tx == null) {
			throw new IllegalArgumentException("TX bandwidth cannot be null");
		}
		if (rxValue == null) {
			throw new IllegalArgumentException("RX value cannot be null");
		}
		if (txValue == null) {
			throw new IllegalArgumentException("TX value cannot be null");
		}
<<<<<<< HEAD
		return new SwitchPortBandwidth(d, p, s, rx, tx, rxValue, txValue);
	}
	
	public DatapathId getSwitchId() {
		return id;
	}
	
	public OFPort getSwitchPort() {
		return pt;
	}
	
	public U64 getLinkSpeedBitsPerSec() {
		return speed;
	}
	
	public U64 getBitsPerSecondRx() {
		return rx;
	}
	
	public U64 getBitsPerSecondTx() {
		return tx;
	}
	
	protected U64 getPriorByteValueRx() {
		return rxValue;
	}
	
	protected U64 getPriorByteValueTx() {
		return txValue;
	}
	
	public long getUpdateTime() {
		return time.getTime();
	}
		
=======
		return new SwitchPortBandwidth(d, p, rx, tx, rxValue, txValue);
	}

	public DatapathId getSwitchId() {
		return id;
	}

	public OFPort getSwitchPort() {
		return pt;
	}

	public U64 getBitsPerSecondRx() {
		return rx;
	}

	public U64 getBitsPerSecondTx() {
		return tx;
	}

	protected U64 getPriorByteValueRx() {
		return rxValue;
	}

	protected U64 getPriorByteValueTx() {
		return txValue;
	}

	public long getUpdateTime() {
		return time.getTime();
	}

>>>>>>> Completed project
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((pt == null) ? 0 : pt.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SwitchPortBandwidth other = (SwitchPortBandwidth) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (pt == null) {
			if (other.pt != null)
				return false;
		} else if (!pt.equals(other.pt))
			return false;
		return true;
	}
<<<<<<< HEAD
=======
	@Override
	public String toString() {
		return "SwitchPortBandwidth [id=" + id + ", pt=" + pt + ", rx=" + rx.getValue() + ", tx=" + tx.getValue() + ", time=" + time
				+ ", rxValue=" + rxValue.getValue() + ", txValue=" + txValue.getValue() + "]";
	}

	public SwitchPortVariables toStringVars(){
		returnVars.rx = rx.getValue();
		returnVars.tx = tx.getValue();
		returnVars.rxValue = rxValue.getValue();
		returnVars.txValue = txValue.getValue();
		return returnVars;
	}
>>>>>>> Completed project
}