package kr.uracle.ums.fpc.bean.config;

import java.util.HashMap;
import java.util.Map;

public class ModuleConfigBean {

	private String NAME;
	private String CLASS_NAME;
	private boolean ALARM_ACTIVATION = false;
	private Map<String, Object> PARAM_MAP = new HashMap<String, Object>(20);
	
	public String getNAME() { return NAME;	}
	public void setNAME(String nAME) { NAME = nAME;	}
	
	public String getCLASS_NAME() { return CLASS_NAME;	}
	public void setCLASS_NAME(String cLASS_NAME) {	CLASS_NAME = cLASS_NAME; }
	
	public boolean isALARM_ACTIVATION() { return ALARM_ACTIVATION;	}
	public void setALARM_ACTIVATION(boolean aLARM_ACTIVATION) { ALARM_ACTIVATION = aLARM_ACTIVATION; }
	
	public Map<String, Object> getPARAM_MAP() { return PARAM_MAP; }
	public void setPARAM_MAP(Map<String, Object> pARAM_MAP) { PARAM_MAP = pARAM_MAP; }
}
