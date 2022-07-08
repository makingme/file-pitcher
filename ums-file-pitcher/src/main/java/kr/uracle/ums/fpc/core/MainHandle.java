package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.bean.AlarmConfigBean;
import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.sdk.util.UmsAlarmSender;

/**
 * @author : URACLE KKB
 * @see : logger - 로거 인스턴스 멤버 변수
 * @see : PRCS_NAME - 프로세스 명 멤버 변수
 * @see : PARAM_MAP - Map(String, Object) 타입의 사용자 지정 설정 정보를 담은 멤버 변수
 * @see : 사용자 지정 설정(PARAM_MAP)에 MAIN_ALARM : Y 설정 정보 지정 시 본 처리 실패 시 알람 발송 지원 
 */
public abstract class MainHandle {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final AlarmConfigBean ALARM_CONFIG;
	
	protected final boolean needAlarm;
	
	/**
	 * @param PRCS_NAME : 프로세스 명
	 * @param PARAM_MAP : 사용자 지정 설정 값 - 설정 파일 지정 가능
	 */
	public MainHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP, AlarmConfigBean ALARM_CONFIG) {
		this.PRCS_NAME = PRCS_NAME;
		this.PARAM_MAP = PARAM_MAP;
		this.ALARM_CONFIG = ALARM_CONFIG;
		
		Object o =PARAM_MAP.get("MAIN_ALARM");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	/**
	 * @see 필수 값 및 사용자 지정 멤버 변수 초기화, 별도 초기화 멤버 없으면 TRUE RETURN
	 * @return 초기화 성공 여부
	 */
	abstract public boolean initailize();
	
	/**
	 * @param path : 처리할 파일 경로(절대 경로)
	 * @param preResultCode : 전 처리 결과 - 전처리 지정 안했다면 무조건 0
	 * @return : 처리 건수, 에러(실패) 시 음수(-1) 값 RETURN - MAIN_ALARM 설정 여부에 따른 알람 발송
	 */
	abstract public int process(Path path, int preResultCode);
	
	public int handle(Path path, int preResultCode) {
		int prcsCnt = 0;
		try {
			prcsCnt  = process(path, preResultCode);					
		}catch(Exception e) {
			prcsCnt = -1;
			e.printStackTrace();
		}
		
		if(prcsCnt > 0) {
			TpsManager.getInstance().addInputCnt(prcsCnt);
			TpsManager.getInstance().addProcessCnt(prcsCnt);
			return prcsCnt;
		}
		
		if(needAlarm && prcsCnt <= 0) {
			String msg = PRCS_NAME+", "+path.toString()+(prcsCnt==0?", 파일 데이터 0":", 파일 본 처리 중 에러 발생");
			sendAlarm(msg);
		}
		return prcsCnt;
	}
	
	public void sendAlarm(String msg) {
		msg = ALARM_CONFIG.getPREFIX_MESSAGE()+msg;
		UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getURL(), msg);
	}
}
