package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.List;
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
 * @see : 사용자 지정 설정(PARAM_MAP)에 DETECT_ALARM : Y 설정 정보 지정 시 탐색 경로 파일 발견 시 알람 발송 지원
 */
public abstract class Detect {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final boolean needAlarm;
	
	/**
	 * @param PRCS_NAME : 프로세스 명
	 * @param PARAM_MAP : 사용자 지정 설정 값 - 설정 파일 지정 가능
	 */
	public Detect(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		
		Object o =PARAM_MAP.get("DETECT_ALARM");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	/**
	 * @param path : 탐색 경로(절대 경로)
	 * @return	탐색된 파일 PATH 목록, 에러 발생 시 자체 처리 됨, NULL RETURN 시 알람 발송 지원, 탐색 파일 없을 경우 SIZE 0인 INSTANCE RETURN
	 */
	abstract public List<Path> process(Path path);
	
	public List<Path> detect(Path path){
		List<Path> pathList = process(path);
		
		if(pathList==null) return null; 
			
		if(needAlarm && pathList.size()>0) {
			String msg = PRCS_NAME+", "+path.toString()+" 경로, 파일 유입("+pathList.size()+")";
			sendAlarm(msg);
		}
		
		return pathList;
	}
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
