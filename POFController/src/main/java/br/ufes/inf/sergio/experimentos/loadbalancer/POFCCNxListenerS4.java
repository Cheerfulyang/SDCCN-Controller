package br.ufes.inf.sergio.experimentos.loadbalancer;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.openflow.protocol.OFPort;
import org.slf4j.LoggerFactory;

import br.ufes.inf.sergio.POFCCNxListener;

public class POFCCNxListenerS4 extends POFCCNxListener implements IOFSwitchListener {
	
	private long id = 4;
	
	public POFCCNxListenerS4() {
		super();
		logger = LoggerFactory.getLogger(POFCCNxListenerS4.class);
	}
	
	@Override
	public void addedSwitch(IOFSwitch sw) {
		if (sw.getId() != id){
			return;
		}
		
		super.addedSwitch(sw);
		
		// Cria regras de encaminhamento
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		addName("a", portMap.get("s4-eth2"), OFPort.OFPP_FLOOD.getValue());
		addCache("a", (byte) 0);
	}

	@Override
	public String getName() {
		return POFCCNxListenerS4.class.getSimpleName();
	}
}
