package br.ufes.inf.sergio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.DateTimeAtCompleted;

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

public class POFCCNxAgregacao2Fase implements IOFMessageListener, IFloodlightModule, IPOFCCNxService {
	
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApi;
	protected static Logger logger;
	protected IPMService pofManager;
	//protected POFCCNxListener listener;
	protected List<POFCCNxListener> listeners;
	protected List<CSEntry> entries1;
	protected List<CSEntry> entries2;
	protected List<CSEntry> entries3;
	protected Date s1LastUpdate;
	protected Date s2LastUpdate;
	protected Date s3LastUpdate;
	private Object lockCacheFull = new Object();
	private Object lockCacheInfo = new Object();
	
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
		logger = LoggerFactory.getLogger(POFCCNxAgregacao2Fase.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		pofManager = context.getServiceImpl(IPMService.class);
		listeners = new ArrayList<POFCCNxListener>();
		listeners.add(new POFCCNxListenerS1());
		listeners.add(new POFCCNxListenerS2());
		listeners.add(new POFCCNxListenerS3());
		s1LastUpdate = new Date();
		s2LastUpdate = new Date();
		s3LastUpdate = new Date();
		entries3 = new ArrayList<CSEntry>();
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
		return "POFCCNxAgregacao2Fase";
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
		logger.debug("ENVIANDO INFO PARA SWITCH " +switchId);
		return pofManager.iSendCacheInfo((int)switchId);
	}
	
	public int removeCSEntry(long switchId, ContentName name) {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Removendo entry "+name +" do switch "+switchId);
	    return pofManager.iDelCSEntry((int)switchId, name);
	}
	
	public int addCSEntry(long switchId, ContentName name) {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return pofManager.iAddCSEntry((int)switchId, name);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch(msg.getType()) {
        	case CACHE_FULL:
        		synchronized(lockCacheFull){
	        		//logger.debug("CACHE FULLL DO SWITCH " +sw.getId());
	        		Date now = new Date();
	        		if ((sw.getId() == 1) && ((now.getTime()-s1LastUpdate.getTime()) < 300)){
	        			break;
	        		}
	        		if ((sw.getId() == 2) && ((now.getTime()-s2LastUpdate.getTime()) < 300)){
	        			break;
	        		}
	        		if ((sw.getId() == 3) && ((now.getTime()-s3LastUpdate.getTime()) < 300)){
	        			break;
	        		}
	        		
	        		if (sw.getId() == 1){
	        			s1LastUpdate = new Date();
	        		}else if (sw.getId() == 1){
	        			s2LastUpdate = new Date();
	        		}else if (sw.getId() == 3){
	        			s3LastUpdate = new Date();
	        		}
	    			this.sendInfo(sw.getId());
	    			
	        		break;
        		}
        		
        	case CACHE_INFO:
        		synchronized(lockCacheInfo){
	        		//logger.debug("CACHE INFO DO SWITCH " +sw.getId());
	        		OFCacheInfo cacheInfo = (OFCacheInfo)msg;
	        		List<CSEntry> entries;
	        		List<CSEntry> entriesOutro;
	        		int idOutro;
	        		
	        		// Faz LRU no 3
	        		if (sw.getId() == 3)
	        		{
	        			s3LastUpdate = new Date();
	        			entries3 = new ArrayList<CSEntry>(Arrays.asList(cacheInfo.getEntries()));
	        			entries = entries3;
	        			CSEntry lru = entries.get(0);
	            		for (int i = 1; i < entries.size(); i++) {
	            			if (entries.get(i).getUpdated().before(lru.getUpdated())) {
	            				lru = entries.get(i);
	            			}
	            		}
	            		removeCSEntry(sw.getId(), lru.getName());
	            		entries3.remove(lru);
	            		s3LastUpdate = new Date();
	            		break;
	        		}
	        		
	        		if (sw.getId() == 1){
	        			entries1 = new ArrayList<CSEntry>(Arrays.asList(cacheInfo.getEntries()));
	        			entries = entries1;
	        			entriesOutro = entries2;
	        			idOutro = 2;
	        			s1LastUpdate = new Date();
	        		}else{
	        			entries2 = new ArrayList<CSEntry>(Arrays.asList(cacheInfo.getEntries()));
	        			entries = entries2;
	        			entriesOutro = entries1;
	        			idOutro = 1;
	        			s2LastUpdate = new Date();
	        		}
	        		
	        		if ((entries1 == null) || (entries2 == null))
	        		{
	        			break;
	        		}
	        		
	        		CSEntry objeto;
	        		// Move entradas duplicadas para o switch 3
	        		CSEntry lru = entries.get(0);
	        		int i = 0;
	        		for (i = 0; i < entries.size(); i++) {
	        			objeto = entries.get(i);
						//logger.debug("ANALIZANDO ENTRADA " + objeto.getName() + " do switch " + sw.getId());
						if (objeto.getUpdated().before(lru.getUpdated())) {
	        				lru = objeto;
	        			}
						
	        			if (entries3.contains(objeto)){
	    					//logger.debug("SWITCH 3 JA TEM A ENTRADA " + objeto.getName());
	        				entries.remove(i);
	        				removeCSEntry(sw.getId(), objeto.getName());
        					boolean removido2 = entriesOutro.remove(objeto);
        					if (removido2){
        						removeCSEntry(idOutro, objeto.getName());
        					}
	        				i--;
	        			} else if (entriesOutro.contains(objeto)){
	    					//logger.debug("ENTRADA DUPLICADA " + objeto.getName());
	        				entries.remove(i);
	        				removeCSEntry(1, objeto.getName());
	    					removeCSEntry(2, objeto.getName());
	    					entriesOutro.remove(objeto);
							addCSEntry(3, objeto.getName());
							CSEntry entry = new CSEntry();
							entry.setName(objeto.getName());
							entry.setCreated(objeto.getCreated());
							entry.setUpdated(objeto.getUpdated());
							entries3.add(entry);
							s1LastUpdate = new Date();
		        			s2LastUpdate = new Date();
	            			i--;
	        			}
	        		}
	        		
	        		if (entries.size() >= 47){
	            		removeCSEntry(sw.getId(), lru.getName());
	            		entries.remove(lru);
	            		if ((entries3.size() < 40) && !(entries3.contains(lru))){
							addCSEntry(3, lru.getName());
							CSEntry entry = new CSEntry();
							entry.setName(lru.getName());
							entry.setCreated(lru.getCreated());
							entry.setUpdated(lru.getUpdated());
							entries3.add(entry);
	            		}
	        		}
	        		
	        		break;
        		}
        		
        	default:
        		break;
		}
		
		return Command.CONTINUE;
	}
}