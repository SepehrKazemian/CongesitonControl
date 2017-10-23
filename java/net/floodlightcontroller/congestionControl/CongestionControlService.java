package net.floodlightcontroller.congestionControl;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.SwitchMessagePair;
 

public interface CongestionControlService extends IFloodlightService
{
    public ConcurrentCircularBuffer<SwitchMessagePair> getBuffer();

}
