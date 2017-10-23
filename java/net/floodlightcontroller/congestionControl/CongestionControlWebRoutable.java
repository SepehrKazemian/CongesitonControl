package net.floodlightcontroller.congestionControl;


import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;


public class CongestionControlWebRoutable implements RestletRoutable 
{

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/history/json", CongestionControlResources.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/wm/pktinhistory";
	}

}
