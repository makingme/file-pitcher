package kr.uracle.ums.fpc.bean.config;

public class DuplexConfigBean {

	enum DuplexSupportType{
		FILE, DB, API
	}
	private boolean ACTIVATION = false;
	private DuplexSupportType DUPLEX_TYPE = DuplexSupportType.FILE; 
	private boolean IS_MASTER = true;
	private String DUPLEXING_FILE = "MASTER.duplex";
	
	private long EXPIRY_TIME = 3*60*1000;
	
	public long getEXPIRY_TIME() { return EXPIRY_TIME;	}
	public void setEXPIRY_TIME(long eXPIRY_TIME) { EXPIRY_TIME = eXPIRY_TIME; }
	
	public boolean isACTIVATION() { return ACTIVATION; }
	public void setACTIVATION(boolean aCTIVATION) { ACTIVATION = aCTIVATION; }

	public DuplexSupportType getDUPLEX_TYPE() { return DUPLEX_TYPE; }
	public void setDUPLEX_TYPE(DuplexSupportType dUPLEX_TYPE) { DUPLEX_TYPE = dUPLEX_TYPE;	}
	
	public boolean isIS_MASTER() { return IS_MASTER; }
	public void setIS_MASTER(boolean iS_MASTER) { IS_MASTER = iS_MASTER; }
	
	public String getDUPLEXING_FILE() { return DUPLEXING_FILE;	}
	public void setDUPLEXING_FILE(String dUPLEXING_FILE) { DUPLEXING_FILE = dUPLEXING_FILE;	}
	

	
}
