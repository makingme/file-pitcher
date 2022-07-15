package kr.uracle.ums.fpc.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.bean.config.ModuleConfigBean;
import kr.uracle.ums.sdk.util.UmsAlarmSender;

/**
 * @author : URACLE KKB
 * @see : logger - 로거 인스턴스 멤버 변수
 * @see : PRCS_NAME - 프로세스 명 멤버 변수
 * @see : PARAM_MAP - Map(String, Object) 타입의 사용자 지정 설정 정보를 담은 멤버 변수
 * @see : ALARM_CONFIG - 알람 설정 정보:발송채널, 발송UMS URL, 알람 고정 문구(PREFIX)
 * @see : 사용자 지정 설정(PARAM_MAP)에 DETECT_ALARM : Y 설정 정보 지정 시 탐색 경로 파일 발견 시 알람 발송 지원
 * @see	: 알람 Method 지원 : public void sendAlarm(String msg)
 */
public abstract class Detect {
	 
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final AlarmConfigBean ALARM_CONFIG;
	
	protected final boolean needAlarm;
	

	/**
	 * @param MODULE_CONFIG	: 모듈 설정 정보 - 모듈명, 알람여부, 구현클래스명, 지정 변수맵
	 * @param ALARM_CONFIG  : 알람 설정 정보 - 발송채널, 발송UMS URL, 알람 고정 문구(PREFIX)
	 */
	public Detect(ModuleConfigBean MODULE_CONFIG, AlarmConfigBean ALARM_CONFIG) {
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
	 * @see 탐색 디렉토리가 미 존재 시 자동 생성 함
	 * @param path : 탐색 경로(절대 경로)
	 * @return	탐색된 파일 PATH 목록, 에러 발생 시 자체 처리 됨, NULL RETURN 시 알람 발송 지원, 탐색 파일 없을 경우 SIZE 0인 INSTANCE RETURN
	 */
	abstract public List<Path> process(Path path);
	
	public List<Path> detect(Path path){
		List<Path> pathList = null;
		// 지정 경로 디렉토리 없으면 생성
		if(Files.exists(path) == false) {
			try {
				Files.createDirectories(path);
				logger.info("{} 디렉토리 생성", path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// 에러 캣치 처리
		try {
			pathList = process(path);			
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		if(pathList==null) return null; 
			
		if(needAlarm && pathList.size()>0) {
			String msg = PRCS_NAME+", "+path.toString()+" 경로, 파일 유입("+pathList.size()+")";
			sendAlarm(msg);
		}
		
		return pathList;
	}
	
	public void sendAlarm(String msg) {
		msg = ALARM_CONFIG.getPREFIX_MESSAGE()+msg;
		UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getURL(), msg);
	}
}
