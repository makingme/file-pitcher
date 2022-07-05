package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.utils.AlarmManager;

/**
 * @author : URACLE KKB
 * @see : logger - 로거 인스턴스 멤버 변수
 * @see : PRCS_NAME - 프로세스 명 멤버 변수
 * @see : PARAM_MAP - Map(String, Object) 타입의 사용자 지정 설정 정보를 담은 멤버 변수
 * @see : 사용자 지정 설정(PARAM_MAP)에 POST_ALARM : Y 설정 정보 지정 시 전처리 실패 시 알람 발송 지원 
 */
public abstract class PostHandle {
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final boolean needAlarm;
	
	/**
	 * @param PRCS_NAME : 프로세스 명
	 * @param PARAM_MAP : 사용자 지정 설정 값 - 설정 파일 지정 가능
	 */
	public PostHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		
		Object o =this.PARAM_MAP.get("POST_ALARM");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	
	/**
	 * @see 필수 값 및 사용자 지정 멤버 변수 초기화
	 * @return 초기화 성공 여부
	 */
	abstract public boolean initailize();

	/**
	 * @param path : 처리할 파일 경로(절대 경로)
	 * @param isMainSuccess : : 본 처리 결과 - 본 처리 지정 안했다면 무조건 TRUE
	 * @return 본 처리 성공 여부, 실패 시 POST_ALARM 설정 여부에 따른 알람 발송
	 */
	abstract public boolean process(Path path, boolean isMainSuccess);
	
	public boolean  handle(Path path, boolean isMainSuccess) {
		boolean isOk = process(path, isMainSuccess);
		if(needAlarm && isOk == false) {
			sendAlarm(PRCS_NAME+", "+path.toString()+" 파일 후처리 중 에러 발생");
		}
		return isOk;
	}
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
