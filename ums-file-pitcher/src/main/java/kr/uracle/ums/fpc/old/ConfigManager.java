package kr.uracle.ums.fpc.old;

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

import kr.uracle.ums.fpc.bean.config.DuplexConfigBean;
import kr.uracle.ums.fpc.bean.config.UmsMonitoringConfigBean;

public class ConfigManager {	
	private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
	
	private final Gson gson = new Gson();
	private String baseFilePath ="./conf/config.json";
	private String baseCharset = "UTF-8";
	
	private RootConfigBean rootConfig;
	
	public ConfigManager() {
		this.baseFilePath = System.getenv("PITCHER_CONFIG") != null ? System.getenv("PITCHER_CONFIG") : this.baseFilePath;
		this.baseCharset = System.getenv("PITCHER_CHARSET") != null ? System.getenv("PITCHER_CHARSET") : this.baseCharset;
	}
	
	public ConfigManager(String baseFilePath, String charset) {
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
			rootConfig = gson.fromJson(fileContent,  new TypeToken<RootConfigBean>(){}.getType());
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
			if(StringUtils.isNotBlank(basePath)) {
				if(Paths.get(basePath).isAbsolute() == false) {
					log.error("BASE_PATH 설정 값은 절대경로 만 가능, 설정 로드 실패");
					return false;
				}
				if(basePath.endsWith(File.separator) == false) basePath += File.separator;				
			}
						
			// 지정 시간단위에 따른 지정시간 값 변경
			String timeUnit = rootConfig.getTIME_UNIT();
			timeUnit = StringUtils.isBlank(timeUnit)?"SEC":timeUnit;
			long multiplyNum = 1;
			switch(timeUnit) {
				case "HOUR":
					multiplyNum = 60*60*1000;
					break;
				case "MIN":
					multiplyNum = 60*1000;
					break;
				case "SEC":
					multiplyNum = 1000;
					break;
				default:
					multiplyNum = 1;
					break;
			}
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
			
			for(Entry<String, PitcherConfigBean> e :rootConfig.getPITCHERS().entrySet()) {
				String name = e.getKey();
				PitcherConfigBean pitcherConfigBean = e.getValue();
				
				// 시간 값  설정에 따른 재 설정
				long cycleTime = pitcherConfigBean.getCYCLE_TIME() * multiplyNum;
				pitcherConfigBean.setCYCLE_TIME(cycleTime);
				
				long stayLimitTime = pitcherConfigBean.getSTAY_LIMIT_TIME() *  multiplyNum;
				pitcherConfigBean.setSTAY_LIMIT_TIME(stayLimitTime);
				
				long keepTime = pitcherConfigBean.getKEEP_TIME() *  multiplyNum;
				pitcherConfigBean.setKEEP_TIME(keepTime);
				
				// Fetch 감시 경로 설정
				String path = pitcherConfigBean.getPATH();
				if(StringUtils.isBlank(path) ) {
					if(StringUtils.isBlank(basePath)) {
						log.error("{} PATH 설정이 없음, 설정 로드 실패", name);
						return false;						
					}
					path = basePath + name;
				}
				
				if(Paths.get(path).isAbsolute() == false ) {
					if(StringUtils.isBlank(basePath)) {
						log.error("{} 상대경로 지정으로 BASE_PATH 설정이 필요함, 설정 로드 실패", name);
						return false;						
					}
					path = basePath + path;
				}
				
				if(path.endsWith(File.separator) == false) path += File.separator;

				pitcherConfigBean.setPATH(path);
				
				// 대상 파일 처리 경로
				String targetPath = pitcherConfigBean.getTARGET_PATH();
				if(StringUtils.isNotBlank(targetPath)) {
					if(Paths.get(targetPath).isAbsolute() == false) {
						if(StringUtils.isBlank(basePath)) {
							log.error("{} 상대경로 지정으로 BASE_PATH 설정이 필요함, 설정 로드 실패", name);
							return false;							
						}
						targetPath = basePath + targetPath;
					}
					if(targetPath.endsWith(File.separator) == false) targetPath += File.separator;
					
					pitcherConfigBean.setTARGET_PATH(targetPath);
				}
				
				// 성공 시 파일 지정 경로
				String successPath = pitcherConfigBean.getSUCCESS_PATH();
				if(StringUtils.isBlank(successPath)) {
					if(StringUtils.isBlank(basePath) ) {
						log.error("BASE_PATH 설정 값 없음, 설정 로드 실패");
						return false;
					}
					pitcherConfigBean.setSUCCESS_PATH(basePath+name.toUpperCase()+"_SUCCESS"+File.separator);

				}else {
					if(Paths.get(successPath).isAbsolute() == false)successPath = basePath+successPath;
					if(successPath.endsWith(File.separator) == false) successPath += File.separator;
					pitcherConfigBean.setSUCCESS_PATH(successPath);
				}
				
				// 실패 시 파일 지정 경로
				String failPath = pitcherConfigBean.getFAIL_PATH();
				if(StringUtils.isBlank(failPath)) {
					if(StringUtils.isBlank(basePath) ) {
						log.error("BASE_PATH 설정 값 없음, 설정 로드 실패");
						return false;
					}
					pitcherConfigBean.setFAIL_PATH(basePath+name.toUpperCase()+"_FAIL"+File.separator);

				}else {
					if(Paths.get(failPath).isAbsolute() == false)failPath = basePath+failPath;
					if(failPath.endsWith(File.separator) == false) failPath += File.separator;
					pitcherConfigBean.setFAIL_PATH(failPath);
				}
				
				if(pitcherConfigBean.getALARM() == null)pitcherConfigBean.setALARM(rootConfig.getALARM());
			}
			
			rootConfig.setBASE_PATH(basePath);
			
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
		try(	InputStream is = new FileInputStream(resource);
				BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charSet)))	) {
			StringBuilder sb=new StringBuilder();
			String val=null;
			while((val=br.readLine()) !=null) sb.append(val);
			fileContent = sb.toString();
		}
		return fileContent;
	}
	
	public RootConfigBean getRootConfigBean() { return rootConfig; }
	public String getBaseFilePath() { return baseFilePath; }
}
