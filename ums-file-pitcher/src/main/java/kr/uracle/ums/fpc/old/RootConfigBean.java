package kr.uracle.ums.fpc.old;

import java.util.HashMap;
import java.util.Map;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.bean.config.DuplexConfigBean;
import kr.uracle.ums.fpc.bean.config.UmsMonitoringConfigBean;

public class RootConfigBean {		
	
	private String TIME_UNIT = "SEC";
	private String BASE_PATH;
	
	private UmsMonitoringConfigBean UMS_MONIT;
	
	private DuplexConfigBean DUPLEX;
	
	private Map<String, PitcherConfigBean> PITCHERS = new HashMap<String, PitcherConfigBean>(5);
		
	private AlarmConfigBean ALARM;
	
	public String getTIME_UNIT() { return TIME_UNIT; }
	public void setTIME_UNIT(String tIME_UNIT) { TIME_UNIT = tIME_UNIT;	}
	
	public String getBASE_PATH() { return BASE_PATH; }
	public void setBASE_PATH(String bASE_PATH) { BASE_PATH = bASE_PATH;	}
	
	public UmsMonitoringConfigBean getUMS_MONIT() { return UMS_MONIT; }
	public void setUMS_MONIT(UmsMonitoringConfigBean uMS_MONIT) { UMS_MONIT = uMS_MONIT; }
	
	public DuplexConfigBean getDUPLEX() { return DUPLEX; }
	public void setDUPLEX(DuplexConfigBean dUPLEX) { DUPLEX = dUPLEX; }
		
	public Map<String, PitcherConfigBean> getPITCHERS() { return PITCHERS; }
	public void setPITCHERS(Map<String, PitcherConfigBean> PITCHERS) { this.PITCHERS = PITCHERS;}
	
	public AlarmConfigBean getALARM() { return ALARM; }
	public void setALARM(AlarmConfigBean aLARM) { ALARM = aLARM; }
			
}
