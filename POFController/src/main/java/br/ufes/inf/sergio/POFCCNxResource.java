package br.ufes.inf.sergio;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class POFCCNxResource extends ServerResource {
    @Get("json")
    public int retrieve() {
        IPOFCCNxService pihr = (IPOFCCNxService)getContext().getAttributes().get(IPOFCCNxService.class.getCanonicalName());
        String name = getOriginalRef().toString().replaceFirst(getRequest().getRootRef().toString(), "");
        if (name.contains("addName")){
        	name = name.replaceFirst("/ccnx/addName/", "");
        	return pihr.addName(name);
        }else if (name.contains("delCache")){
        	return pihr.delCache(Integer.parseInt((String) getRequest().getAttributes().get("id")));
        }else if (name.contains("addStrictCache")){
        	name = name.replaceFirst("/ccnx/addStrictCache/", "");
        	return pihr.addCache(name, (byte)1);
        }else{
        	name = name.replaceFirst("/ccnx/addCache/", "");
        	return pihr.addCache(name, (byte)0);
        }
    }
}
