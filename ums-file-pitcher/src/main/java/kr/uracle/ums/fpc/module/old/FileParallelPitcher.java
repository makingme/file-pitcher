package kr.uracle.ums.fpc.module.old;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.old.Handler;
import kr.uracle.ums.fpc.old.Pitcher;
import kr.uracle.ums.fpc.old.PitcherConfigBean;

public class FileParallelPitcher extends Pitcher{
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public FileParallelPitcher(PitcherConfigBean config, UmsMonitoringConfigBean monitConfig, String threadName) {
		super(config, monitConfig, threadName);
	}
	
	private int oldFileCnt = 0;
	
	@Override
	public boolean extraInit() { 
		handlerList = new ArrayList<Handler>(MAX_THREAD);
		HANDLER_CLASS = PITCHER_CONFIG.getHANDLER_CLASS();
		
		return true;
	}
	
	@Override
	public int checkFile(List<Path> list) {
		int size = list.size();

		if(FILE_MAX>0 && (size>= FILE_MAX) && preMaxFileCnt != size) {
			preMaxFileCnt = size;
			sendAlarm(PITCHER_CONFIG.getALARM(), "파일 최대 갯수 제한("+FILE_MAX+") 초과, 현재 파일 갯수:"+size);  
		}
		
		long now = System.currentTimeMillis();
		oldFileCnt = 0;
		
		Iterator<Path> iter = list.iterator();
		while(iter.hasNext()) {
			Path p = iter.next();
			
			// 지정 확장자 아닌 파일
			if(p.getFileName().toString().endsWith(FILE_EXTENTION) == false){
				try {
					Files.delete(p);
					iter.remove();
					log.info("지원 확장자({})외 파일({}) 삭제", FILE_EXTENTION, p.toString());
				} catch (IOException e) {
					log.error("지원 확장자({})외 파일({}) 삭제 중 오류:{}", FILE_EXTENTION, p.toString(), e);
				}
				continue;
			}
			
			// 장기 보관 파일 확인 후 알람
			int fileHashCode = p.hashCode();
			if(fileHistoryMap.get(fileHashCode) == null)fileHistoryMap.put(fileHashCode, now);
			 
			long past = fileHistoryMap.get(fileHashCode);
			if(STAY_LIMIT_TIME >0  &&  (now - past) >  STAY_LIMIT_TIME) {
				oldFileCnt +=1;
			}
			
			// 파일 보관 정책에 따른 삭제
			if(KEEP_TIME>0 && (now - p.toFile().lastModified()) > KEEP_TIME) {
				try {
					Files.delete(p);
					iter.remove();
					log.info("보관 시간 시간({}) 초과 파일({}) 삭제", KEEP_TIME, p.toString());
				} catch (IOException e) {
					log.error("보관 시간 시간({}) 초과 파일({}) 삭제 중 오류:{}", KEEP_TIME, p.toString(), e);
				}
			}
		}
		if(oldFileCnt <= 0) preOldFileCnt = 0;
		if(oldFileCnt > 0 && preOldFileCnt != oldFileCnt) {
			preOldFileCnt = oldFileCnt;
			log.info("장기 체류 파일({})개 발견으로 알람 발송", oldFileCnt);
			sendAlarm(PITCHER_CONFIG.getALARM(), STAY_LIMIT_TIME/1000 + "초 이상, 잔류 파일 발견("+oldFileCnt+")");  
		}else {
			oldFileCnt = 0;
		}
		
		return oldFileCnt;
		
	}

	@Override
	public int hadleFile(List<Path> list) {
		if(StringUtils.isBlank(HANDLER_CLASS)) {
			if(oldFileCnt <= 0)log.info("감시 전용 모드 작동 중...");
			return oldFileCnt;
		}
		// 현재 Activate 상태의 핸드러만 추출
		Iterator<Handler> iter = handlerList.iterator();
		while(iter.hasNext()) {
			Handler h = iter.next();
			if(h.isAlive() == false) iter.remove();
		}
		
		// 현재 가용 쓰레드 갯수 확인
		int freeHandlerCnt = MAX_THREAD - handlerList.size();
		if(freeHandlerCnt <= 0) {
			log.info("현재 모든 가용 쓰레드가 활성화 중입니다.");
		}
		
		// 가용 가능한 쓰레드 갯수 만큼 파일 처리
		int fileCnt = list.size();
		if(fileCnt > freeHandlerCnt) fileCnt =  freeHandlerCnt;
		
		int processCnt = 0;
		for(int i =0; i<fileCnt ; i++) {
			Path p = list.get(i);
			// 파일 존재 여부 확인
			if(Files.exists(p) == false) continue;

			// 파일 처리 핸들러 생성
			Handler h = generateHandler(PITCHER_CONFIG, MONIT_CONFIG, p, i+1);
			if(h == null) {
				log.error("{} 핸들러 생성 중 에러 발생", PITCHER_CONFIG.getHANDLER_CLASS());
				return processCnt;
			}
			// 파일 처리 핸들러가 파일 처리 하게 실행
			h.start();
			// 파일 처리 핸들러 추가
			handlerList.add(h);
			processCnt +=1;
		}
		
		return processCnt;
	}
}
