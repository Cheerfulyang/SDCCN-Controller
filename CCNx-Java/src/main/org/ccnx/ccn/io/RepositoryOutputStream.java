/*
 * Part of the CCNx Java Library.
 *
 * Copyright (C) 2008, 2009, 2010 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.ccnx.ccn.io;

import java.io.IOException;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.impl.repo.RepositoryFlowControl;
import org.ccnx.ccn.impl.security.crypto.ContentKeys;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.KeyLocator;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
import org.ccnx.ccn.protocol.SignedInfo.ContentType;


/**
 * A subclass of CCNOutputStream which writes its segments to a repository.
 * If the constructor is called with local set to true, the output stream will write
 * to a repository on the local device.
 * If no repository is available, it will throw an exception.
 * 
 * Data written using this class can be read using a normal CCNInputStream; that
 * class doesn't care whether its content comes from a repository or a cache (or a mix of the two).
 */
public class RepositoryOutputStream extends CCNOutputStream {
	
	public RepositoryOutputStream(ContentName name, CCNHandle handle) throws IOException {
		this(name, null, null, handle);
	}
	
	public RepositoryOutputStream(ContentName name,
								  PublisherPublicKeyDigest publisher, 
								  CCNHandle handle) throws IOException {
		this(name, null, publisher, handle);
	}

	public RepositoryOutputStream(ContentName name, 
								  KeyLocator locator, 
								  PublisherPublicKeyDigest publisher, 
								  CCNHandle handle) throws IOException {
		this(name, locator, publisher, null, null, handle);
	}

	public RepositoryOutputStream(ContentName name, 
								  ContentKeys keys,
								  CCNHandle handle) throws IOException {
		this(name, null, null, keys, handle);
	}

	public RepositoryOutputStream(ContentName name, 
								  KeyLocator locator,
								  PublisherPublicKeyDigest publisher, 
								  ContentKeys keys,
								  CCNHandle handle) throws IOException {
		this(name, locator, publisher, null, keys, handle);
	}

	public RepositoryOutputStream(ContentName name, 
								  KeyLocator locator,
								  PublisherPublicKeyDigest publisher, 
								  ContentType type,
								  ContentKeys keys, 
								  CCNHandle handle) throws IOException {
		super(name, locator, publisher, type, keys, new RepositoryFlowControl(name, handle));
	}
	

	public RepositoryOutputStream(ContentName name, CCNHandle handle, boolean local) throws IOException {
		this(name, null, null, handle, local);
	}
	
	public RepositoryOutputStream(ContentName name,
								  PublisherPublicKeyDigest publisher, 
								  CCNHandle handle,
								  boolean local) throws IOException {
		this(name, null, publisher, handle, local);
	}

	public RepositoryOutputStream(ContentName name, 
								  KeyLocator locator, 
								  PublisherPublicKeyDigest publisher, 
								  CCNHandle handle,
								  boolean local) throws IOException {
		this(name, locator, publisher, null, null, handle, local);
	}

	public RepositoryOutputStream(ContentName name, 
								  ContentKeys keys,
								  CCNHandle handle,
								  boolean local) throws IOException {
		this(name, null, null, keys, handle, local);
	}

	public RepositoryOutputStream(ContentName name, 
								  KeyLocator locator,
								  PublisherPublicKeyDigest publisher, 
								  ContentKeys keys,
								  CCNHandle handle,
								  boolean local) throws IOException {
		this(name, locator, publisher, null, keys, handle, local);
	}

	public RepositoryOutputStream(ContentName name, 
								  KeyLocator locator,
								  PublisherPublicKeyDigest publisher, 
								  ContentType type,
								  ContentKeys keys, 
								  CCNHandle handle,
								  boolean local) throws IOException {
		super(name, locator, publisher, type, keys, new RepositoryFlowControl(name, handle, local));
	}
}

