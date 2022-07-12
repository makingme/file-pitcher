package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.sdk.util.UmsAlarmSender;

/**
 * @author : URACLE KKB
 * @see : logger - 로거 인스턴스 멤버 변수
 * @see : PRCS_NAME - 프로세스 명 멤버 변수
 * @see : PARAM_MAP - Map(String, Object) 타입의 사용자 지정 설정 정보를 담은 멤버 변수
 * @see : ALARM_CONFIG - 알람 설정 정보:발송채널, 발송UMS URL, 알람 고정 문구(PREFIX)
 * @see : 사용자 지정 설정(PARAM_MAP)에 FILTER_ALARM : Y 설정 정보 지정 시 탐색 파일 중 필터링 파일 발생 시 알람 발송
 * @see	: 알람 Method 지원 : public void sendAlarm(String msg)
 */
public abstract class Filter {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final AlarmConfigBean ALARM_CONFIG;
	
	protected final boolean needAlarm;

	/**
	 * @param PRCS_NAME : 프로세스 명
	 * @param PARAM_MAP : 사용자 지정 설정 값 - 설정 파일에서 값 지정 가능
	 * @param ALARM_CONFIG : 알람 설정 정보 - 발송채널, 발송UMS URL, 알람 고정 문구(PREFIX) 
	 */
	public Filter(String PRCS_NAME, Map<String, Object> PARAM_MAP, AlarmConfigBean ALARM_CONFIG) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		this.ALARM_CONFIG = ALARM_CONFIG;
		
		Object o =PARAM_MAP.get("FILTER_ALARM");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	/**
	 * @see 필수 값 및 사용자 지정 멤버 변수 초기화, 별도 초기화 멤버 없으면 TRUE RETURN
	 * @return 초기화 성공 여부
	 */
	abstract public boolean initailize();
	
	/**
	 * @param pathList : Detect에서 탐색된 파일 PATH 목록(절대 경로)
	 * @return	필터링 된 파일 PATH 목록, 에러 발생 시 NULL RETURN - 알람 발송 함
	 */
	abstract public List<Path> process(List<Path> pathList);
	
	
	public List<Path> filtering(List<Path> pathList){
		List<Path> targetPathList = null;
		
		// 필터 파일 확인을 위한 목록 복사
		List<Path> oriList = new ArrayList<Path>(pathList.size());
		oriList.addAll(pathList);
		
		// 에러 캣치 처리
		try {
			// 필터 처리
			targetPathList= process(pathList);			
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
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
	}
	
	public void sendAlarm(String msg) {
		msg = ALARM_CONFIG.getPREFIX_MESSAGE()+msg;
		UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getURL(), msg);
	}
}
