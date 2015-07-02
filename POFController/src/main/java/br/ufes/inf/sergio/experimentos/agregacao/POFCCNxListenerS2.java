package br.ufes.inf.sergio.experimentos.agregacao;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;

import org.slf4j.LoggerFactory;

import br.ufes.inf.sergio.POFCCNxListener;

public class POFCCNxListenerS2 extends POFCCNxListener implements IOFSwitchListener {
	
	private long id = 2;
	
	public POFCCNxListenerS2() {
		super();
		logger = LoggerFactory.getLogger(POFCCNxListenerS2.class);
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
		addName("sergio", portMap.get("s2-eth1"), portMap.get("s2-eth2"));
		addCache("sergio", (byte) 0);
	}

	@Override
	public String getName() {
		return POFCCNxListenerS2.class.getSimpleName();
	}
}
