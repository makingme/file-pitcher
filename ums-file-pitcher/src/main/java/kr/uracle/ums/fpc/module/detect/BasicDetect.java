package kr.uracle.ums.fpc.module.detect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.core.Detect;

/**
 * @author : URACLE KKB
 * @see : 지정 경로의 파일 탐색 - 쓰기 가능한 상태이며, 지정경로에 WATCH_TIME(15초) 이상 사이즈가 동일한 파일 탐색
 * @see : 사용자 지정 설정(PARAM_MAP)에 WATCH_TIME : 1000(mils) 설정 정보 지정 시 파일 탐색 시간 시간 변동 가능(기본: 15*1000)
 */
public class BasicDetect extends Detect{

	private Map<File, String> fileHistory = new HashMap<File, String>();
	private long WATCH_TIME = 15*1000;
	private String PATTERN;
	
	public BasicDetect(String PRCS_NAME, Map<String, Object> PARAM_MAP, AlarmConfigBean alramConfig) {
		super(PRCS_NAME, PARAM_MAP, alramConfig);
	}

	@Override
	public boolean initailize() {
		Object wObj = PARAM_MAP.get("WATCH_TIME");
		if(wObj != null) {
			long wTime = Long.valueOf(wObj.toString().replaceAll("\\D", ""));
			if(wTime != 0)WATCH_TIME = wTime;
		}
		PATTERN = PARAM_MAP.get("PATTERN")!=null?PARAM_MAP.get("PATTERN").toString():".+";
		return true;
	}
	
	@Override
	public List<Path> process(Path path) {
		List<Path> targetFileList = new ArrayList<Path>();
		try {
			// 파일 탐색
			List<Path> list = Files.walk(path).filter(p -> p.toFile().isFile() && Pattern.matches(PATTERN, p.getFileName().toString())).collect(Collectors.toList());
			
			// 지정 경로의 파일이 없으면 빈 인스턴스 리턴
			if(ObjectUtils.isEmpty(list)) {
				// 파일 히스토리에 정보가 있다면 별도의 방법으로 파일이 지워짐으로 간주 - 로그 출력
				if(fileHistory.size() > 0) {
					for(Entry<File, String> element : fileHistory.entrySet()) {
						File f = element.getKey();
						logger.warn("{} 파일이 사라짐", f.getName());
					}
				}
				return targetFileList;
			}

			long now = System.currentTimeMillis();
			int totalCnt = list.size();
			int writeCnt = 0;
			int newCnt = 0;
			int remainCnt =0;
			int prcsCnt = 0;
			for(Path p : list) {
				// 쓰기 불가능 파일은 생성 중 파일로 지정
				if(Files.isWritable(p) == false) {
					writeCnt++;
					continue;
				}
				
				// 파일 히스트로 확인
				String fileInfo = fileHistory.get(p.toFile());
				
				// 신규 유입 파일은 히스토리에 등록 후 스킵
				if(StringUtils.isBlank(fileInfo)){
					fileHistory.put(p.toFile(), now+"_"+p.toFile().length());
					newCnt++;
					continue;
				}
				
				Long regTime = Long.parseLong(fileInfo.split("_")[0]);
				Long fileSize = Long.parseLong(fileInfo.split("_")[1]);
								
				// 파일 사이즈 체크
				if(p.toFile().length() != fileSize) {
					fileHistory.put(p.toFile(), regTime+"_"+p.toFile().length());
					remainCnt++;
					continue;
				}
				
				// 기준 시간 미달 파일은 스킵
				if(WATCH_TIME > (now - regTime)) {
					remainCnt++;
					continue;
				}
				
				// 기준 시간, 사이즈 변동 만족 파일 히스토리에서 삭제
				fileHistory.remove(p.toFile());
				
				// 모든 기준 조건 만족 파일 타겟(처리파일) 목록에 추가
				targetFileList.add(p);
				prcsCnt++;
			}
			logger.info("총 파일:{}, 쓰는중:{}, 신규파일:{}, 감시파일:{}, 처리파일:{}", totalCnt, writeCnt, newCnt, remainCnt, prcsCnt);
		 }catch (Exception e) {
			targetFileList = null;
			logger.error("{} 파일 탐색 중 에러 발생:{}", path.toString(), e);
			e.printStackTrace();
		}
		return targetFileList;
	}
	
	public static void main(String[] args) {
		Detect d = new BasicDetect("", new HashMap<String, Object>(), new AlarmConfigBean());
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				while(true){
					Path path = Paths.get("D:\\TEST\\PITCHER\\RECEIVE\\");
					List<Path> targetFileList = d.process(path);
					for(Path p: targetFileList) {
						System.out.println("처리 파일:"+p.toString());
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(false);
		t.start();
	}
}
