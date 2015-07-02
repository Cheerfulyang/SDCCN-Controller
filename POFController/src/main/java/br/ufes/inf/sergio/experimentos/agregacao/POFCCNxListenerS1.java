package br.ufes.inf.sergio.experimentos.agregacao;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;

import org.openflow.protocol.OFPort;
import org.slf4j.LoggerFactory;

import br.ufes.inf.sergio.POFCCNxListener;

public class POFCCNxListenerS1 extends POFCCNxListener implements IOFSwitchListener {
	
	private long id = 1;
	
	public POFCCNxListenerS1() {
		super();
		logger = LoggerFactory.getLogger(POFCCNxListenerS1.class);
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
		addName("sergio", portMap.get("s1-eth1"), portMap.get("s1-eth2"));
		addCache("sergio", (byte) 0);
	}

	@Override
	public String getName() {
		return POFCCNxListenerS1.class.getSimpleName();
	}
}
