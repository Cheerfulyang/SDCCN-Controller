/**
 * Copyright (c) 2012, 2013, Huawei Technologies Co., Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.huawei.ipr.pof.manager.database;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ccnx.ccn.protocol.ContentName;
import org.openflow.protocol.OFCacheMod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huawei.ipr.pof.manager.IPMService;

public class PMCacheTableDatabase {						//globalID
    protected Map<Integer, OFCacheMod> cacheEntriesMap;	//<entryId, CacheEntry>
    protected Integer cacheEntryNo;
    protected List<Integer> freeCacheEntryIDList;
    
    protected Map<String, Integer> matchKeyMap;			//<keyString, entryId>
    
    public PMCacheTableDatabase(){
        cacheEntriesMap = new ConcurrentHashMap <Integer, OFCacheMod>();
        cacheEntryNo = IPMService.CACHEENTRYID_START;
        freeCacheEntryIDList = Collections.synchronizedList(new ArrayList<Integer>());
        
        matchKeyMap = new ConcurrentHashMap<String, Integer>();
    }
    
    public OFCacheMod getCacheEntry(int index){
        return cacheEntriesMap.get(index);
    }
    
    public void putCacheEntry(int index, OFCacheMod CacheEntry){
        cacheEntriesMap.put(index, CacheEntry);
    }
    
    public void putMatchKey(String keyString, int entryId){
    	matchKeyMap.put(keyString, entryId);
    }
    
    public Integer getMatchKeyIndex(String keyString){
    	return matchKeyMap.get(keyString);
    }
    
    public void deleteMatchKey(String keyString){
    	matchKeyMap.remove(keyString);
    }
    
    public OFCacheMod deleteCacheEntry(int index){
        OFCacheMod CacheEntry = cacheEntriesMap.remove(index);
        freeCacheEntryIDList.add(index);
        return CacheEntry;
    }

    public Map<Integer, OFCacheMod> getCacheEntriesMap() {
        return cacheEntriesMap;
    }
    
    public int getNewCacheEntryID() {
        int newCacheEntryID;
        if (0 == freeCacheEntryIDList.size()) {
            newCacheEntryID = cacheEntryNo;
            cacheEntryNo++;
        } else {
            newCacheEntryID = freeCacheEntryIDList.remove(0);
        }
        return newCacheEntryID;
    }
    
	public boolean saveAllDataIntoFile(OutputStream out) {
        Gson gson = new Gson();
        String string;
        java.lang.reflect.Type type;

        try {           
            //save freeCacheEntryIDList
            type = new TypeToken<List<Integer>>(){}.getType();
            string = gson.toJson(freeCacheEntryIDList, type);
            out.write(string.getBytes());
            out.write('\n');
            
            //save cacheEntriesMap
            Iterator<Integer> CacheEntryIdItor  = cacheEntriesMap.keySet().iterator();
            int cacheId;
            OFCacheMod CacheEntry;
            String tableIdFlagString;
            while(CacheEntryIdItor.hasNext()){
            	cacheId = CacheEntryIdItor.next();
            	CacheEntry = cacheEntriesMap.get(cacheId);
            	
            	tableIdFlagString = "#cacheid#" + cacheId;
            	out.write(tableIdFlagString.getBytes());
            	out.write('\n');
            	
            	if(false == saveCacheEntryIntoFile(out, CacheEntry)){
            		return false;
            	}            	
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        
        return true;
	}
	
	private boolean saveCacheEntryIntoFile(OutputStream out, OFCacheMod CacheEntry) {
        Gson gson = new Gson();
        String string;
        java.lang.reflect.Type type;

        try {
            string = gson.toJson(CacheEntry.getCommand(), byte.class);
            out.write(string.getBytes());
            out.write('\n');
            
            string = gson.toJson(CacheEntry.getIdleTimeout(), short.class);
            out.write(string.getBytes());
            out.write('\n');
            
            string = gson.toJson(CacheEntry.getHardTimeout(), short.class);
            out.write(string.getBytes());
            out.write('\n');
            
            string = gson.toJson(CacheEntry.getPriority(), short.class);
            out.write(string.getBytes());
            out.write('\n');
            
            
            string = gson.toJson(CacheEntry.getIndex(), int.class);
            out.write(string.getBytes());
            out.write('\n');
            
            //save match
            type = new TypeToken<ContentName>(){}.getType();
            string = gson.toJson(CacheEntry.getName(), type);
            out.write(string.getBytes());
            out.write('\n');
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        
        return true;
	}
	
	public boolean loadAllDataFromFile(BufferedReader br, List<String> returnedCurLineString){ 
        Gson gson = new Gson();
        String lineString;
        java.lang.reflect.Type type;
        
        try {
            //read flowTableId
            lineString = br.readLine();
            
            //read freeCacheEntryIDList
            lineString = br.readLine();
            type = new TypeToken<List<Integer>>(){}.getType();
            freeCacheEntryIDList = gson.fromJson(lineString, type);
            
            //read cacheEntriesMap
            if(null == cacheEntriesMap){
            	cacheEntriesMap = new ConcurrentHashMap<Integer, OFCacheMod>();
            }else{
            	cacheEntriesMap.clear();
            }
            int cacheid;
            lineString = br.readLine();
            
            while( null != lineString && lineString.contains("#cacheid#") ){
            	cacheid = Integer.parseInt( lineString.substring("#cacheid#".length()));
            	OFCacheMod CacheEntry = new OFCacheMod();
            	CacheEntry.setLengthU(OFCacheMod.MAXIMAL_LENGTH);
            	CacheEntry.setIndex(cacheid);
            	
            	cacheEntriesMap.put(cacheid, CacheEntry);
            	if(false == loadCacheEntryFromFile(br, CacheEntry, returnedCurLineString)){
            		return false;
            	}
            	
            	lineString = returnedCurLineString.get(0);
            	
            }
            cacheEntryNo = cacheEntriesMap.size();
            
        }catch (Exception e) {
            
            e.printStackTrace();
            return false;
        }

        returnedCurLineString.add(0, lineString);
        return true;
	}
	
	private boolean loadCacheEntryFromFile(BufferedReader br, OFCacheMod CacheEntry, List<String> returnedCurLineString){ 
        Gson gson = new Gson();
        String lineString;
        java.lang.reflect.Type type;
        
        try {
            //read command
            lineString = br.readLine();
            CacheEntry.setCommand( gson.fromJson(lineString, byte.class) );

            //read idleTimeout
            lineString = br.readLine();
            CacheEntry.setIdleTimeout( gson.fromJson(lineString, short.class) );
            
            //read idleTimeout
            lineString = br.readLine();
            CacheEntry.setHardTimeout( gson.fromJson(lineString, short.class) );

            //read priority
            lineString = br.readLine();
            CacheEntry.setPriority( gson.fromJson(lineString, short.class) );
            
            //read flowId/index
            lineString = br.readLine();
            CacheEntry.setIndex( gson.fromJson(lineString, int.class) );
            
            //read name
            type = new TypeToken<ContentName>(){}.getType();
            lineString = br.readLine();
            ContentName cn = gson.fromJson(lineString, type);
            CacheEntry.setName( cn );
            
        }catch (Exception e) {
            
            e.printStackTrace();
            return false;
        }
        returnedCurLineString.add(0, lineString);
        return true;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((cacheEntriesMap == null) ? 0 : cacheEntriesMap.hashCode());
        result = prime * result
                + ((cacheEntryNo == null) ? 0 : cacheEntryNo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PMCacheTableDatabase other = (PMCacheTableDatabase) obj;
        if (cacheEntriesMap == null) {
            if (other.cacheEntriesMap != null)
                return false;
        } else if (!cacheEntriesMap.equals(other.cacheEntriesMap))
            return false;
        if (cacheEntryNo == null) {
            if (other.cacheEntryNo != null)
                return false;
        } else if (!cacheEntryNo.equals(other.cacheEntryNo))
            return false;
        return true;
    }
    
    
}
