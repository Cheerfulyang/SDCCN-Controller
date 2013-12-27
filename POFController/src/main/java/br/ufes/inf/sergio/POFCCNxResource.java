package br.ufes.inf.sergio;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class POFCCNxResource extends ServerResource {
    @Get("json")
    public int retrieve() {
        IPOFCCNxService pihr = (IPOFCCNxService)getContext().getAttributes().get(IPOFCCNxService.class.getCanonicalName());
        String name = getOriginalRef().toString().replaceFirst(getRequest().getRootRef().toString(), "");
        name = name.replaceFirst("/ccnx/addName/", "");
        return pihr.addName(name);
    }
}
