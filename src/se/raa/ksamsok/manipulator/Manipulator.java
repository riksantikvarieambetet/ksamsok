package se.raa.ksamsok.manipulator;

public interface Manipulator extends Runnable 
{
	public String getStatus();
	
	public String getName();
	
	public void stopThread();
	
	public boolean isRunning();
}
