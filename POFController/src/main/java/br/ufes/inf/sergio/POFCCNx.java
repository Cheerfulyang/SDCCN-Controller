package br.ufes.inf.sergio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.openflow.protocol.OFMatch20;
import org.openflow.protocol.OFMatchX;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.instruction.OFInstructionApplyActions;
import org.openflow.protocol.table.OFFlowTable;
import org.openflow.protocol.table.OFTableType;
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
		int res = 0;
		int switchId = pofManager.iGetAllSwitchID().get(0); // FIXME descobrir switch mais proximo
		Map<String, OFMatch20> fieldMap = listener.getFieldMap();
		byte tableId = 0;
		 
		/*
		 * Install flow in CCNx Interest and Content Tables
		 */
		OFFlowTable[] ofTables = new OFFlowTable[2];
		ofTables[0] = listener.getCCNxInterestTable();
		ofTables[1] = listener.getCCNxContentTable();

		// faz matching do name
		ArrayList<OFMatchX> matchXList = new ArrayList<OFMatchX>();
		byte[] value = new byte[POFCCNxListener.CCNX_MAX_NAME_SIZE/8];
		byte[] mask = new byte[POFCCNxListener.CCNX_MAX_NAME_SIZE/8];
		byte[] str = null;
		ContentName cname = null;
		try {
			cname = ContentName.fromURI("ccnx:/"+name);
			str = cname.encode();
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("NAME = "+String.valueOf(cname));
		for (int i = 0; i < str.length - 2; i++){ // - 2 para eliminar 2 bytes de lixo no final
			value[i] = str[i];
			mask[i] = (byte) 0xff;
		}
		OFMatchX matchX = new OFMatchX(fieldMap.get("name"), value, mask);
		matchXList.add(matchX);
		
		// output
		List<OFInstruction> insList = new ArrayList<OFInstruction>();
		OFInstruction ins = new OFInstructionApplyActions();
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFAction action = new OFActionOutput();
        ((OFActionOutput)action).setPortId(OFPort.OFPP_FLOOD.getValue());
		((OFActionOutput)action).setPacketOffset((short)0);
		actionList.add(action);
		((OFInstructionApplyActions) ins).setActionList(actionList);
		((OFInstructionApplyActions) ins).setActionNum((byte)actionList.size());
		insList.add(ins);
		
		// add flow table
		for (OFFlowTable n : ofTables){
			tableId = pofManager.parseToGlobalTableId(switchId, OFTableType.OF_LPM_TABLE.getValue(), n.getTableId());
			res |= pofManager.iAddFlowEntry(switchId, tableId, (byte)matchXList.size(), matchXList, 
					(byte)insList.size(), insList, (short) 1);
		}
		return res;
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

	@Override
	public int addCache(String name) {
		int t = 0;
		int switchId = pofManager.iGetAllSwitchID().get(0); // FIXME descobrir switch mais proximo
		try {
			return pofManager.iAddCacheEntry(switchId, ContentName.fromURI("ccnx:/"+name), (short) 1);
		} catch (MalformedContentNameStringException e) {
			e.printStackTrace();
		}
		return 1;
	}
}