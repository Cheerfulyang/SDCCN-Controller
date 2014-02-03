package br.ufes.inf.sergio;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.openflow.protocol.OFMatch20;
import org.openflow.protocol.OFMatchX;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionSetField;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.instruction.OFInstructionApplyActions;
import org.openflow.protocol.instruction.OFInstructionGotoTable;
import org.openflow.protocol.table.OFFlowTable;
import org.openflow.protocol.table.OFTableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huawei.ipr.pof.manager.IPMService;

public class POFCCNxListener implements IOFSwitchListener {
	
	protected IPMService pofManager;
	protected static Logger logger;
	protected final int TABLE_SIZE = 128; //FIXME AUMENTAR?
	public final static int CCNX_MAX_NAME_SIZE = 128; //FIXME AUMENTAR MULTIPLO DE 8 - POREM ESTA DANDO ERRO
	public final static String CCNX_FIRST_TABLE_NAME = "CCNx First Flow table";
	protected OFFlowTable ccnx_interest_table = null;
	protected OFFlowTable ccnx_content_table = null;
	protected Map<String, OFMatch20> fieldMap = null;
	
	public POFCCNxListener() {
		logger = LoggerFactory.getLogger(POFCCNxListener.class);
		fieldMap = new HashMap<String, OFMatch20>(); 
	}
	
	public void setPofManager(IPMService manager) {
		pofManager = manager;
	}
	
	public OFFlowTable getCCNxInterestTable() {
		return this.ccnx_interest_table;
	}
	
	public OFFlowTable getCCNxContentTable() {
		return this.ccnx_content_table;
	}
	
	public Map<String, OFMatch20> getFieldMap() {
		return this.fieldMap;
	}

	private void enableDataPort(int switchId) { // FIXME PEGAR DO ARQUIVO DE CONF
		logger.debug("HABILITANDO O DATA PORT!");
		List<Integer> portIdList = pofManager.iGetAllPortId(switchId);
		for (int portId : portIdList){
        	OFPortStatus portStatus = pofManager.iGetPortStatus(switchId, portId);
        	//if (portStatus.getDesc().getName().matches("^veth\\d+$")){
        	if (portStatus.getDesc().getName().matches("^veth0$")){
        		pofManager.iSetPortOpenFlowEnable(switchId, portId, (byte)1);
        		continue;
            }
            if (portStatus.getDesc().getName().matches("^s\\d+-eth\\d+$")){
        		pofManager.iSetPortOpenFlowEnable(switchId, portId, (byte)1);
            }
        }
	}
	
	private void createInitTables(int switchId) {
		List<OFMatch20> fieldList;
		List<OFMatchX> matchXList;
		List<OFInstruction> insList;
		OFMatchX matchX;
		OFInstruction ins;
		short fieldId;
		byte[] value;
		byte[] mask;
		byte globalTableId, nextTableId, firstCCnxTableId;
		OFMatch20 m = null;
		
		/*
		 * Create CCNx Protocols
		 * Type is a separate protocol because it will be in a different table
		 * and Content packets have Signature before Name so offset if different
		 */
		fieldList = new ArrayList<OFMatch20>();
		fieldId = pofManager.iNewField("type", (short) 16, (short)0);
		m = pofManager.iGetMatchField(fieldId);
		fieldList.add(m);
		fieldMap.put("type", m);
		pofManager.iAddProtocol("CCNx Type", fieldList);
		fieldList = new ArrayList<OFMatch20>();
		fieldId = pofManager.iNewField("name", (short)CCNX_MAX_NAME_SIZE, (short)0);
		m = pofManager.iGetMatchField(fieldId);
		fieldList.add(m);
		fieldMap.put("name", m);	
		pofManager.iAddProtocol("CCNx Name", fieldList);
		
		/*
		 * Create Ethernet protocol
		 */
		fieldList = new ArrayList<OFMatch20>();
		fieldId = pofManager.iNewField("Dmac", (short)48, (short)0);
		m = pofManager.iGetMatchField(fieldId);
		fieldList.add(m);
		fieldMap.put("Dmac", m);
		fieldId = pofManager.iNewField("Smac", (short)48, (short)48);
		m = pofManager.iGetMatchField(fieldId);
		fieldList.add(m);
		fieldMap.put("Smac", m);
		fieldId = pofManager.iNewField("Eth Type", (short)16, (short)96);
		m = pofManager.iGetMatchField(fieldId);
		fieldList.add(m);
		fieldMap.put("Eth Type", m);
		pofManager.iAddProtocol("Ethernet sem VLAN", fieldList);
		
		/*
		 *  Create First CCNx Table. 
		 *  This will match CCNx type (Interest or Content) and forward to appropriate table.
		 */
		fieldList = new ArrayList<OFMatch20>();
		fieldList.add(fieldMap.get("type"));
		globalTableId = pofManager.iAddFlowTable(switchId, CCNX_FIRST_TABLE_NAME, OFTableType.OF_LPM_TABLE.getValue(),
				(short)16, TABLE_SIZE, (byte)fieldList.size(), fieldList);
		if (globalTableId == -1){
			logger.error("Failed to create CCNx first flow table!");
			System.exit(1);
		}
		firstCCnxTableId = globalTableId;
		
		/*
		 *  Create CCNx Interest Table. 
		 */
		fieldList = new ArrayList<OFMatch20>();
		fieldList.add(fieldMap.get("name"));
		nextTableId = pofManager.iAddFlowTable(switchId, "CCNx Interest Flow Table", OFTableType.OF_LPM_TABLE.getValue(),
				(short)CCNX_MAX_NAME_SIZE, TABLE_SIZE, (byte)fieldList.size(), fieldList);
		if (nextTableId == -1){
			logger.error("Failed to create CCNx Interest Flow Table!");
			System.exit(1);
		}
		ccnx_interest_table = pofManager.iGetFlowTable(switchId, nextTableId);
		// add flow entry in CCNx First Flow Table for interests
		insList = new ArrayList<OFInstruction>();
		ins = new OFInstructionGotoTable();
		((OFInstructionGotoTable)ins).setNextTableId(nextTableId);
		((OFInstructionGotoTable)ins).setPacketOffset((short)2);
		insList.add(ins);
		matchXList = new ArrayList<OFMatchX>();
		m = fieldMap.get("type");
		value = DatatypeConverter.parseHexBinary("01d2");
		mask = DatatypeConverter.parseHexBinary("ffff");
		matchX = new OFMatchX(m, value, mask);
		matchXList.add(matchX);
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		/*
		 *  Create CCNx Content Table. 
		 */
		fieldList = new ArrayList<OFMatch20>();
		fieldList.add(fieldMap.get("name"));
		nextTableId = pofManager.iAddFlowTable(switchId, "CCNx Content Flow Table", OFTableType.OF_LPM_TABLE.getValue(),
				(short)CCNX_MAX_NAME_SIZE, TABLE_SIZE, (byte)fieldList.size(), fieldList);
		if (nextTableId == -1){
			logger.error("Failed to create CCNx Content Flow Table!");
			System.exit(1);
		}
		ccnx_content_table = pofManager.iGetFlowTable(switchId, nextTableId);
		// add flow entry in CCNx First Flow Table for Contents
		insList = new ArrayList<OFInstruction>();
		ins = new OFInstructionGotoTable();
		((OFInstructionGotoTable)ins).setNextTableId(nextTableId);
		((OFInstructionGotoTable)ins).setPacketOffset((short)(2+136)); // type + Signature
		insList.add(ins);
		matchXList = new ArrayList<OFMatchX>();
		m = fieldMap.get("type");
		value = DatatypeConverter.parseHexBinary("0482");
		mask = DatatypeConverter.parseHexBinary("ffff");
		matchX = new OFMatchX(m, value, mask);
		matchXList.add(matchX);
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		/*
		 *  Create First Flow table. 
		 *  This will handle Ethernet packets and forward to our CCNx table
		 */
		fieldList = new ArrayList<OFMatch20>();
		fieldList.add(fieldMap.get("Dmac"));
		fieldList.add(fieldMap.get("Smac"));
		fieldList.add(fieldMap.get("Eth Type"));
		nextTableId = globalTableId;
		globalTableId = pofManager.iAddFlowTable(switchId, IPMService.FIRST_ENTRY_TABLE_NAME,
				OFTableType.OF_MM_TABLE.getValue(), (short)112, TABLE_SIZE, (byte)fieldList.size(),
				fieldList);
		if (globalTableId == -1){
			logger.error("Failed to create flow table!");
			System.exit(1);
		}
		// add flow mod for ARP
        matchXList = new ArrayList<OFMatchX>();
        for (OFMatch20 mm : fieldList) {
                value = new byte[mm.getLength()/8];
                mask = new byte[mm.getLength()/8];
                if (mm.getFieldName().equals("Eth Type")){
                        value = DatatypeConverter.parseHexBinary("0806");
                        mask = DatatypeConverter.parseHexBinary("ffff");
                }
                matchX = new OFMatchX(mm, value, mask);
                matchXList.add(matchX);
        }
		insList = new ArrayList<OFInstruction>();
		ins = new OFInstructionApplyActions();
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFAction action = new OFActionOutput();
		((OFActionOutput)action).setPortId(OFPort.OFPP_FLOOD.getValue());
		((OFActionOutput)action).setPacketOffset((short)0);
		actionList.add(action);
		((OFInstructionApplyActions) ins).setActionList(actionList);
		((OFInstructionApplyActions) ins).setActionNum((byte)actionList.size());
		insList.add(ins);
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		// add flow mod for CCNx
		// create matches. Will match everything
		matchXList = new ArrayList<OFMatchX>();
		for (OFMatch20 mm : fieldList) {
			value = new byte[mm.getLength()/8];
			mask = new byte[mm.getLength()/8];
			matchX = new OFMatchX(mm, value, mask);
			matchXList.add(matchX);
		}
		insList = new ArrayList<OFInstruction>();
		ins = new OFInstructionGotoTable();
		((OFInstructionGotoTable)ins).setNextTableId(firstCCnxTableId);
		((OFInstructionGotoTable)ins).setPacketOffset((short) (14 + 20 + 8)); // Eth + IP + UDP
		insList.add(ins);
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		// teste cache
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int t = 0;
		try {
			t = pofManager.iAddCacheEntry(switchId, ContentName.fromURI("ccnx:/tes"), (short) 1);
			logger.debug("t = " + t);
			
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void addedSwitch(IOFSwitch sw) {
		// sleep 5 seconds to wait handshakes
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		enableDataPort((int)sw.getId());
		createInitTables((int)sw.getId());
	}

	@Override
	public void removedSwitch(IOFSwitch sw) {
		return;
	}

	@Override
	public String getName() {
		return POFCCNxListener.class.getSimpleName();
	}
}
