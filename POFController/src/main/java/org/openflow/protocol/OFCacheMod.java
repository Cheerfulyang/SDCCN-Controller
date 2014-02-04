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
    
    public enum OFFlowEntryCmd {
        OFPCC_ADD,                  /* New Cache. */
        OFPCC_MODIFY,               /* Modify all matching caches. */
        OFPCC_MODIFY_STRICT,        /* Modify entry strictly matching wildcards */
        OFPCC_DELETE,               /* Delete all matching caches. */
        OFPCC_DELETE_STRICT         /* Strictly match wildcards and priority. */
    }

    // Open Flow Flow Mod Flags. Use "or" operation to set multiple flags
    //public static final short OFPFF_SEND_FLOW_REM = 0x1; // 1 << 0
    //public static final short OFPFF_CHECK_OVERLAP = 0x2; // 1 << 1
    //public static final short OFPFF_EMERG         = 0x4; // 1 << 2

    protected byte command;
    //protected int counterId;   
    
    //protected long cookie;
    //protected long cookieMask;
    
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
     * Get cookie
     * @return cookie
     */
    //public long getCookie() {
    ////    return this.cookie;
    //}

    /**
     * Set cookie
     * @param cookie
     */
    //public OFCacheMod setCookie(long cookie) {
    //    this.cookie = cookie;
    //    return this;
    //}

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
    
    
    /*public int getCounterId() {
        return counterId;
    }

    public void setCounterId(int counterId) {
        this.counterId = counterId;
    }

    public long getCookieMask() {
        return cookieMask;
    }

    public void setCookieMask(long cookieMask) {
        this.cookieMask = cookieMask;
    }*/

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
        data.readByte();
        //this.counterId = data.readInt();
        
        //this.cookie = data.readLong();
        //this.cookieMask = data.readLong();
        
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
        data.writeZero(1);
        //data.writeInt(this.counterId);
        
        //data.writeLong(this.cookie);
        //data.writeLong(this.cookieMask);
        
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
        string += HexString.ByteZeroEnd(1);
        
        //string += HexString.toHex(counterId);
        
        //string += HexString.toHex(cookie);
        
        //string += HexString.toHex(cookieMask);

        string += HexString.toHex(idleTimeout);
        string += " ";
        
        string += HexString.toHex(hardTimeout);
        string += HexString.toHex(priority);
        string += " ";
        
        string += HexString.toHex(index);
        
        string += HexString.ByteZeroEnd(4);
        
        string += name.toString();
        
        return string;        
    }
    
    @Override
    public String toString(){
        String string = super.toString();
        string += "; FlowEntry:" +
                    "cmd=" + command +
                    //";cid=" + counterId +
                   // ";ck=" + cookie +
                    //";ckm=" + cookieMask +
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
        //result = prime * result + (int) (cookie ^ (cookie >>> 32));
        //result = prime * result + (int) (cookieMask ^ (cookieMask >>> 32));
        //result = prime * result + counterId;
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
        //if (cookie != other.cookie)
        //    return false;
        //if (cookieMask != other.cookieMask)
        //    return false;
        //if (counterId != other.counterId)
        //    return false;
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
