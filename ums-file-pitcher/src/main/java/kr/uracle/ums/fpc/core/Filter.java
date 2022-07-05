package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.ArrayList;
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
 * @see : 사용자 지정 설정(PARAM_MAP)에 FILTER_ALARM : Y 설정 정보 지정 시 탐색 파일 중 필터링 파일 발생 시 알람 발송
 */
public abstract class Filter {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final boolean needAlarm;

	/**
	 * @param PRCS_NAME : 프로세스 명
	 * @param PARAM_MAP : 사용자 지정 설정 값 - 설정 파일 지정 가능
	 */
	public Filter(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		
		Object o =PARAM_MAP.get("FILTER_ALARM");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	/**
	 * @param pathList : Detect에서 탐색된 파일 PATH 목록(절대 경로)
	 * @return	필터링 된 파일 PATH 목록, 에러 발생 시 자체 처리 됨, NULL RETURN 시 알람 발송 지원
	 */
	abstract public List<Path> process(List<Path> pathList);
	
	
	public List<Path> filtering(List<Path> pathList){
		// 필터 파일 확인을 위한 목록 복사
		List<Path> oriList = new ArrayList<Path>(pathList.size());
		oriList.addAll(pathList);
		
		// 필터 처리
		List<Path> targetPathList= process(pathList);
		
		// NULL일경우 Pitcher에서 장애 처리 
		if(targetPathList == null) return null;
		
		// 필터 처리 여부 확인
		if(oriList.size() != targetPathList.size()) {
			String filterFile ="";
			int filterCnt =0;
			for(Path p : oriList) {
				if(targetPathList.contains(p) == false) {
					filterFile+=p.toString()+", ";
					filterCnt++;
				}
			}
			logger.info("필터 파일 정보:{}", filterFile);
			if(needAlarm) {
				String msg = PRCS_NAME+", 파일 필터("+filterCnt+") 처리 됨" ;
				sendAlarm(msg);
			}
		}
		
		return targetPathList;
	};
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
