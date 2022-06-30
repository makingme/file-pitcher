package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.utils.AlarmManager;

public abstract class Roguing {
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	
	protected final String PRCS_NAME;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final boolean needAlarm;

	public Roguing(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
		this.PRCS_NAME = PRCS_NAME;
		Object o =PARAM_MAP.get("ROGUING_ALARM_USEYN");
		String yn = ObjectUtils.isEmpty(o)?"N":o.toString();
		needAlarm = yn.equalsIgnoreCase("Y")?true:false;
	}
	
	abstract public List<Path> process(List<Path> pathList);
	
	public List<Path> roguing(List<Path> pathList){
		List<Path> oriList = new ArrayList<Path>(pathList.size());
		oriList.addAll(oriList);
		List<Path> targetPathList= process(pathList);
		
		if(targetPathList == null) return null;
		
		if(oriList.size() != targetPathList.size()) {
			String filterFile ="";
			int filterCnt =0;
			for(Path p : oriList) {
				if(targetPathList.contains(p) == false) {
					filterFile+=p.toString()+", ";
					filterCnt++;
				}
			}
			log.info("필터 파일 정보:{}", filterFile);
			if(needAlarm) sendAlarm(PRCS_NAME+", 파일 필터("+filterCnt+") 처리 됨");
		}
		
		return targetPathList;
	};
	
	public void sendAlarm(String msg) {
		AlarmManager.getInstance().sendAlarm(msg);
	}
}
