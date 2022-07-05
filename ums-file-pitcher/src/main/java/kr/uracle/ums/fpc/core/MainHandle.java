package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.fpc.utils.AlarmManager;

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
	
	protected final boolean needAlarm;
	
	/**
	 * @param PRCS_NAME : 프로세스 명
	 * @param PARAM_MAP : 사용자 지정 설정 값 - 설정 파일 지정 가능
	 */
	public MainHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		
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
	 * @param isPreSuccess : 전 처리 결과 - 전처리 지정 안했다면 무조건 TRUE
	 * @return : 처리 건수, 에러(실패) 시 음수 값 RETURN
	 */
	abstract public int process(Path path, boolean isPreSuccess);
	
	public boolean handle(Path path, boolean isPreSuccess) {
		boolean isOk = false;
		int prcsCnt  = process(path, isPreSuccess);		
		if(prcsCnt >= 0) {
			isOk = true;
			TpsManager.getInstance().addInputCnt(prcsCnt);
			TpsManager.getInstance().addProcessCnt(prcsCnt);
		}
		
		if(needAlarm && isOk == false) {
			String msg = PRCS_NAME+", "+path.toString()+" 파일 본 처리 실패";
			sendAlarm(msg);
		}
		return isOk;
	}
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
