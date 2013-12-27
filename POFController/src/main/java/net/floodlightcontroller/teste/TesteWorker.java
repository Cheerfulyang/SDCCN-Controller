package net.floodlightcontroller.teste;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.DatatypeConverter;

import org.openflow.protocol.OFMatch20;
import org.openflow.protocol.OFMatchX;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionSetField;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.instruction.OFInstructionApplyActions;
import org.openflow.protocol.instruction.OFInstructionGotoTable;
import org.openflow.protocol.table.OFTableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huawei.ipr.pof.manager.IPMService;

public class TesteWorker extends Thread {
	
	protected static Logger logger;
	protected IPMService pofManager;
	
	public void setPofManager(IPMService p){
		this.pofManager = p;
	}
	
	public void run(){
		logger = LoggerFactory.getLogger(TesteWorker.class);
		try {
			launch();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void tutorial(int switchId){
		List<OFMatch20> fieldList = new ArrayList<OFMatch20>();
		List<OFMatchX> matchXList;
		List<OFInstruction> insList;
		List<OFAction> actionList;
		OFMatch20 match20;
		OFMatchX matchX;
		OFInstruction ins;
		OFAction action;
		short fieldId;
		byte[] value;
		byte[] mask;
		byte globalTableId, nextTableId;
		Integer outputPortId = 0;
		
		logger.debug(pofManager.iGetAllFlowTable(switchId).toString());
		
		// adiciona protocolo
		fieldId = pofManager.iNewField("Dmac", (short)48, (short)0);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("Smac", (short)48, (short)48);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("Eth Type", (short)16, (short)96);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("DIP", (short)64, (short)112);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("SIP", (short)64, (short)176);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		fieldId = pofManager.iNewField("Protocol", (short)16, (short)240);
		fieldList.add(pofManager.iGetMatchField(fieldId));
		pofManager.iAddProtocol("ETH + FP", fieldList);

		// Configure FP Flow Table
		fieldList = new ArrayList<OFMatch20>();
		matchXList = new ArrayList<OFMatchX>();
		insList = new ArrayList<OFInstruction>();
		match20 = pofManager.iGetMatchField((short)4); // DIP
		fieldList.add(match20);
		value = DatatypeConverter.parseHexBinary("1122334455667788");
		mask = DatatypeConverter.parseHexBinary("ffffffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		matchXList.add(matchX);
		globalTableId = pofManager.iAddFlowTable(switchId, "FP Flow Table", OFTableType.OF_LPM_TABLE.getValue(),
				(short) 64, 128, (byte)fieldList.size(), fieldList);
		logger.debug(String.valueOf(globalTableId));
		ins = new OFInstructionApplyActions();
		actionList = new ArrayList<OFAction>();
		action = new OFActionSetField();
		match20 = pofManager.iGetMatchField((short)1);
		value = DatatypeConverter.parseHexBinary("001fbbbbbbbb");
		mask = DatatypeConverter.parseHexBinary("ffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		((OFActionSetField) action).setFieldSetting(matchX);
		actionList.add(action);
		action = new OFActionSetField();
		match20 = pofManager.iGetMatchField((short)2);
		value = DatatypeConverter.parseHexBinary("aaaaaaaaaaaa");
		mask = DatatypeConverter.parseHexBinary("ffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		((OFActionSetField) action).setFieldSetting(matchX);
		actionList.add(action);
		action = new OFActionSetField();
		match20 = pofManager.iGetMatchField((short)4);
		value = DatatypeConverter.parseHexBinary("0123456789abcedf");
		mask = DatatypeConverter.parseHexBinary("ffffffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		((OFActionSetField) action).setFieldSetting(matchX);
		actionList.add(action);
		action = new OFActionSetField();
		match20 = pofManager.iGetMatchField((short)5);
		value = DatatypeConverter.parseHexBinary("1122334455667788");
		mask = DatatypeConverter.parseHexBinary("ffffffffffffffff");
		matchX = new OFMatchX(match20, value, mask);
		((OFActionSetField) action).setFieldSetting(matchX);
		actionList.add(action);
		((OFInstructionApplyActions) ins).setActionList(actionList);
		((OFInstructionApplyActions) ins).setActionNum((byte)actionList.size());
		insList.add(ins);
		
		ins = new OFInstructionApplyActions();
		actionList = new ArrayList<OFAction>();
		action = new OFActionOutput();
		List<Integer> portIdList = pofManager.iGetAllPortId(switchId);
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
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		// Create First Flow Table
		fieldList = new ArrayList<OFMatch20>();
		match20 = pofManager.iGetMatchField((short)1); // Dmac
		fieldList.add(match20);
		match20 = pofManager.iGetMatchField((short)3); // Eth Type
		fieldList.add(match20);
		pofManager.iAddFlowTable(switchId, IPMService.FIRST_ENTRY_TABLE_NAME,
				OFTableType.OF_MM_TABLE.getValue(), (short) 64, 128, (byte)fieldList.size(), fieldList);
		
		// Configure FP Parse Table
		fieldList = new ArrayList<OFMatch20>();
		matchXList = new ArrayList<OFMatchX>();
		insList = new ArrayList<OFInstruction>();
		match20 = pofManager.iGetMatchField((short)4); // SIP
		fieldList.add(match20);
		value = DatatypeConverter.parseHexBinary("0000000000000000");
		mask = DatatypeConverter.parseHexBinary("0000000000000000");
		matchX = new OFMatchX(match20, value, mask);
		matchXList.add(matchX);
		match20 = pofManager.iGetMatchField((short)5); // DIP
		fieldList.add(match20);
		value = DatatypeConverter.parseHexBinary("0000000000000000");
		mask = DatatypeConverter.parseHexBinary("0000000000000000");
		matchX = new OFMatchX(match20, value, mask);
		matchXList.add(matchX);
		match20 = pofManager.iGetMatchField((short)6); // Protocol
		fieldList.add(match20);
		value = DatatypeConverter.parseHexBinary("0901");
		mask = DatatypeConverter.parseHexBinary("ffff");
		matchX = new OFMatchX(match20, value, mask);
		matchXList.add(matchX);
		nextTableId = globalTableId;
		globalTableId = pofManager.iAddFlowTable(switchId, "FP Parse Flow Table", OFTableType.OF_MM_TABLE.getValue(),
				(short) 144, 128, (byte)fieldList.size(), fieldList);
		ins = new OFInstructionGotoTable();
		((OFInstructionGotoTable)ins).setNextTableId(nextTableId);
		insList.add(ins);
		pofManager.iAddFlowEntry(switchId, globalTableId, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		// Configure First Flow Table
		insList = new ArrayList<OFInstruction>();
		matchXList = new ArrayList<OFMatchX>();
		match20 = pofManager.iGetMatchField((short)1); // Dmac
		value = DatatypeConverter.parseHexBinary("000000000000");
		mask = DatatypeConverter.parseHexBinary("000000000000");
		matchX = new OFMatchX(match20, value, mask);
		matchXList.add(matchX);
		match20 = pofManager.iGetMatchField((short)3); // Eth Type
		value = DatatypeConverter.parseHexBinary("0888");
		mask = DatatypeConverter.parseHexBinary("ffff");
		matchX = new OFMatchX(match20, value, mask);
		matchXList.add(matchX);
		ins = new OFInstructionGotoTable();
		((OFInstructionGotoTable)ins).setNextTableId(globalTableId);
		insList.add(ins);
		pofManager.iAddFlowEntry(switchId, (byte)0, (byte)matchXList.size(), matchXList, 
				(byte)insList.size(), insList, (short) 1);
		
		// Enable data port
		pofManager.iSetPortOpenFlowEnable(switchId, outputPortId, (byte)1);
	}
		
	private void launch(){
		//Wait for switch to connect
		List<Integer> allSwitchID = null;
		do {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				allSwitchID = pofManager.iGetAllSwitchID();
			} catch (Exception e){
				e.printStackTrace();
			}
		}while ((allSwitchID == null) || (allSwitchID.size() == 0));

		logger.debug("Switch {} conectou!", allSwitchID.get(0));
		tutorial(allSwitchID.get(0));
	}
}
