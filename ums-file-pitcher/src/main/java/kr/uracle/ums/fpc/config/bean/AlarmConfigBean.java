package kr.uracle.ums.fpc.config.bean;

import java.util.ArrayList;
import java.util.List;

public class AlarmConfigBean {

	enum CHANNELS {
	    PUSH, 
	    KKOALT, KKOFRT, 
	    RCS_SMS, RCS_LMS, RCS_MMS, RCS_FREE, RCS_CELL, RCS_DESC, 
	    SMS, LMS, MMS, 
	    NAVERT;
	}
	
	private CHANNELS SEND_CHANNEL;
	private List<String> TARGETS = new ArrayList<String>(10);
	private String MESSAGE;
	
	public CHANNELS getSEND_CHANNEL() { return SEND_CHANNEL; }
	public void setSEND_CHANNEL(CHANNELS sEND_CHANNEL) { SEND_CHANNEL = sEND_CHANNEL; }
	
	public List<String> getTARGETS() { return TARGETS; }
	public void setTARGETS(List<String> tARGETS) { TARGETS = tARGETS; }
	
	public String getMESSAGE() { return MESSAGE; }
	public void setMESSAGE(String mESSAGE) { MESSAGE = mESSAGE;	}
	
	
}
