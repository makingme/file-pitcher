package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.bean.config.ModuleConfigBean;
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
	 * @param MODULE_CONFIG	: 모듈 설정 정보 - 모듈명, 알람여부, 구현클래스명, 지정 변수맵
	 * @param ALARM_CONFIG  : 알람 설정 정보 - 발송채널, 발송UMS URL, 알람 고정 문구(PREFIX)
	 */
	public MainHandle(ModuleConfigBean MODULE_CONFIG, AlarmConfigBean ALARM_CONFIG) {
		this.PRCS_NAME = MODULE_CONFIG.getNAME()==null?this.getClass().getSimpleName():MODULE_CONFIG.getNAME();
		this.PARAM_MAP = MODULE_CONFIG.getPARAM_MAP();
		this.ALARM_CONFIG = ALARM_CONFIG;
		
		needAlarm = MODULE_CONFIG.isALARM_ACTIVATION();
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
