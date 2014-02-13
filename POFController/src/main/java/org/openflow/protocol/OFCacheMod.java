package org.openflow.protocol;

import java.util.ArrayList;
import java.util.List;

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.protocol.factory.OFInstructionFactory;
import org.openflow.protocol.factory.OFInstructionFactoryAware;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.table.OFTableType;
import org.openflow.util.HexString;
import org.openflow.util.U16;

import br.ufes.inf.sergio.POFCCNxListener;


public class OFCacheMod extends OFMessage implements Cloneable {
    public static final int MINIMUM_LENGTH = OFMessage.MINIMUM_LENGTH + 40;  //48
    public static final int MAXIMAL_LENGTH = OFCacheMod.MINIMUM_LENGTH;
    
    public enum OFCacheEntryCmd {
        OFPCAC_ADD,                  /* New Cache. */
        OFPCAC_MODIFY,               /* Modify matching cache. */
        OFPCAC_DELETE,               /* Delete matching cache. */
    }

    protected byte command;
    protected byte strict;   
    protected short idleTimeout;
    protected short hardTimeout;
    protected short priority;
    protected int index;
    protected ContentName name;
    
    public OFCacheMod() {
        super();
        this.type = OFType.CACHE_MOD;
        //this.length = U16.t(MINIMUM_LENGTH);
        this.length = MINIMUM_LENGTH;
        this.name = new ContentName();
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
    public OFCacheMod setCommand(byte command) {
        this.command = command;
        return this;
    }
    
    public byte getStrict() {
    	return this.strict;
    }
    
    public void setStrict(byte strict) {
    	this.strict = strict;
    }

    /**
     * Get hard_timeout
     * @return hardTimeout
     */
    public short getHardTimeout() {
        return this.hardTimeout;
    }

    /**
     * Set hard_timeout
     * @param hardTimeout
     */
    public OFCacheMod setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
        return this;
    }

    /**
     * Get idle_timeout
     * @return idleTimeout
     */
    public short getIdleTimeout() {
        return this.idleTimeout;
    }

    /**
     * Set idle_timeout
     * @param idleTimeout
     */
    public OFCacheMod setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    /**
     * Get priority
     * @return priority
     */
    public short getPriority() {
        return this.priority;
    }

    /**
     * Set priority
     * @param priority
     */
    public OFCacheMod setPriority(short priority) {
        this.priority = priority;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    
    public ContentName getName() {
        return name;
    }

    public void setName(ContentName name) {
        this.name = name;
    }

    @Override
    public void readFrom(ChannelBuffer data) {
        super.readFrom(data);
        
        this.command = data.readByte();
        this.strict = data.readByte();       
        this.idleTimeout = data.readShort();
        this.hardTimeout = data.readShort();
        this.priority = data.readShort();
        
        this.index = data.readInt();
        data.readBytes(4);
        
        String n = new String();
        char c;
        do {
        	c = data.readChar();
        	n += c;
        }while (c != '\0');
        try {
			name = ContentName.fromURI("ccnx:/"+n);
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public void writeTo(ChannelBuffer data) {
        super.writeTo(data);
        
        data.writeByte(this.command);
        data.writeByte(this.strict);
        data.writeShort(idleTimeout);
        data.writeShort(hardTimeout);
        data.writeShort(priority);
        
        data.writeInt(index);
        data.writeZero(4);
        
        data.writeBytes(name.toString().getBytes());
        
        while (data.writableBytes() > 0){
        	data.writeZero(1);
        }
    }
    
    @Override
    public String toBytesString(){
        String string = super.toBytesString();
        
        string += HexString.toHex(command);
        string += HexString.toHex(strict);
        string += HexString.toHex(idleTimeout);
        string += " ";
        
        string += HexString.toHex(hardTimeout);
        string += HexString.toHex(priority);
        string += " ";
        
        string += HexString.toHex(index);
        string += HexString.ByteZeroEnd(4);
        string += " ";
        
        string += name.toString();
        
        return string;        
    }
    
    @Override
    public String toString(){
        String string = super.toString();
        string += "; FlowEntry:" +
                    "cmd=" + command +
                    ";st=" + strict +
                    ";it=" + idleTimeout +
                    ";ht=" + hardTimeout +
                    ";p=" + priority +
                    ";i=" + index +
                    ";m=" + name;
    
        return string;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + command;
        result = prime * result + strict;
        result = prime * result + hardTimeout;
        result = prime * result + idleTimeout;
        result = prime * result + index;
        result = prime * result + name.hashCode();
        result = prime * result + priority;
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
        OFCacheMod other = (OFCacheMod) obj;
        if (command != other.command)
            return false;
        if (strict != other.strict)
        	return false;
        if (hardTimeout != other.hardTimeout)
            return false;
        if (idleTimeout != other.idleTimeout)
            return false;
        if (index != other.index)
            return false;
        if (!name.equals(other.name))
            return false;
        if (priority != other.priority)
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public OFCacheMod clone() throws CloneNotSupportedException {
        OFCacheMod flowMod = (OFCacheMod) super.clone();
        int n = name.count();
        flowMod.setName(name.copy(n));

        return flowMod;
    }
}
