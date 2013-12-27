package net.floodlightcontroller.pktinhistory;

import java.util.ArrayList;
import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class PktInHistoryResource extends ServerResource {
    @Get("json")
    public List<String> retrieve() {
        IPktinHistoryService pihr = (IPktinHistoryService)getContext().getAttributes().get(IPktinHistoryService.class.getCanonicalName());
        List<String> l = new ArrayList<String>();
        l.addAll(java.util.Arrays.asList(pihr.sayHello()));
        return l;
    }
}