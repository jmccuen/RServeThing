package com.thingworx.things.rserve;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RserveException;

import com.thingworx.common.utils.MetadataUtilities;
import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.datashape.DataShape;
import com.thingworx.entities.interfaces.IServiceProvider;
import com.thingworx.handlers.ServiceHandlerBase;
import com.thingworx.metadata.DataShapeDefinition;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.metadata.collections.FieldDefinitionCollection;
import com.thingworx.system.managers.DataShapeManager;
import com.thingworx.things.connected.RemoteDatabase;
import com.thingworx.things.database.DBUtilities;
import com.thingworx.things.database.QueryHandlerBase;
import com.thingworx.things.database.QueryParam;
import com.thingworx.types.BaseTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.collections.ValueCollectionList;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;



public class RHandler
  extends ServiceHandlerBase
{
  public static final String HANDLER_NAME = "R";
  private static final String SCRIPT_CONFIGURATION_TABLE = "R";
  private static final String CODE_FIELD = "code";
  
  
  public InfoTable processService(IServiceProvider serviceProvider, ValueCollection params) throws Exception
  {
	   
	    RServeThing RServeThing = (RServeThing)serviceProvider;
	    InfoTable result = new InfoTable();
	    String RCode = ((String)getServiceImplementation().getConfigurationData().getValue("R", "code"));
	    
	    RCode = RCode.replace("\r", "");
	    
	    for (Entry<String, IPrimitiveType> entry : params.entrySet()) {
	    	if (entry.getValue().getBaseType() == BaseTypes.INFOTABLE) {
	    		try { 
	    			
	    			DataShapeDefinition ds = ((InfoTable)entry.getValue().getValue()).getDataShape();
	    			ValueCollectionList rows = ((InfoTable)entry.getValue().getValue()).getRows();
	    			FieldDefinitionCollection fields = ds.getFields();
	    			ArrayList<FieldDefinition> fieldList = fields.getOrderedFieldsByOrdinal();
	    			
	    			REXP[] itParam = new REXP[fieldList.size()];
	    			
	    			String[] colNames = new String[fieldList.size()];
	    			int c = 0;
	    			for (FieldDefinition field : fieldList) {
	    				colNames[c] = field.getName();
	    				int r = 0;
	    				if (field.getBaseType() == BaseTypes.NUMBER)
	    				{
	    					double[] values = new double[rows.getRowCount()];
	    					for (ValueCollection row : rows) {
		    					values[r] = (Double)BaseTypes.ConvertToObject(row.getValue(field.getName()), field.getBaseType());
		    					r++;
		    				}
	    					itParam[c] = new REXPDouble(values);
	    				} else if (field.getBaseType() == BaseTypes.INTEGER || field.getBaseType() == BaseTypes.LONG)
	    				{
	    					int[] values = new int[rows.getRowCount()];
	    					for (ValueCollection row : rows) {
		    					values[r] = (Integer)BaseTypes.ConvertToObject(row.getValue(field.getName()), field.getBaseType());
		    					r++;
		    				}
	    					itParam[c] = new REXPInteger(values);
	    					
	    				} else if (field.getBaseType() == BaseTypes.BOOLEAN) 
	    				{
	    					boolean[] values = new boolean[rows.getRowCount()];
	    					for (ValueCollection row : rows) {
		    					values[r] = (Boolean)BaseTypes.ConvertToObject(row.getValue(field.getName()), field.getBaseType());
		    					r++;
		    				}
	    					itParam[c] = new REXPLogical(values);
	    				} else {
	    					String[] values = new String[rows.getRowCount()];
	    					for (ValueCollection row : rows) {
		    					values[r] = row.getStringValue(field.getName());
		    					r++;
		    				}
	    					itParam[c] = new REXPString(values);
	    				}
	    				
	    				c++;
	    			}
	    			
	    			RServeThing._connection.assign(entry.getKey(), REXP.createDataFrame(new RList(itParam,colNames)));
	    		} catch (Exception e ) {
	    			throw new Exception("Error - " + e.getMessage());
	    		}
	    	} else if (entry.getValue().getBaseType() == BaseTypes.NUMBER || entry.getValue().getBaseType() == BaseTypes.LONG || entry.getValue().getBaseType() == BaseTypes.INTEGER)  {
	    		RServeThing._connection.assign(entry.getKey(), new REXPDouble((double)entry.getValue().getValue()));
	    	} else if (entry.getValue().getBaseType() == BaseTypes.STRING) { 
	    		RServeThing._connection.assign(entry.getKey(), entry.getValue().getStringValue());
	    	}
	    }
	  
	   
	    String dataShape = getServiceDefinition().getResultType().getDataShapeName();
	    
	    if(dataShape == null || dataShape.isEmpty()) {
	    	dataShape = null;
	    } else {
	    	result = InfoTableInstanceFactory.createInfoTableFromDataShape(dataShape);
	    }
	    
	    try { 
	    	
	    	REXP value = RServeThing._connection.parseAndEval(RCode);
	    	
	    	return GetResultTable(value);
	    
	} catch (Exception err) {
	    	throw new Exception(err.getMessage());
	}
  }
  
 
 
public InfoTable GetResultTable(REXP value) throws Exception {
	InfoTable result = new InfoTable();
	
	if (value.isList()) {
		int c = 0;
		Iterator<?> items = value.asList().iterator();
		while(items.hasNext()) {
			
			REXP item = (REXP) items.next();
			if (item.isList()) {
				
				FieldDefinition field = new FieldDefinition();
				String fieldName = "values";
				field.setName(fieldName);
				field.setBaseType(BaseTypes.INFOTABLE);
				result.addField(field);
				
				ValueCollection row = new ValueCollection();
				row.SetInfoTableValue(fieldName, GetResultTable(item));	
				result.addRow(row);
	
				
			} else if (item.length() > 0) {
				FieldDefinition field = new FieldDefinition();
				String fieldName = "value" + (c + 1);
				try {
					Vector names = value.asList().names; 
					fieldName = (String)names.get(c);
				} catch (Exception err) {};
				field.setName(fieldName);
				field.setBaseType(GetBaseType(item));
				result.addField(field);
				
				String[] fieldValues = item.asStrings();
				
				for (int r=0;r<item.length();r++) {
					String fieldValue = fieldValues[r];
					
					if (c==0) {
						ValueCollection row = new ValueCollection();
						row.setValue(fieldName, BaseTypes.ConvertToPrimitive(fieldValue,GetBaseType(item)));
						result.addRow(row);
					} else {
						result.getRow(r).setValue(fieldName, BaseTypes.ConvertToPrimitive(fieldValue,GetBaseType(item)));
						
					}
				}
			} else if (item.isLogical()){
				if (c==0) { 
					FieldDefinition field = new FieldDefinition();
					field.setName("result");
					field.setBaseType(BaseTypes.BOOLEAN);
					result.addField(field);
				}

				ValueCollection row = new ValueCollection();
				row.SetBooleanValue("result",item.asInteger());
				result.addRow(row);
	
			} else if (item.isInteger()){
				if (c==0) { 
					FieldDefinition field = new FieldDefinition();
					field.setName("result");
					field.setBaseType(BaseTypes.INTEGER);
					result.addField(field);
				}
				
				ValueCollection row = new ValueCollection();
				row.SetIntegerValue("result",item.asInteger());
				result.addRow(row);

			} else if (value.isNumeric()) {
				if (c==0) { 
					FieldDefinition field = new FieldDefinition();
					field.setName("result");
					field.setBaseType(BaseTypes.NUMBER);
					result.addField(field);
				}
				ValueCollection row = new ValueCollection();
				row.SetNumberValue("result",item.asDouble());
				result.addRow(row);

			} else {
				if (c==0) { 
					FieldDefinition field = new FieldDefinition();
					field.setName("result");
					field.setBaseType(BaseTypes.STRING);
					result.addField(field);
				}
				
				ValueCollection row = new ValueCollection();
				row.SetStringValue("result",item.asString());
				result.addRow(row);

			}
			c++;
		}
	} else if (value.isLogical()) {
	
		FieldDefinition field = new FieldDefinition();
		field.setName("result");
		field.setBaseType(BaseTypes.BOOLEAN);
		result.addField(field);
		
		if (value.length() > 0) {
			for(int i=0;i<value.length();i++) {
				Integer item = value.asIntegers()[i];
				ValueCollection row = new ValueCollection();
				row.SetBooleanValue("result",item);
				result.addRow(row);
			}
		} else {
			ValueCollection row = new ValueCollection();
			row.SetBooleanValue("result",value.asInteger());
			result.addRow(row);
		}
		
	} else if (value.isInteger()){
		FieldDefinition field = new FieldDefinition();
		field.setName("result");
		field.setBaseType(BaseTypes.INTEGER);
		result.addField(field);
		if (value.length() > 0) {
			for(int i=0;i<value.length();i++) {
				Integer item = value.asIntegers()[i];
				ValueCollection row = new ValueCollection();
				row.SetIntegerValue("result",item);
				result.addRow(row);
			}
		} else {
			ValueCollection row = new ValueCollection();
			row.SetIntegerValue("result",value.asInteger());
			result.addRow(row);
		}

	} else if (value.isNumeric()) {
		
		FieldDefinition field = new FieldDefinition();
		field.setName("result");
		field.setBaseType(BaseTypes.NUMBER);
		result.addField(field);
		
		if (value.length() > 0) {
			for(int i=0;i<value.length();i++) {
				Double item = value.asDoubles()[i];
				ValueCollection row = new ValueCollection();
				row.SetNumberValue("result",item);
				result.addRow(row);
			}
		} else {
			ValueCollection row = new ValueCollection();
			row.SetNumberValue("result",value.asDouble());
			result.addRow(row);
		}
	} else if (value.isRaw()) { 
		//must be an image?
		FieldDefinition field = new FieldDefinition();
    	field.setBaseType(BaseTypes.IMAGE);
    	field.setName("result");
    	result.addField(field);
    	ValueCollection values = new ValueCollection();
    	values.SetImageValue("result", BaseTypes.ConvertToPrimitive(value.asBytes(), BaseTypes.IMAGE));
    	result.addRow(values);
		
	} else {
		FieldDefinition field = new FieldDefinition();
		field.setName("result");
		field.setBaseType(BaseTypes.STRING);
		result.addField(field);
		if (value.length() > 0) {
			for(int i=0;i<value.length();i++) {
				String item = value.asStrings()[i];
				ValueCollection row = new ValueCollection();
				row.SetStringValue("result",item);
				result.addRow(row);
			}
		} else {
			
			ValueCollection row = new ValueCollection();
			row.SetStringValue("result",value.asString());
			result.addRow(row);
		}
	}
	
	return result;
		
	}


  
  public BaseTypes GetBaseType(REXP fieldList) {
	  if (fieldList.isLogical()) {
		  return BaseTypes.BOOLEAN;
	  } else if (fieldList.isInteger())
	  {
		  return BaseTypes.LONG;
	  } else if (fieldList.isNumeric())
	  {
		  return BaseTypes.NUMBER;
	  } else {
		  return BaseTypes.STRING;
	  }
  }


}