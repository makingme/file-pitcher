package kr.uracle.ums.fpc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmManager {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final AlarmManager instance = new AlarmManager();
	
	public static AlarmManager getInstance() {
		return instance;
	}
	
	public void sendAlarm(String msg) {
		logger.info("알람발송 요청:{}",msg);
	}
}
