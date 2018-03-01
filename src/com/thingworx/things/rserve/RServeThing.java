package com.thingworx.things.rserve;

import org.rosuda.REngine.Rserve.RConnection;
import org.slf4j.Logger;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxDataShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxFieldDefinition;
import com.thingworx.metadata.annotations.ThingworxHandlerDefinitions;
import com.thingworx.things.connections.ConnectableThing;

@SuppressWarnings("serial")
@ThingworxBaseTemplateDefinition(name = "GenericThing")
@ThingworxConfigurationTableDefinitions(tables = {
		@ThingworxConfigurationTableDefinition(name = "RServeConfiguration", description = "", isMultiRow = false, ordinal = 0, dataShape = @ThingworxDataShapeDefinition(fields = {
				@ThingworxFieldDefinition(name = "host", description = "host address for the RServe Server", baseType = "STRING", ordinal = 0, aspects={"defaultValue:localhost"}),
				@ThingworxFieldDefinition(name = "port", description = "Port for the RServe Server", baseType = "INTEGER", ordinal = 1, aspects={"defaultValue:6311"}),
				@ThingworxFieldDefinition(name = "username", description = "", baseType = "STRING", ordinal = 2),
				@ThingworxFieldDefinition(name = "password", description = "", baseType = "PASSWORD", ordinal = 3) })) })
@ThingworxHandlerDefinitions(handlers={@com.thingworx.metadata.annotations.ThingworxHandlerDefinition(name="R", description="Connected R handler", isUserDefinable=true, className="com.thingworx.things.rserve.RHandler")})
public class RServeThing extends ConnectableThing {
	RConnection _connection;
	
	static Logger _logger = LogUtilities.getInstance().getApplicationLogger(RServeThing.class);


	
	@Override
	public boolean isConnected() throws Exception {
		// TODO Auto-generated method stub      
		return this._connection.isConnected();
	}
	
	
	
	public void startConnectableThing() throws Exception
	  {
		this._connection = new RConnection((String)getConfigurationSetting("RServeConfiguration", "host"),(Integer)getConfigurationSetting("RServeConfiguration", "port"));
		if (this._connection.isConnected()) {
			if (this._connection.needLogin()) {
				this._connection.login((String) getConfigurationSetting("RServeConfiguration", "username"),(String) getConfigurationSetting("RServeConfiguration", "password"));
			}
		}
	    
	  }
	  
	  public void stopConnectableThing() throws Exception
	  {
	    try
	    {
	      this._connection.close();
	      this._connection.finalize();
	      
	    }
	    catch (Exception eClosePool)
	    {
	      _logger.error("Error Closing Connection Pool in " + getName() + " [" + eClosePool.getMessage() + "]");
	    }
	    
	  }

	  
	
}
