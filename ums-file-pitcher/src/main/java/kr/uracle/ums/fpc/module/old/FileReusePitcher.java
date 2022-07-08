package kr.uracle.ums.fpc.module.old;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.config.bean.old.PitcherConfigBean;
import kr.uracle.ums.fpc.core.old.Pitcher;

public class FileReusePitcher extends Pitcher{

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	public FileReusePitcher(PitcherConfigBean config, UmsMonitoringConfigBean monitConfig, String threadName) {
		super(config, monitConfig, threadName);
	}

	@Override
	public boolean extraInit() { 
		Long stayLimitTime = PITCHER_CONFIG.getSTAY_LIMIT_TIME();
		if(stayLimitTime <= 0) {
			log.error("STAY_LIMIT_TIME 설정이 누락됨, 기동 실패");
			return false;
		}
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
		Iterator<Path> iter = list.iterator();
		while(iter.hasNext()) {
			Path p = iter.next();

			int fileHashCode = p.hashCode();
			if(fileHistoryMap.get(fileHashCode) == null)fileHistoryMap.put(fileHashCode, now);
			 
			long past = fileHistoryMap.get(fileHashCode);
			if((now - past) <=  STAY_LIMIT_TIME) {
				iter.remove();
			}
			 
		}
		return list.size();

	}

	@Override
	public int hadleFile(List<Path> list) {
		int oldFileCnt = list.size();
		if(oldFileCnt <= 0 ) {
			log.info("{} 경로 재처리 파일 없음", path);
			return oldFileCnt;
		}
		log.info("{} 경로 재처리 파일 {}개 발견", path, oldFileCnt);
		Iterator<Path> iter = list.iterator();
		long now = System.currentTimeMillis();
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
			
			String fileName = p.getFileName().toString();
			String t = targetPath+ fileName.substring(0, fileName.lastIndexOf(FILE_EXTENTION)-1)+"_RE"+now+"."+fileName.substring(fileName.lastIndexOf(FILE_EXTENTION));
			try {
				Files.move(p, Paths.get(t), StandardCopyOption.REPLACE_EXISTING);
				log.info("장기 체류 파일({}) 이동({})", p.toString(), t);
			} catch (IOException e) {
				log.error("장기 체류 파일({}) 이동({}) 중 오류:{}", p.toString(), t, e);
			}
		}
		
		return list.size();
	}

}
