package br.ufes.inf.sergio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ccnx.ccn.impl.CCNFlowControl.SaveType;
import org.ccnx.ccn.io.content.CCNStringObject;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.openflow.protocol.OFMatchX;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.instruction.OFInstructionApplyActions;
import org.openflow.protocol.table.OFFlowTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huawei.ipr.pof.manager.IPMService;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;

public class POFCCNx implements IOFMessageListener, IFloodlightModule, IPOFCCNxService {
	
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApi;
	protected static Logger logger;
	protected IPMService pofManager;
	protected POFCCNxListener listener;

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IPOFCCNxService.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(IPOFCCNxService.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IPOFCCNxService.class);
	    l.add(IRestApiService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(POFCCNx.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		pofManager = context.getServiceImpl(IPMService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
	    restApi.addRestletRoutable(new POFCCNxWebRoutable());
	    listener = new POFCCNxListener();
	    listener.setPofManager(pofManager);
	    floodlightProvider.addOFSwitchListener(listener);
	}
	
	

	@Override
	public int addName(String name){
		logger.debug("ADDING NAME "+name);
		// pega informacoes do switch e tabela
		int switchId = pofManager.iGetAllSwitchID().get(0); // FIXME descobrir switch mais proximo
		OFFlowTable n = listener.getCCNxFlowTable();
		
		// faz matching do name
		byte[] value = new byte[POFCCNxListener.CCNX_MAX_NAME_SIZE/8];
		byte[] mask = new byte[POFCCNxListener.CCNX_MAX_NAME_SIZE/8];
		//byte[] str = name.getBytes();
		byte[] str = null;
		ContentName cname = null;
		CCNStringObject ccnstr = null;
		try {
			cname = ContentName.fromURI("ccnx:/"+name);
			ccnstr = new CCNStringObject(cname, (String)null, SaveType.RAW, null);
			str = ccnstr.;
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("NAME = "+String.valueOf(cname));
		for (int i = 0; i < str.length; i++){
			value[i] = str[i];
			mask[i] = (byte) 0xff;
		}
		OFMatchX matchX = new OFMatchX(n.getMatchFieldList().get(0), value, mask);
		ArrayList<OFMatchX> matchXList = new ArrayList<OFMatchX>();
		matchXList.add(matchX);
		// cria acoes
		// primeira - output
		List<OFInstruction> insList = new ArrayList<OFInstruction>();
		OFInstruction ins = new OFInstructionApplyActions();
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFAction action = new OFActionOutput();
		List<Integer> portIdList = pofManager.iGetAllPortId(switchId);
		int outputPortId = 0;
        for (int portId : portIdList){
        	OFPortStatus portStatus = pofManager.iGetPortStatus(switchId, portId);
            //logger.debug("PortStatus [id=" + portId + "]: " + portStatus.toString());
            if (portStatus.getDesc().getName().equals("veth0")){
            	outputPortId = portId;
            	break;
            }
        }
        ((OFActionOutput)action).setPortId(outputPortId);
		((OFActionOutput)action).setPacketOffset((short)0);
		actionList.add(action);
		((OFInstructionApplyActions) ins).setActionList(actionList);
		((OFInstructionApplyActions) ins).setActionNum((byte)actionList.size());
		insList.add(ins);
		// segunda - TODO
		return pofManager.iAddFlowEntry(switchId, (byte)10, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1); //FIXME 10
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		return null;
	}
}