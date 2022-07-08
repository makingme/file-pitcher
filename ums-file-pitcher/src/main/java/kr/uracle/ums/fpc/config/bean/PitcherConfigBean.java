package kr.uracle.ums.fpc.config.bean;

import java.util.HashMap;
import java.util.Map;

public class PitcherConfigBean {

	private String PATH;
	private int MAX_THREAD = 5;
	private Long CYCLE;
	private String DETECTION_CLASS;
	private String FILTER_CLASS;
	private String PREHANDLE_CLASS;
	private String MAINHANDLE_CLASS;
	private String POSTHANDLE_CLASS;

	private Map<String, Object> PARAM_MAP = new HashMap<String, Object>(30);
	
	public String getPATH() { return PATH; }
	public void setPATH(String PATH) { this.PATH = PATH; }
	
	public int getMAX_THREAD() { return MAX_THREAD;	}
	public void setMAX_THREAD(int mAX_THREAD) { MAX_THREAD = mAX_THREAD; }	
	
	public Long getCYCLE() { return CYCLE; }
	public void setCYCLE(Long cYCLE) { CYCLE = cYCLE; }	
	
	public String getDETECTION_CLASS() { return DETECTION_CLASS; }
	public void setDETECTION_CLASS(String DETECTION_CLASS) { this.DETECTION_CLASS = DETECTION_CLASS;	}
	
	public String getFILTER_CLASS() { return FILTER_CLASS;	}
	public void setFILTER_CLASS(String FILTER_CLASS) { this.FILTER_CLASS = FILTER_CLASS;	}
	
	public String getPREHANDLE_CLASS() { return PREHANDLE_CLASS; }
	public void setPREHANDLE_CLASS(String PREHANDLE_CLASS) { this.PREHANDLE_CLASS = PREHANDLE_CLASS;	}
	
	public String getMAINHANDLE_CLASS() { return MAINHANDLE_CLASS;	}
	public void setMAINHANDLE_CLASS(String MAINHANDLE_CLASS) { this.MAINHANDLE_CLASS = MAINHANDLE_CLASS;	}
	
	public String getPOSTHANDLE_CLASS() { return POSTHANDLE_CLASS; }
	public void setPOSTHANDLE_CLASS(String POSTHANDLE_CLASS) { this.POSTHANDLE_CLASS = POSTHANDLE_CLASS;	}
	
	public Map<String, Object> getPARAM_MAP() { return PARAM_MAP; }
	public void setPARAM_MAP(Map<String, Object> PARAM_MAP) { this.PARAM_MAP = PARAM_MAP; }	

}
