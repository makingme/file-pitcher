package kr.uracle.ums.fpc.core.old;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.bean.AlarmConfigBean;
import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.config.bean.old.PitcherConfigBean;

public abstract class Pitcher extends Thread{

	public enum PitcherStatus{
		READY, POCCESS, DONE
	}
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	// 파일 잔류 히스토리 저장
	protected final LinkedHashMap<Integer, Long> fileHistoryMap;
	
	protected PitcherStatus status = PitcherStatus.READY;
		
	protected final  Calendar calendar = Calendar.getInstance();
	
	protected final int LIMIT_PROCESSING = 20;
	protected final String PATTEN = "\\$\\{[^\\s,;\\(\\)\\*\\+\\-]+\\}";
	
	protected final String NAME;
	protected final int FILE_MAX;
	protected final int MAX_THREAD;
	protected final String FILE_EXTENTION;
	
	protected final long KEEP_TIME;
	protected final long STAY_LIMIT_TIME;
	
	protected final PitcherConfigBean PITCHER_CONFIG;
	protected final UmsMonitoringConfigBean MONIT_CONFIG;
	
	protected final String SUCCESS_PATH;
	protected final String FAIL_PATH;
	
	protected String path;
	protected String targetPath;
	
	protected long CYCLE = 3000;
	protected boolean isRun = false;
	protected boolean isMaster = false;
	
	protected List<Handler> handlerList;
	protected String HANDLER_CLASS;
	
	private long startTime = 0;
	private long leadTime =0;
	
	protected int preMaxFileCnt = 0;
	protected int preOldFileCnt = 0;
		
	public Pitcher(PitcherConfigBean config, UmsMonitoringConfigBean monitConfig,String threadName) {

		this.PITCHER_CONFIG = config;
		this.MONIT_CONFIG = monitConfig;
		
		String successPath = config.getSUCCESS_PATH();
		String failPath = config.getFAIL_PATH();
				
		int maxThreadCnt = config.getMAX_THREAD();
		if(maxThreadCnt > LIMIT_PROCESSING || maxThreadCnt <= 0) maxThreadCnt = LIMIT_PROCESSING;
		
		String extention = config.getFILE_EXTENTION();
		if(StringUtils.isBlank(extention)) extention = ".txt";
		
		NAME = threadName;
		SUCCESS_PATH = successPath;
		FAIL_PATH = failPath;
		MAX_THREAD = maxThreadCnt;
		FILE_EXTENTION = extention;
		FILE_MAX = config.getFILE_MAX();
		STAY_LIMIT_TIME = config.getSTAY_LIMIT_TIME() > 0 ? config.getSTAY_LIMIT_TIME() : 0;
		KEEP_TIME = config.getKEEP_TIME() > 0 ? config.getKEEP_TIME() : 0;
		this.setName(NAME);
		
		fileHistoryMap = new LinkedHashMap<Integer, Long>(FILE_MAX, .75f, true){
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Map.Entry<Integer, Long> eldest) {
					return size() >= FILE_MAX;
			}
		};
		
	}
	
	
	public boolean init(){
		boolean isOk = true;

		if(PITCHER_CONFIG == null) {
			log.error("{} 설정 정보가 없습니다.", NAME);
			return false;
		}

		path = PITCHER_CONFIG.getPATH();
		 
		String targetPath = PITCHER_CONFIG.getTARGET_PATH();
		if(StringUtils.isNotBlank(targetPath)) this.targetPath = targetPath;
		
		CYCLE = PITCHER_CONFIG.getCYCLE_TIME()>0 ? PITCHER_CONFIG.getCYCLE_TIME(): CYCLE;
		
		isOk = extraInit();
		
		isRun = isOk;
		return isOk;
	}
	
	// 추가 설정
	abstract public boolean extraInit();
	// 파일 체크
	abstract public int checkFile(List<Path> list);
	// 파일 처리
	abstract public int hadleFile(List<Path> list);
	
	public void run() {
		
		while(isRun) {
			status = PitcherStatus.READY;
			try {
				sleep(CYCLE);
			} catch (InterruptedException e) {
				log.error("{} 대기 중 에러 발생:{}", this.getName(), e);
			}
			
			// 마스터가 아니면 휴식, 매니저가 마스터 파일을 감시하여 이상 시 슬레이브에서 마스터로 전환 함
			if(isMaster == false) {
				log.info("[{}]  현재 슬레이브로 {} 동안 휴식", NAME, CYCLE);
				continue;
			}
			
			status = PitcherStatus.POCCESS;
			// 수행 시작 시간 - 매니저에서 Hang 여부 판단을 위해 기록해둠 
			startTime = System.currentTimeMillis();
			
			// 지정 경로 파일 탐색 - 사이즈 0, 지정 파일확장자외 파일은 FAIL 경로로 옮기고 나머지 파일 목록을 전달함
			List<Path> list = search(Paths.get(path));
			log.debug("{} 경로, 파일 {}개 탐색, 탐색 소요시간(ms):{}", path, list.size(), System.currentTimeMillis() - startTime);
			// 탐색된 파일 없으면 대기
			if(ObjectUtils.isEmpty(list)) {
				log.info("{} 경로 파일 없음, {} 동안 휴식", path, CYCLE);
				leadTime = System.currentTimeMillis() - startTime;
				status = PitcherStatus.DONE;
				continue;
			}
			
			// 파일 체크 - 기준 시간 초과 존재 파일 - 미처리 파일, 장기 보관 파일 - 삭제 대상 파일 
			if(STAY_LIMIT_TIME >0 || KEEP_TIME>0 ) {
				checkFile(list);
			}
			
			// 파일 처리
			int cnt = hadleFile(list);
			leadTime = System.currentTimeMillis() - startTime;
			if(cnt>0) log.info("{} 경로, 파일 처리 완료, 처리 소요시간(ms):{}", path, leadTime);
			status = PitcherStatus.DONE;
		}
	}
	
	protected List<Path> search(Path path){
		List<Path> list = null;
		String	failPath = null;
		try {
			list = Files.walk(path).filter(p -> p.toFile().isFile()).collect(Collectors.toList());
			Iterator<Path> iter = list.iterator();
			while(iter.hasNext()) {
				Path p = iter.next();
				if(Files.isWritable(p) == false) {
					iter.remove();
					continue;
				}
				if(Files.size(p) > 0) continue;
				
				failPath = FAIL_PATH + p.getFileName().toString();
				Files.move(p, Paths.get(failPath), StandardCopyOption.REPLACE_EXISTING);	
				log.info("사이즈 0인 파일{} 발견, {} 경로로 이동", p.toString(), failPath);
				iter.remove();
			}

		 }catch (IOException e) {
			 list = null;
			log.error("{} 파일 탐색 중 에러 발생:{}", path.toString(), e);
			e.printStackTrace();
		}
		return list;
	}

	protected Handler generateHandler(PitcherConfigBean pitcherConfig, UmsMonitoringConfigBean monitConfig, Path source, int index) {
		Handler handler = null;
		try {
			Class<?> targetClass = Class.forName(pitcherConfig.getHANDLER_CLASS());
			Constructor<?> ctor = targetClass.getDeclaredConstructor(PitcherConfigBean.class, UmsMonitoringConfigBean.class, Path.class, Integer.class);
			handler = (Handler)ctor.newInstance(pitcherConfig, monitConfig, source, index);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return handler;
	}
	
	protected String getNow(String format){
		if(StringUtils.isBlank(format)) format = "yyyyMMddHHmmss";
		SimpleDateFormat f = null;
		try {
			f = new SimpleDateFormat (format);
		}catch(IllegalArgumentException e) {
			f = new SimpleDateFormat ("yyyyMMddHHmmss");
		}
		return f.format(calendar.getTime());
	}
	
	// 알람 관련 클래스 개발
	protected void sendAlarm(AlarmConfigBean alarm, String addMessage) {
		
	}
	
	private String getSubDirectoryName(String pattenContext) {
		return null;
	}
	
	public void changeMaster(boolean isMaster) { this.isMaster = isMaster;	}
	
	public long getStartTime() { return startTime; }
	
	public long getLeadTime() { return leadTime;}
	
	public PitcherStatus getStatus() { return status; }
	
}
