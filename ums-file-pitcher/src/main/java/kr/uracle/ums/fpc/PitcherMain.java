package kr.uracle.ums.fpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.config.bean.old.ConfigManager;
import kr.uracle.ums.fpc.config.bean.old.RootConfigBean;
import kr.uracle.ums.fpc.core.old.FilePitcherManager;
import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.fpc.tps.TpsManager;

public class PitcherMain {
	
	private final static Logger log = LoggerFactory.getLogger(PitcherMain.class);
	
	public static void main(String[] args) {
		
		// 설정 매니저 로딩
		ConfigManager configManager = new ConfigManager();
		boolean isOk = configManager.load();
		if(isOk == false)return;
		
		// 설정 빈 가져오기
		RootConfigBean rootConfig = configManager.getRootConfigBean();
		
		// 모니터링 설정 가져오기
		UmsMonitoringConfigBean monitConfig = rootConfig.getUMS_MONIT();
		// 모니터링 서버 체크 TCP 모듈 기동
		TcpAliveConManager.getInstance().init(null, monitConfig.getUMS_IPADREESS(), monitConfig.getCYCLE_TIME());
		// 모니터링 전송 쓰레드 기동
		TpsManager.initialize(monitConfig);
		
		// 파일 피쳐 매니저 기동
		FilePitcherManager manager = new FilePitcherManager(rootConfig);
		manager.init();
		isOk = manager.createDirectory();
		if(isOk) {
			manager.start();
		}else {
			log.error("파일 디렉토리 생성 에러로 인한 구동 실패");
		}
	}
}
