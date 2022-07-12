package kr.uracle.ums.fpc.old;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;

import kr.uracle.ums.fpc.bean.config.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.tps.TpsManager;

public abstract class Handler extends Thread{
		
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	protected final Gson gson = new Gson();
	
	protected final PitcherConfigBean PITCHER_CONFIG;
	protected final UmsMonitoringConfigBean MONIT_CONFIG;
	
	protected Path SOURCE_PATH;
	protected final Path SUCCESS_PATH;
	protected final Path FAIL_PATH;
	protected final String FILE_NAME;
	protected final String DELIMETER;
		
	protected final Map<String, String> headerMap = new HashMap<String, String>(50);
	protected final Map<String, String> CUSTOM_MAP;
	
	protected Path convertFilePath = null;
	
	protected int inputCnt = 0;
	protected int outputCnt = 0;
	
	public Handler(PitcherConfigBean pitcherConfig, UmsMonitoringConfigBean monitConfig, Path source, Integer index) { 
		this.PITCHER_CONFIG = pitcherConfig;
		this.MONIT_CONFIG = monitConfig;
		this.DELIMETER = PITCHER_CONFIG.getDELIMITER();
		this.SOURCE_PATH = source;	
		this.SUCCESS_PATH = Paths.get(pitcherConfig.getSUCCESS_PATH());
		this.FAIL_PATH = Paths.get(pitcherConfig.getFAIL_PATH());;
		this.FILE_NAME = SOURCE_PATH.getFileName().toString();
		
		CUSTOM_MAP = pitcherConfig.getCUSTOM_MAP();
		
	}
	
	abstract protected boolean preProcess(Path source);
	abstract protected boolean hadle(Path source, boolean isSuccess);
	abstract protected boolean postProcess(Path source, boolean isSuccess);
	
	@Override
	public void run() {
		boolean isSuccess =	preProcess(SOURCE_PATH);
		log.info("{} 파일 선처리 완료, 선결과:{}", SOURCE_PATH, isSuccess);
		isSuccess = hadle(SOURCE_PATH, isSuccess);
		log.info("{} 파일 처리 완료, 처리 결과:{}", SOURCE_PATH, isSuccess);
		isSuccess =  postProcess(SOURCE_PATH, isSuccess);
		log.info("{} 파일 후처리 완료,  후처리 결과:{}", SOURCE_PATH, isSuccess);
		if(inputCnt > 0 ) TpsManager.getInstance().addProcessCnt(inputCnt);
		
	}
	
	protected boolean parseFile(Path source) {
		String csvFile = source.getParent().toString()+File.separator+ FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+".csv";
		convertFilePath = Paths.get(csvFile);
		boolean isHeader = true;
		Map<String, String> map = null;
		
		inputCnt  = 0;
		try(BufferedReader reader = Files.newBufferedReader(source);
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile)), ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);	
		){
			String[] header = new String[PITCHER_CONFIG.getFILE_HEADERS().size()];
			int index =0;
			for(String s : PITCHER_CONFIG.getFILE_HEADERS()) {
				if(s.startsWith("{") == true) s = "#" + s;
				if(s.startsWith("#") == true) s = s.replace("#", "#{");
				if(s.startsWith("#{") == false) s = "#{" + s;
				if(s.endsWith("}") == false) s = s + "}";
				header[index] = s;
				index +=1;
			}
			
			writer.writeNext(header);
			String line = null;
			while((line = reader.readLine()) != null) {
				if(line.equals("")) {
					isHeader = false;
					continue;
				}
				if(isHeader) {
					
					map = gson.fromJson(line, new TypeToken<Map<String, String>>(){}.getType());
					for(Entry<String, String> e: map.entrySet()) {
						headerMap.put(e.getKey(), e.getValue());
					}
				}else {
					String[] array = line.split(DELIMETER);
					writer.writeNext(array);
					inputCnt+=1;
				}
			}
			writer.flush();
			TpsManager.getInstance().addInputCnt(inputCnt);
		}catch(IOException e) {
			log.error("{} 파일 처리 중 에러 발생:{}",source.toString(), e);
			return false;
		}catch(JsonSyntaxException | IllegalStateException e) {
			log.error("{} 파일 CSV로 변환 중 에러 발생:{}",source.toString(), e);
			return false;
		}
		
		return true;
	}
	
	protected boolean move(Path sorce, boolean isSuccess) {
		String targetPath = null;
		if(isSuccess) {
			log.info("{} 파일 처리 성공, {} 경로로 이동", SOURCE_PATH, SUCCESS_PATH);
			targetPath = SUCCESS_PATH.toString()+File.separator;
		}else {
			log.info("{} 파일 처리 실패, {} 경로로 이동", SOURCE_PATH, FAIL_PATH);
			targetPath = FAIL_PATH.toString()+File.separator;
		}			
		try {
			if(Files.exists(SOURCE_PATH))Files.move(SOURCE_PATH, Paths.get(targetPath+FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
			if(convertFilePath != null && Files.exists(convertFilePath) ) {
				Files.move(convertFilePath, Paths.get(targetPath+convertFilePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}

		} catch (IOException e) {
			log.error("파일 처리 후 {} 경로로 이동 중 에러 발생:{}", targetPath, e);
			return false;
		}
			
		return true;
	}
	
}
