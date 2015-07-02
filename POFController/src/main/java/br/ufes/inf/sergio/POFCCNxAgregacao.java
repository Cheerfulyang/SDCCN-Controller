package br.ufes.inf.sergio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.openflow.protocol.CSEntry;
import org.openflow.protocol.OFCacheFull;
import org.openflow.protocol.OFCacheFull.OFCacheFullEntryCmd;
import org.openflow.protocol.OFCacheInfo;
import org.openflow.protocol.OFCacheInfo.OFCacheInfoEntryCmd;
import org.openflow.protocol.OFCacheMod;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufes.inf.sergio.experimentos.agregacao.POFCCNxListenerS1;
import br.ufes.inf.sergio.experimentos.agregacao.POFCCNxListenerS2;
import br.ufes.inf.sergio.experimentos.agregacao.POFCCNxListenerS3;

import com.huawei.ipr.pof.manager.IPMService;

public class POFCCNxAgregacao implements IOFMessageListener, IFloodlightModule, IPOFCCNxService {
	
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApi;
	protected static Logger logger;
	protected IPMService pofManager;
	//protected POFCCNxListener listener;
	protected List<POFCCNxListener> listeners;
	
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
		logger = LoggerFactory.getLogger(POFCCNxAgregacao.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		pofManager = context.getServiceImpl(IPMService.class);
		listeners = new ArrayList<POFCCNxListener>();
		listeners.add(new POFCCNxListenerS1());
		listeners.add(new POFCCNxListenerS2());
		listeners.add(new POFCCNxListenerS3());
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
	    restApi.addRestletRoutable(new POFCCNxWebRoutable());
	    //listener = new POFCCNxListener();
	    for (POFCCNxListener listener : listeners) {
		    listener.setPofManager(pofManager);
		    floodlightProvider.addOFSwitchListener(listener);
		}   
	    floodlightProvider.addOFMessageListener(OFType.CACHE_FULL, this);
	    floodlightProvider.addOFMessageListener(OFType.CACHE_INFO, this);
	}	

	@Override
	public int addName(String name){
		return listeners.get(0).addName(name, OFPort.OFPP_FLOOD.getValue(), OFPort.OFPP_FLOOD.getValue());
	}

	@Override
	public String getName() {
		return "POFCCNxAgregacao";
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
	public int addCache(String name, byte strict) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int delCache(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int sendInfo() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public int sendInfo(long switchId) {
		return pofManager.iSendCacheInfo((int)switchId);
	}
	
	public int removeCSEntry(long switchId, ContentName name) {
	    return pofManager.iDelCSEntry((int)switchId, name);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch(msg.getType()) {
        	case CACHE_FULL:
        		OFCacheFull cacheFull = (OFCacheFull)msg;
//        		if (cacheFull.getCommand() == OFCacheFullEntryCmd.OFPCFAC_WARN.ordinal()){
//        			logger.debug("WARNING: CACHE QUASE ENCHENDO!!!");
//        		}else{
//        			logger.debug("CRITICAL: CACHE ENCHEU!!!");
//        		}
        		this.sendInfo(sw.getId());
        		break;
        		
        	case CACHE_INFO:
        		OFCacheInfo cacheInfo = (OFCacheInfo)msg;
//        		if (cacheInfo.getCommand() == OFCacheInfoEntryCmd.OFPCIAC_REPLY.ordinal()){
//        			logger.debug("AEEEE, CHEGOU REPLY");
//        		}else{
//        			logger.debug("ERRRRRRROOOOOOOOOOOOO!");
//        		}
        		
        		CSEntry[] entries = cacheInfo.getEntries();
        		CSEntry lru = entries[0];
        		for (int i = 1; i < entries.length; i++) {
//        			System.out.println(entries[i].getName() + ", " + entries[i].getCreated() + ", " + entries[i].getUpdated());
        			if (entries[i].getUpdated().before(lru.getUpdated())) {
        				lru = entries[i];
        			}
        		}
        		removeCSEntry(sw.getId(), lru.getName());
        		break;
        		
        	default:
        		break;
		}
		
		return Command.CONTINUE;
	}
}