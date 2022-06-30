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
 * @see : PARAM_MAP - Map(String, Object) 타입의 사용자 지정 설정 값을 담은 변수 
 */
public abstract class Detect {
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	/**
	 * @see 사용자 지정 변수 값
	 */
	protected final Map<String, Object> PARAM_MAP;
	
	protected final boolean needAlarm;
	
	public Detect(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		
		Object o =PARAM_MAP.get("DETECT_ALARM_USEYN");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	/**
	 * @param path : 탐색 경로(절대 경로)
	 * @param pattern : 탐색 파일명 정규식 패턴 (확장자 포함 전체 파일명, NULL 혹은 빈값 일 경우 패턴 적용 없이 모든 파일 탐색 대상)
	 * @return	탐색된 파일 PATH 목록, 에러 발생 시 자체 처리 후 NULL RETURN(알람대상), 탐색 파일 없을 경우 SIZE 0인 INSTANCE RETURN
	 */
	abstract public List<Path> process(Path path, String pattern);
	
	public List<Path> detect(Path path, String pattern){
		List<Path> pathList = process(path, pattern);
		
		if(pathList==null) return null; 
			
		if(needAlarm && pathList.size()>0) {
			String msg =PRCS_NAME+", "+path.toString()+" 경로, 파일 유입("+pathList.size()+")";
			sendAlarm(msg);
		}
		
		return pathList;
	}
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
