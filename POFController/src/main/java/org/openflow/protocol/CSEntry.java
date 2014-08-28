package org.openflow.protocol;

import java.util.Date;

import org.ccnx.ccn.protocol.ContentName;

public class CSEntry {
	protected ContentName name;
	protected Date created;
	protected Date updated;
	
	public void setName(ContentName name) {
		this.name = name;
	}
	
	public void setCreated (Date created) {
		this.created = created;
	}
	
	public void setUpdated (Date updated) {
		this.updated = updated;
	}
	
	public ContentName getName() {
		return this.name;
	}
	
	public Date getCreated() {
		return this.created;
	}
	
	public Date getUpdated() {
		return this.updated;
	}
}