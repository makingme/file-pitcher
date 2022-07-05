package kr.uracle.ums.fpc.core;

import java.nio.file.Path;

public class HandlerEx extends Thread{
	
	private final Path TARGET_PATH;
	
	private PreHandle preHandler = null;
	private MainHandle mainHandler = null;
	private PostHandle postHandler = null;
	
	
	
	public HandlerEx(Path TARGET_PATH) {
		this.TARGET_PATH = TARGET_PATH;
	}

	@Override
	public void run() {
		boolean isOk = true;
		if(preHandler != null) {
			isOk = preHandler.handle(TARGET_PATH);
		}
		
		if(mainHandler != null) {
			isOk = mainHandler.handle(TARGET_PATH, isOk);	
		}
		
		if(postHandler != null) {
			isOk = postHandler.handle(TARGET_PATH, isOk);
		}
	}
	
}
