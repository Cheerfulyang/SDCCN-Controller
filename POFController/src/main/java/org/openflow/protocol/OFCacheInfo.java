package org.openflow.protocol;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.HexString;
import org.openflow.util.ParseString;

public class OFCacheInfo extends OFMessage implements Cloneable {
	public static final int MINIMUM_LENGTH = OFMessage.MINIMUM_LENGTH + 16;
    public static final int MAXIMAL_LENGTH = OFCacheInfo.MINIMUM_LENGTH + (OFGlobal.POFLR_CACHE_MAX_ENTRIES * OFGlobal.CCNX_MAX_NAME_SIZE);
    
    
    public enum OFCacheInfoEntryCmd {
    	OFPCIAC_REQUEST,
        OFPCIAC_REPLY,                  /* New Cache. */
    }

    protected byte command;
    protected int totalEntries;
    protected CSEntry[] entries;
    
    public OFCacheInfo() {
        super();
        this.type = OFType.CACHE_INFO;
        this.length = MAXIMAL_LENGTH;
    }
    
    public int getLengthU() {
        return this.length;
    }

    /**
     * Get command
     * @return command
     */
    public byte getCommand() {
        return this.command;
    }

    /**
     * Set command
     * @param command
     */
    public OFCacheInfo setCommand(byte command) {
        this.command = command;
        return this;
    }
    
    public CSEntry[] getEntries() {
    	return this.entries;
    }
    
    public int getTotalEntries() {
    	return this.totalEntries;
    }

    @Override
    public void readFrom(ChannelBuffer data) {
        super.readFrom(data);
        
        this.command = data.readByte();
        data.readBytes(7);
        
        this.totalEntries = data.readInt();  
        data.readBytes(4);
        
        byte[] name = new byte[OFGlobal.CCNX_MAX_NAME_SIZE * OFGlobal.POFLR_CACHE_MAX_ENTRIES];
        data.readBytes(name);
        String fullName =  ParseString.ByteToString(name);
        String[] names = fullName.split("\n");
        this.entries = new CSEntry[totalEntries];
        
        //System.out.println("TOTALENTRIES = "+totalEntries+", fullName = "+fullName);
        
        try {
        	for (int i = 0; i < totalEntries; i++) {
        		String[] aux = names[i].split("\t");
        		this.entries[i] = new CSEntry();
				this.entries[i].setName(ContentName.fromURI(aux[0]));
				try {
					this.entries[i].setCreated(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).parse(aux[1]));
					this.entries[i].setUpdated(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).parse(aux[2]));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println(entries[i].getName() + ", " + entries[i].getCreated());
        	}
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        while(data.readableBytes() > 0)
        	data.readByte();
    }

    @Override
    public void writeTo(ChannelBuffer data) {
        super.writeTo(data);
        
        data.writeByte(this.command);
        data.writeZero(7);
        
        data.writeInt(totalEntries);
        data.writeZero(4);
        
//        for (int i = 0; i < totalEntries; i++) {
//        	data.writeBytes(entries[i].toString().getBytes());
//        }
                
        while (data.writableBytes() > 0){
        	data.writeZero(1);
        }
    }
    
    @Override
    public String toBytesString(){
        String string = super.toBytesString();
        
        string += HexString.toHex(command);
        string += HexString.ByteZeroEnd(7);
        
        string += HexString.toHex(totalEntries);
        
        for (int i = 0; i < totalEntries; i++) {
        	string += entries[i].toString();
        }
        
        return string;        
    }
    
    @Override
    public String toString(){
        String string = super.toString();
        string += "; FlowEntry:" +
                    "cmd=" + command +
                    ";te=" + totalEntries;
//        for (int i = 0; i < totalEntries; i++) {
//        	string += ";e"+i+"="+entries[i].toString();
//        }
    
        return string;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + command;
        result = prime * result + totalEntries;
        for (int i = 0; i < totalEntries; i++) {
        	result = prime * result + entries[i].hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        OFCacheInfo other = (OFCacheInfo) obj;
        if (command != other.command)
            return false;
        if (totalEntries != other.totalEntries)
        	return false;
        for (int i = 0; i < totalEntries; i++) {
        	if (entries[i] != other.entries[i])
        		return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public OFCacheInfo clone() throws CloneNotSupportedException {
    	OFCacheInfo flowMod = (OFCacheInfo) super.clone();

        return flowMod;
    }
}
