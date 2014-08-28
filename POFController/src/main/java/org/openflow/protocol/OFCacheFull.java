package org.openflow.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.HexString;

public class OFCacheFull extends OFMessage implements Cloneable {
    public static final int MINIMUM_LENGTH = OFMessage.MINIMUM_LENGTH + 40;  //48
    public static final int MAXIMAL_LENGTH = OFCacheFull.MINIMUM_LENGTH;
    
    public enum OFCacheFullEntryCmd {
    	OFPCFAC_WARN,
        OFPCFAC_CRIT,                  /* New Cache. */
    }

    protected byte command;
    protected int totalEntries;
    protected int usedEntries;
    
    public OFCacheFull() {
        super();
        this.type = OFType.CACHE_FULL;
        //this.length = U16.t(MINIMUM_LENGTH);
        this.length = MINIMUM_LENGTH;
    }
    
    public int getLengthU() {
        return MAXIMAL_LENGTH;
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
    public OFCacheFull setCommand(byte command) {
        this.command = command;
        return this;
    }

    @Override
    public void readFrom(ChannelBuffer data) {
        super.readFrom(data);
        
        this.command = data.readByte();
        data.readBytes(7);
        
        this.totalEntries = data.readInt();  
        this.usedEntries = data.readInt();
    }

    @Override
    public void writeTo(ChannelBuffer data) {
        super.writeTo(data);
        
        data.writeByte(this.command);
        data.writeZero(7);
        
        data.writeInt(totalEntries);
        data.writeInt(usedEntries);
        
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
        string += HexString.toHex(usedEntries);
        
        return string;        
    }
    
    @Override
    public String toString(){
        String string = super.toString();
        string += "; FlowEntry:" +
                    "cmd=" + command +
                    ";te=" + totalEntries +
                    ";ue=" + usedEntries;
    
        return string;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + command;
        result = prime * result + totalEntries;
        result = prime * result + usedEntries;
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
        OFCacheFull other = (OFCacheFull) obj;
        if (command != other.command)
            return false;
        if (totalEntries != other.totalEntries)
        	return false;
        if (usedEntries != other.usedEntries)
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public OFCacheFull clone() throws CloneNotSupportedException {
    	OFCacheFull flowMod = (OFCacheFull) super.clone();

        return flowMod;
    }
}
