package br.ufes.inf.sergio;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import net.floodlightcontroller.restserver.RestletRoutable;

public class POFCCNxWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
        //router.attach("/addName/{name}/json", POFCCNxResource.class);
		router.attach("/addName/", POFCCNxResource.class).setMatchingMode(Template.MODE_STARTS_WITH);
		router.attach("/addCache/", POFCCNxResource.class).setMatchingMode(Template.MODE_STARTS_WITH);
		router.attach("/addStrictCache/", POFCCNxResource.class).setMatchingMode(Template.MODE_STARTS_WITH);
		router.attach("/delCache/{id}", POFCCNxResource.class);
		router.attach("/sendInfo/", POFCCNxResource.class);
        return router;
	}

	@Override
	public String basePath() {
		return "/ccnx";
	}

}
