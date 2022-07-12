package kr.uracle.ums.fpc.old;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.DuplexConfigBean;
import kr.uracle.ums.fpc.bean.config.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.old.Pitcher.PitcherStatus;

public class FilePitcherManager extends Thread{

	private final Map<String, String> filePaths = new HashMap<String, String>(50);
	private static final Logger log = LoggerFactory.getLogger(FilePitcherManager.class);
	private static final int SLEEP_TIME = 5*1000;
	private static final int THREAD_HANGTIME = 60 * 1000;
	private static final int STANDARD_LEADTIME = 3 * 1000; 
	
	private boolean isRun = false;
	private final boolean isMaster;
	private final String DUPLEXING_FILE;
	
	private final UmsMonitoringConfigBean MONIT_CONFIG;
	private final DuplexConfigBean duplexConfigBean;
	private final Map<String, PitcherConfigBean> PITCHERS_CONFIG;
	private final List<Pitcher> pitcherList;
	

	public FilePitcherManager(RootConfigBean configBean) {
		this.MONIT_CONFIG = configBean.getUMS_MONIT();
		this.duplexConfigBean = configBean.getDUPLEX();

		this.PITCHERS_CONFIG = configBean.getPITCHERS();
		this.pitcherList = new ArrayList<Pitcher>(PITCHERS_CONFIG.size());
		
		this.isMaster = duplexConfigBean.isIS_MASTER();
		DUPLEXING_FILE = duplexConfigBean.getDUPLEXING_FILE();
		this.setName(this.getClass().getSimpleName());
	}
	
	public boolean init() {
		boolean isOk = false;
		for(Entry<String, PitcherConfigBean> e: PITCHERS_CONFIG.entrySet()) {
			String name = e.getKey();
			PitcherConfigBean fConfig = e.getValue();
			Pitcher pitcher = generatePitcher(fConfig, MONIT_CONFIG, name);
			isOk = pitcher.init();
			
			if(isOk == false) return false;
			pitcher.changeMaster(isMaster);
			pitcherList.add(pitcher);
			
			filePaths.put(name+"_PATH", fConfig.getPATH());
			filePaths.put(name+"_SUCCESS_PATH", fConfig.getSUCCESS_PATH());
			filePaths.put(name+"_FAIL_PATH", fConfig.getFAIL_PATH());
			if(StringUtils.isNotBlank(fConfig.getTARGET_PATH())) filePaths.put(name+"_TARGET_PATH", fConfig.getTARGET_PATH());
		}
		isRun = isOk;
		return true;
	}
	
	protected Pitcher generatePitcher(PitcherConfigBean pitcherConfig, UmsMonitoringConfigBean monitConfig, String name) {
		Pitcher pitcher = null;
		try {
			Class<?> targetClass = Class.forName(pitcherConfig.getPITCHER_CLASS());
			Constructor<?> ctor = targetClass.getDeclaredConstructor(PitcherConfigBean.class, UmsMonitoringConfigBean.class, String.class);
			pitcher = (Pitcher)ctor.newInstance(pitcherConfig, monitConfig, name);
		} catch (Exception e) {
			log.error("{} 클래스 생성 중 에러 발생, 에러:{}", pitcherConfig.getPITCHER_CLASS(), e);
			return null;
		}
		return pitcher;
	}
	
	public void executePitchers() {
		for(Pitcher f : pitcherList) {
			log.info("{} Pitcher Start..", f.getName());
			f.start();
		}
	}
	
	@Override
	public void run() {
		executePitchers();
		boolean isOk = false;
		Path dPath = Paths.get(DUPLEXING_FILE);
		while(isRun) {
			try {
				sleep(SLEEP_TIME);
				long now = System.currentTimeMillis();
				if(isMaster) {
					isOk = createDirectory();
					if(isOk == false) continue;
					
					isOk = monitoring(now);
					
					if(isOk) {
						if(Files.exists(dPath) == false) Files.createFile(dPath);
						FileTime f = FileTime.fromMillis(now);
						Files.setLastModifiedTime(dPath, f);
						continue;
					}
					changePitcherMaster(isOk);
					
				}else {
					isOk = needSwap(dPath, now);
					changePitcherMaster(isOk);
					if(isOk) {
						isOk = createDirectory();
						if(isOk == false) continue;
						isOk = monitoring(now);
						if(isOk == false) {
							// 알람
						}
					}
				}
				
			}catch(InterruptedException e) {
				log.error("FilePitcher Sleep 중 인터럽터 발생:{}", e);
			} catch (IOException e) {
				log.error("파일 처리 중 에러:{}", e);
			}

		}
	}
	
	private boolean monitoring(long milTime) {
		long now = milTime > 0 ? milTime : System.currentTimeMillis();
		for(Pitcher f : pitcherList) {
			long leadTime = f.getLeadTime();
			if(leadTime > STANDARD_LEADTIME ) log.warn("{} Pitcher 체크 필요, 수행 시간 {}", f.getName(), leadTime);
			
			if( f.getStatus() == PitcherStatus.POCCESS && now - f.getStartTime() > THREAD_HANGTIME) {
				log.error("{} Pitcher {}초 수행 소요.", f.getName(), (now - f.getStartTime())/1000);
				return false;
			}
		}
		return true;
	}
	 
	private void changePitcherMaster(boolean isMaster) {
		for(Pitcher f : pitcherList) {
			f.changeMaster(isMaster);
		}
	}
	
	private boolean needSwap(Path dPath, long now) {		
		try {
			if(Files.exists(dPath) == false) return false;
			FileTime f = Files.getLastModifiedTime(dPath);
			if((now -f.toMillis()) > duplexConfigBean.getEXPIRY_TIME()) {
				log.info("마스터 파일 만료, BACKUP FILE PITCHER 기동");
				return true;
			}
		}catch(IOException e) {
			log.error("{} 이중화 파일 처리 중 에러 발생:{}", DUPLEXING_FILE, e);
		}

		return false;
	}

	public boolean createDirectory() {
		for(Entry<String, String> element : filePaths.entrySet()) {
			String path = element.getValue();
			Path p = Paths.get(path);
			try {
				if(Files.isDirectory(p) == false) Files.createDirectories(p);
			} catch (IOException e) {
				log.error("{} 디렉토리 체크 중 에러 발생:{}", path, e);
				return false;
			}
		}
		return true;
	}
	
}
