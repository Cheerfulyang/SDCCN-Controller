package br.ufes.inf.sergio;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

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
	public final static String CCNX_TABLE_NAME = "CCNx Flow table";
	protected OFFlowTable ccnx_flow_table = null;
	
	public POFCCNxListener() {
		logger = LoggerFactory.getLogger(POFCCNxListener.class);
	}
	
	public void setPofManager(IPMService manager) {
		pofManager = manager;
	}
	
	public OFFlowTable getCCNxFlowTable() {
		return this.ccnx_flow_table;
	}

	private void enableDataPort(int switchId) { // FIXME PEGAR DO ARQUIVO DE CONF
		logger.debug("HABILITANDO O DATA PORT!");
		List<Integer> portIdList = pofManager.iGetAllPortId(switchId);
		for (int portId : portIdList){
        	OFPortStatus portStatus = pofManager.iGetPortStatus(switchId, portId);
            //if (portStatus.getDesc().getName().equals("veth0")){
            //	pofManager.iSetPortOpenFlowEnable(switchId, portId, (byte)1);
            //	continue;
            //}
            if (portStatus.getDesc().getName().matches("^s\\d+-eth\\d+$")){
        		pofManager.iSetPortOpenFlowEnable(switchId, portId, (byte)1);
            	//break;
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
		byte globalTableId, nextTableId;
		
		/*
		 *  Create CCNx Table. 
		 *  This will match CCNx names. For now we have chosen a max size of 1000 bytes 
		 *  to match ccnx names but this can be easyly modified
		 *  
		 */
		fieldList = new ArrayList<OFMatch20>();
		matchXList = new ArrayList<OFMatchX>();
		// create protocol
		fieldId = pofManager.iNewField("type", (short) 16, (short)0);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("name", (short)CCNX_MAX_NAME_SIZE, (short)16);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		pofManager.iAddProtocol("CCNx", fieldList);
		// configure flow table
		globalTableId = pofManager.iAddFlowTable(switchId, CCNX_TABLE_NAME, OFTableType.OF_LPM_TABLE.getValue(),
				(short)(CCNX_MAX_NAME_SIZE+16), TABLE_SIZE, (byte)fieldList.size(), fieldList);
		if (globalTableId == -1){
			logger.error("Failed to create CCNx flow table!");
			System.exit(1);
		}
		ccnx_flow_table = pofManager.iGetFlowTable(switchId, globalTableId);
		
		/*
		 *  Create First Flow table. 
		 *  This will handle Ethernet packets and forward to our CCNx table
		 */

		fieldList = new ArrayList<OFMatch20>();
		// create protocol
		fieldId = pofManager.iNewField("Dmac", (short)48, (short)0);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("Smac", (short)48, (short)48);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("Eth Type", (short)16, (short)96);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		pofManager.iAddProtocol("Ethernet sem VLAN", fieldList);
		
		// configure flow table
		nextTableId = globalTableId;
		globalTableId = pofManager.iAddFlowTable(switchId, IPMService.FIRST_ENTRY_TABLE_NAME,
				OFTableType.OF_MM_TABLE.getValue(), (short)112, TABLE_SIZE, (byte)fieldList.size(),
				fieldList);
		if (globalTableId == -1){
			logger.error("Failed to create flow table!");
			System.exit(1);
		}
		// add flow mod for ARP
		// create matches. Will match everything
		matchXList = new ArrayList<OFMatchX>();
		for (OFMatch20 m : fieldList) {
			value = new byte[m.getLength()/8];
			mask = new byte[m.getLength()/8];
			if (m.getFieldName().equals("Eth Type")){
				value = DatatypeConverter.parseHexBinary("0806");
				mask = DatatypeConverter.parseHexBinary("ffff");
			}
			matchX = new OFMatchX(m, value, mask);
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
		for (OFMatch20 m : fieldList) {
			value = new byte[m.getLength()/8];
			mask = new byte[m.getLength()/8];
			matchX = new OFMatchX(m, value, mask);
			matchXList.add(matchX);
		}
		insList = new ArrayList<OFInstruction>();
		// ========== TESTE - muda mac para outra coisa qualquer
		/*ins = new OFInstructionApplyActions();
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFActionSetField action = new OFActionSetField();
		OFMatch20 match20 = pofManager.iGetMatchField((short)3); // Dmac
		value = DatatypeConverter.parseHexBinary("001fbbbbbbbb");
		mask = DatatypeConverter.parseHexBinary("ffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		((OFActionSetField) action).setFieldSetting(matchX);
		actionList.add(action);
		action = new OFActionSetField();
		match20 = pofManager.iGetMatchField((short)4); // Smac
		value = DatatypeConverter.parseHexBinary("aaaaaaaaaaaa");
		mask = DatatypeConverter.parseHexBinary("ffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		((OFActionSetField) action).setFieldSetting(matchX);
		actionList.add(action);
		((OFInstructionApplyActions) ins).setActionList(actionList);
		((OFInstructionApplyActions) ins).setActionNum((byte)actionList.size());
		insList.add(ins);*/
		// ========================
		ins = new OFInstructionGotoTable();
		((OFInstructionGotoTable)ins).setNextTableId(nextTableId);
		// set packet offset Ethernet + IP + UDP
		//((OFInstructionGotoTable)ins).setPacketOffset((short) 14);
		((OFInstructionGotoTable)ins).setPacketOffset((short) (14 + 20 + 8));
		insList.add(ins);
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
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
