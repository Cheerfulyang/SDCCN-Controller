package br.ufes.inf.sergio.experimentos.agregacao;

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
		addName("sergio", portMap.get("s3-eth3"), OFPort.OFPP_FLOOD.getValue());
		//addCache("sergio", (byte) 0); // COMENTAR PARA A 2 FASE (AGREGACAO)
		try {
			Thread.sleep(5000);
			addCSEntry(ContentName.fromURI("ccnx:/sergio/1"));
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return POFCCNxListenerS3.class.getSimpleName();
	}
}
