package kr.uracle.ums.fpc.bean.config;

public class PitcherConfigBean {
	
	private String PATH;
	private int MAX_THREAD = 10;
	private Long CYCLE;
	private ModuleConfigBean DETECTION;
	private ModuleConfigBean FILTER;
	private ModuleConfigBean PREHANDLE;
	private ModuleConfigBean MAINHANDLE;
	private ModuleConfigBean POSTHANDLE;

	public String getPATH() { return PATH; }
	public void setPATH(String PATH) { this.PATH = PATH; }
	
	public int getMAX_THREAD() { return MAX_THREAD;	}
	public void setMAX_THREAD(int mAX_THREAD) { MAX_THREAD = mAX_THREAD; }	
	
	public Long getCYCLE() { return CYCLE; }
	public void setCYCLE(Long cYCLE) { CYCLE = cYCLE; }	
	
	public ModuleConfigBean getDETECTION() { return DETECTION; }
	public void setDETECTION(ModuleConfigBean dETECTION) { DETECTION = dETECTION; }
	
	public ModuleConfigBean getFILTER() { return FILTER; }
	public void setFILTER(ModuleConfigBean fILTER) { FILTER = fILTER; }
	
	public ModuleConfigBean getPREHANDLE() { return PREHANDLE;	}
	public void setPREHANDLE(ModuleConfigBean pREHANDLE) { PREHANDLE = pREHANDLE; }
	
	public ModuleConfigBean getMAINHANDLE() { return MAINHANDLE; }
	public void setMAINHANDLE(ModuleConfigBean mAINHANDLE) { MAINHANDLE = mAINHANDLE; }
	
	public ModuleConfigBean getPOSTHANDLE() { return POSTHANDLE; }
	public void setPOSTHANDLE(ModuleConfigBean pOSTHANDLE) { POSTHANDLE = pOSTHANDLE; }
	
}
