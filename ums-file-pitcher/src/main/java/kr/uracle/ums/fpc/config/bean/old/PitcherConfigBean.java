package kr.uracle.ums.fpc.config.bean.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.uracle.ums.fpc.config.bean.AlarmConfigBean;

public class PitcherConfigBean {
	
	private String PATH;
	private String TARGET_PATH;
	
	private String SUCCESS_PATH;
	private String FAIL_PATH;
	
	private String INNER_SORT;
	private long CYCLE_TIME;
	private long STAY_LIMIT_TIME = 0;
	private long KEEP_TIME = 0;
	private int MAX_THREAD = 10;
	
	private String FILE_EXTENTION = "txt";
	private List<String> FILE_HEADERS = new ArrayList<String>(20);
	private String DELIMITER = ";";
	private String PITCHER_CLASS = "";
	private String HANDLER_CLASS = "";
	private Map<String, String> CUSTOM_MAP = new HashMap<String, String>(20);
	
	private int FILE_MAX;
	private AlarmConfigBean ALARM;
	
	
	public String getPATH() { return PATH; }
	public void setPATH(String pATH) { PATH = pATH; }
	
	public String getTARGET_PATH() { return TARGET_PATH; }
	public void setTARGET_PATH(String tARGET_PATH) { TARGET_PATH = tARGET_PATH;	}
	
	public String getSUCCESS_PATH() { return SUCCESS_PATH;	}
	public void setSUCCESS_PATH(String sUCCESS_PATH) { SUCCESS_PATH = sUCCESS_PATH;	}
	
	public String getFAIL_PATH() { return FAIL_PATH; }
	public void setFAIL_PATH(String fAIL_PATH) { FAIL_PATH = fAIL_PATH;	}
	
	public String getINNER_SORT() { return INNER_SORT; }
	public void setINNER_SORT(String iNNER_SORT) { INNER_SORT = iNNER_SORT;	}
	
	public long getCYCLE_TIME() { return CYCLE_TIME;	}
	public void setCYCLE_TIME(long cYCLE_TIME) { CYCLE_TIME = cYCLE_TIME; }
	
	public long getSTAY_LIMIT_TIME() { return STAY_LIMIT_TIME;	}
	public void setSTAY_LIMIT_TIME(long sTAY_LIMIT_TIME) { STAY_LIMIT_TIME = sTAY_LIMIT_TIME;	}
	
	public long getKEEP_TIME() { return KEEP_TIME; }
	public void setKEEP_TIME(long kEEP_TIME) { KEEP_TIME = kEEP_TIME; }
	
	public int getMAX_THREAD() { return MAX_THREAD;	}
	public void setMAX_THREAD(int mAX_THREAD) { MAX_THREAD = mAX_THREAD; }
	
	
	public List<String> getFILE_HEADERS() { return FILE_HEADERS; }
	public void setFILE_HEADERS(List<String> fILE_HEADERS) { FILE_HEADERS = fILE_HEADERS; }
	
	public String getDELIMITER() { return DELIMITER; }
	public void setDELIMITER(String dELIMETER) { DELIMITER = dELIMETER;	}
		
	public String getFILE_EXTENTION() { return FILE_EXTENTION; }
	public void setFILE_EXTENTION(String fILE_EXTENTION) { FILE_EXTENTION = fILE_EXTENTION;	}
	
	public String getPITCHER_CLASS() { return PITCHER_CLASS; }
	public void setPITCHER_CLASS(String PITCHER_CLASS) { this.PITCHER_CLASS = PITCHER_CLASS;	}

	public String getHANDLER_CLASS() { return HANDLER_CLASS; }
	public void setHANDLER_CLASS(String hANDLER_CLASS) { HANDLER_CLASS = hANDLER_CLASS;	}
	
	public Map<String, String> getCUSTOM_MAP() { return CUSTOM_MAP;	}
	public void setCUSTOM_MAP(Map<String, String> cUSTOM_MAP) { CUSTOM_MAP = cUSTOM_MAP; }
	
	
	public int getFILE_MAX() { return FILE_MAX; }
	public void setFILE_MAX(int fILE_MAX) { FILE_MAX = fILE_MAX; }
	
	public AlarmConfigBean getALARM() { return ALARM; }
	public void setALARM(AlarmConfigBean aLARM) { ALARM = aLARM; }
	
}
