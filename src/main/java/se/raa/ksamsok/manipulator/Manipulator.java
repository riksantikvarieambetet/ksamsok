package se.raa.ksamsok.manipulator;

public interface Manipulator extends Runnable 
{
	String getStatus();
	
	String getName();
	
	void stopThread();
	
	boolean isRunning();
}
