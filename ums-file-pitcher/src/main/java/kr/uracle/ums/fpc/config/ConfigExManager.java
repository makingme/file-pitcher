package kr.uracle.ums.fpc.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import kr.uracle.ums.fpc.config.bean.DuplexConfigBean;
import kr.uracle.ums.fpc.config.bean.PitcherExConfigBean;
import kr.uracle.ums.fpc.config.bean.RootConfigExBean;
import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;

public class ConfigExManager {	

	private static final Logger log = LoggerFactory.getLogger(ConfigExManager.class);
	
	private final Gson gson = new Gson();
	private String baseFilePath ="./conf/configEx.json";
	private String baseCharset = "UTF-8";
	
	private RootConfigExBean rootConfig;
	
	public ConfigExManager() {
		this.baseFilePath = System.getenv("PITCHER_CONFIG") != null ? System.getenv("PITCHER_CONFIG") : this.baseFilePath;
		this.baseCharset = System.getenv("PITCHER_CHARSET") != null ? System.getenv("PITCHER_CHARSET") : this.baseCharset;
	}
	
	public ConfigExManager(String baseFilePath, String charset) {
		if(StringUtils.isBlank(baseFilePath)) baseFilePath =this.baseFilePath;
		if(StringUtils.isBlank(charset))  charset =this.baseCharset;
		
		this.baseFilePath =  baseFilePath;
		this.baseCharset = charset;
	}
	
	public boolean load() { return load(baseFilePath, baseCharset); }
	public boolean load(String filePath, String charSet) {
		String fileContent = null;
		try{
			// Root 설정 파일 내용 로드
			fileContent = loadJsonFile(filePath, charSet);
			if(StringUtils.isBlank(fileContent)) {
				log.error("{}설정 파일에 내용이 없음으로 인해 설정 로드 실패", filePath);
				return false;
			}
			
			// Bean Object로 변환
			rootConfig = gson.fromJson(fileContent,  new TypeToken<RootConfigExBean>(){}.getType());
			if(rootConfig == null) {
				log.error("{}설정 파일에 내용이 없음으로 인해 설정 로드 실패", filePath);
				return false;
			}
			
			// PITCHER 설정 확인
			if(ObjectUtils.isEmpty(rootConfig.getPITCHERS())) {
				log.error("{}설정 파일에 PITCHER 설정이 없음, 설정 로드 실패", filePath);
				return false;
			}
			
			// BASEPATH 값 유효성 확인 
			String basePath = rootConfig.getBASE_PATH();
			if(StringUtils.isBlank(basePath) || Paths.get(basePath).isAbsolute() == false) {
				log.error("BASE_PATH 설정 값 누락");
				return false;
			}
			
			if(Paths.get(basePath).isAbsolute() == false) {
				log.error("BASE_PATH 설정 값({})은 절대경로 만 가능, 설정 로드 실패", basePath);
				return false;
			}
			
			if(basePath.endsWith(File.separator) == false)  rootConfig.setBASE_PATH(basePath+File.separator);		
						
		
			// 이중화 설정
			DuplexConfigBean duplexConfig = rootConfig.getDUPLEX();
			// 이중화 파일 경로 보충
			String duplexFile = duplexConfig.getDUPLEXING_FILE();
			if(duplexConfig.isACTIVATION() && StringUtils.isBlank(duplexFile)) {
				log.error("이중화 파일 설정이 비어 있음, 설정 로드 실패");
				return false; 
			}
			if(Paths.get(duplexFile).isAbsolute() == false) {
				duplexConfig.setDUPLEXING_FILE(basePath+duplexFile);
			}
			
			long multiplyNum = 1000;
			long duplex_expiryTime = duplexConfig.getEXPIRY_TIME() * multiplyNum;
			duplexConfig.setEXPIRY_TIME(duplex_expiryTime > 0 ? duplex_expiryTime : 60 * 1000);
			
			// UMS 모니터링 설정
			UmsMonitoringConfigBean monitConfig = rootConfig.getUMS_MONIT();
			if(StringUtils.isBlank(monitConfig.getPROGRAM_ID()) || ObjectUtils.isEmpty(monitConfig.getUMS_IPADREESS())) {
				rootConfig.setUMS_MONIT(null);
			}
			
			if(StringUtils.isBlank(monitConfig.getSERVER_ID())) {
				monitConfig.setSERVER_ID(InetAddress.getLocalHost().getHostName()+"_"+monitConfig.getPROGRAM_ID());
			}
			
			if(StringUtils.isBlank(monitConfig.getSERVER_NAME())) {
				monitConfig.setSERVER_NAME(monitConfig.getSERVER_ID());;
			}
			
			long monit_cycleTime = monitConfig.getCYCLE_TIME() * multiplyNum;
			monitConfig.setCYCLE_TIME(monit_cycleTime);
			
			for(Entry<String, PitcherExConfigBean> e :rootConfig.getPITCHERS().entrySet()) {
				String name = e.getKey();
				PitcherExConfigBean pitcherConfigBean = e.getValue();
								
				// Fetch 감시 경로 설정
				String path = pitcherConfigBean.getPATH();
				if(StringUtils.isBlank(path) ) {
					log.error("{} PATH 설정이 없음, 설정 로드 실패", name);
					return false;				
				}
				
				if(Paths.get(path).isAbsolute() == false ) {
					path = basePath + path;
				}
				
				if(path.endsWith(File.separator) == false) path += File.separator;

				pitcherConfigBean.setPATH(path);
			}
			
			log.info("{}설정 파일 정상 로드", filePath);
		}catch (FileNotFoundException e) {
			log.error("RootManager 설정 파일({})이 없습니다.", filePath);
		}catch (IOException e) {
			log.error("RootManager 설정 파일({}) 처리중 IO 에러 발생", filePath);
		}catch(JsonSyntaxException e) {
			log.error("RootManager 설정 파일({}) JSON 포맷 이상", filePath);
		}catch(Exception e) {
			log.error("RootManager 설정 파일({}) 로딩 중 에러 발생:{}", filePath, e);
		}
		return true;
	}
	
	private String loadJsonFile(String filePath, String charSet) throws IOException {
		String fileContent = null;
		File resource=new File(filePath);
		if(!resource.exists()) {
			log.error("{} 파일을 찾을 수 없습니다.", filePath);
			return null;
		}
		try( InputStream is = new FileInputStream(resource);
				BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charSet)))	) {
			
			StringBuilder sb=new StringBuilder();
			String val=null;
			while((val=br.readLine()) !=null) sb.append(val);
			fileContent = sb.toString();
		}
		return fileContent;
	}
	
	
	public RootConfigExBean getRootConfig() { return rootConfig; }

	public String getBaseFilePath() { return baseFilePath; }

}
