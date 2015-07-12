package br.ufes.inf.sergio.experimentos.loadbalancer;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;

import org.ccnx.ccn.protocol.ContentName;
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
		
		String name = "a/p/00f";
		byte[] mask = new byte[POFCCNxListener.CCNX_MAX_NAME_SIZE/8];
		byte[] str = null;
		ContentName cname = null;
		try {
			cname = ContentName.fromURI("ccnx:/"+name);
			str = cname.encode();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(str.length - 2);
		for (int i = 0; i < str.length - 2; i++){ // - 2 para eliminar 2 bytes de lixo no final
			mask[i] = (byte) 0xff;
		}
		mask[str.length - 4] = 0x00;
		mask[str.length - 5] = 0x00;
		
		addName("a/p/100", mask, portMap.get("s1-eth2"));
		addName("a/p/101", mask, portMap.get("s1-eth3"));
		addName("a/p/102", mask, portMap.get("s1-eth4"));
		addName("a/p/103", mask, portMap.get("s1-eth5"));
		addName("a/p/104", mask, portMap.get("s1-eth2"));
		addName("a/p/105", mask, portMap.get("s1-eth3"));
		addName("a/p/106", mask, portMap.get("s1-eth4"));
		addName("a/p/107", mask, portMap.get("s1-eth5"));
		addName("a/p/108", mask, portMap.get("s1-eth2"));
		addName("a/p/109", mask, portMap.get("s1-eth3"));
	}

	@Override
	public String getName() {
		return POFCCNxListenerS1.class.getSimpleName();
	}
}
