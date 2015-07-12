package br.ufes.inf.sergio.experimentos.loadbalancer;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.openflow.protocol.OFPort;
import org.slf4j.LoggerFactory;

import br.ufes.inf.sergio.POFCCNxListener;

public class POFCCNxListenerS3 extends POFCCNxListener implements IOFSwitchListener {
	
	private long id = 3;
	
	public POFCCNxListenerS3() {
		super();
		logger = LoggerFactory.getLogger(POFCCNxListenerS3.class);
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
		addName("a", portMap.get("s3-eth2"), OFPort.OFPP_FLOOD.getValue());
		addCache("a", (byte) 0);
	}

	@Override
	public String getName() {
		return POFCCNxListenerS3.class.getSimpleName();
	}
}
