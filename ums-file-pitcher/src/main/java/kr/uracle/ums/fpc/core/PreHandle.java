package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.utils.AlarmManager;

public abstract class PreHandle {
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final boolean needAlarm;
	
	public PreHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		
		Object o =PARAM_MAP.get("PREHANDLE_ALARM_USEYN");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	abstract public boolean process(Path path);
	
	public boolean  preHandle(Path path) {
		boolean isOk = process(path);
		if(needAlarm && isOk == false) {
			sendAlarm(PRCS_NAME+", "+path.toString()+" 파일 전처리 중 에러 발생");
		}
		
		return isOk;
	}
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
