package com.zhiquayeo.skynet.endpoint.vrep;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import coppelia.IntW;
import coppelia.remoteApi;

public class VREPRobot {
	private remoteApi d_vrep;
	private int d_clientId;
	private String d_robotName = "[NONE PROVIDED]";
	private String d_robotCreator = "[NONE PROVIDED]";
	
	// Handles
	private Set<Integer> d_digitalInChannels = new HashSet<>();
	private int[] d_digitalInHandles = new int[12];
	
	private Set<Integer> d_digitalOutChannels = new HashSet<>();
	private int[] d_digitalOutHandles = new int[12];
	
	private Set<Integer> d_analogInChannels = new HashSet<>();
	private int[] d_analogInHandles = new int[12];
	private float[] d_analogInMultipliers = new float[12];
	
	private Set<Integer> d_pwmOutChannels = new HashSet<>();
	private int[] d_pwmOutHandles = new int[12];
	private float[] d_pwmMultipliers = new float[12];
	
	
	public VREPRobot(remoteApi vrep, int clientId, JSONObject robotInfo) {
		d_vrep = vrep;
		d_clientId = clientId;
		
		int rc;
		IntW tempHandle = new IntW(-1);
		
		// Set up defaults
		for (int i = 0; i < 12; i++) {
			d_pwmMultipliers[i] = 1.0f;
			d_digitalInHandles[i] = -1;
			d_digitalOutHandles[i] = -1;
			d_analogInHandles[i] = -1;
			d_pwmOutHandles[i] = -1;
			d_analogInMultipliers[i] = 1.0f;
		}
		
		if (robotInfo.containsKey("name")) {
			d_robotName = (String)robotInfo.get("name");
		}
		if (robotInfo.containsKey("creator")) {
			d_robotCreator = (String)robotInfo.get("creator");
		}
		
		if (robotInfo.containsKey("digitalIn")) {
			JSONArray items = (JSONArray)robotInfo.get("digitalIn");
			Iterator<JSONObject> itr = items.iterator();
			while (itr.hasNext()) {
				JSONObject item = itr.next();
				if (item.containsKey("channel") &&
					item.containsKey("name")) {
					
					int channel = ((Number)item.get("channel")).intValue();
					String objName = (String)item.get("name");
					rc = d_vrep.simxGetObjectHandle(d_clientId, objName, tempHandle, remoteApi.simx_opmode_oneshot_wait);
					if (rc == remoteApi.simx_return_ok) {
						d_digitalInChannels.add(channel);
						d_digitalInHandles[channel] = tempHandle.getValue();
					}
					else {
						System.err.println("[VREPRobot] Could not get handle for " + objName);
					}
				}
				else {
					System.err.println("[VREPEndpoint] Missing Fields");
				}
			}
		}
		
		if (robotInfo.containsKey("digitalOut")) {
			JSONArray items = (JSONArray)robotInfo.get("digitalOut");
			Iterator<JSONObject> itr = items.iterator();
			while (itr.hasNext()) {
				JSONObject item = itr.next();
				if (item.containsKey("channel") &&
					item.containsKey("name")) {
					
					int channel = ((Number)item.get("channel")).intValue();
					String objName = (String)item.get("name");
					rc = d_vrep.simxGetObjectHandle(d_clientId, objName, tempHandle, remoteApi.simx_opmode_oneshot_wait);
					if (rc == remoteApi.simx_return_ok) {
						d_digitalOutChannels.add(channel);
						d_digitalOutHandles[channel] = tempHandle.getValue();
					}
					else {
						System.err.println("[VREPRobot] Could not get handle for " + objName);
					}
				}
				else {
					System.err.println("[VREPEndpoint] Missing Fields");
				}
			}
		}
		
		if (robotInfo.containsKey("analogIn")) {
			JSONArray items = (JSONArray)robotInfo.get("analogIn");
			Iterator<JSONObject> itr = items.iterator();
			while (itr.hasNext()) {
				JSONObject item = itr.next();
				if (item.containsKey("channel") &&
					item.containsKey("name")) {
					
					int channel = ((Number)item.get("channel")).intValue();
					String objName = (String)item.get("name");
					rc = d_vrep.simxGetObjectHandle(d_clientId, objName, tempHandle, remoteApi.simx_opmode_oneshot_wait);
					if (rc == remoteApi.simx_return_ok) {
						d_analogInChannels.add(channel);
						d_analogInHandles[channel] = tempHandle.getValue();
						if (item.containsKey("multiplier")) {
							d_analogInMultipliers[channel] = ((Number)item.get("multiplier")).floatValue();
						}
					}
					else {
						System.err.println("[VREPRobot] Could not get handle for " + objName);
					}
				}
				else {
					System.err.println("[VREPEndpoint] Missing Fields");
				}
			}
		}
		
		if (robotInfo.containsKey("pwmOut")) {
			JSONArray items = (JSONArray)robotInfo.get("pwmOut");
			Iterator<JSONObject> itr = items.iterator();
			while (itr.hasNext()) {
				JSONObject item = itr.next();
				if (item.containsKey("channel") &&
					item.containsKey("name")) {
					
					int channel = ((Number)item.get("channel")).intValue();
					String objName = (String)item.get("name");
					rc = d_vrep.simxGetObjectHandle(d_clientId, objName, tempHandle, remoteApi.simx_opmode_oneshot_wait);
					if (rc == remoteApi.simx_return_ok) {
						d_pwmOutChannels.add(channel);
						d_pwmOutHandles[channel] = tempHandle.getValue();
						if (item.containsKey("multiplier")) {
							d_pwmMultipliers[channel] = ((Number)item.get("multiplier")).floatValue();
						}
					}
					else {
						System.err.println("[VREPRobot] Could not get handle for " + objName);
					}
				}
				else {
					System.err.println("[VREPEndpoint] Missing Fields");
				}
			}
		}
	}
	
	public int getDigitalOutHandle(int channel) {
		return d_digitalOutHandles[channel];
	}
	
	public int getDigitalInHandle(int channel) {
		return d_digitalInHandles[channel];
	}
	
	public int getAnalogInHandle(int channel) {
		return d_analogInHandles[channel];
	}
	
	public int getPwmOutHandle(int channel) {
		return d_pwmOutHandles[channel];
	}
	
	public float getPwmMultiplier(int channel) {
		return d_pwmMultipliers[channel];
	}
	
	public float getAnalogInMultiplier(int channel) {
		return d_analogInMultipliers[channel];
	}
	
	public Set<Integer> getDigitalInChannels() {
		return d_digitalInChannels;
	}
	
	public Set<Integer> getDigitalOutChannels() {
		return d_digitalOutChannels;
	}
	
	public Set<Integer> getAnalogInChannels() {
		return d_analogInChannels;
	}
	
	public Set<Integer> getPwmOutChannels() {
		return d_pwmOutChannels;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("--- VREP Robot ---\n");
		sb.append("Name: " + d_robotName + "\n");
		sb.append("Creator: " + d_robotCreator + "\n\n");
		sb.append("Digital Inputs: \n");
		if (d_digitalInChannels.size() > 0) {
			Iterator<Integer> itr = d_digitalInChannels.iterator();
			while (itr.hasNext()) {
				int ch = itr.next();
				sb.append("\t" + ch + " -> " + d_digitalInHandles[ch] + "\n");
			}
		}
		else {
			sb.append("\tNONE");
		}
		sb.append("\n");
		
		sb.append("Digital Outputs: \n");
		if (d_digitalOutChannels.size() > 0) {
			Iterator<Integer> itr = d_digitalOutChannels.iterator();
			while (itr.hasNext()) {
				int ch = itr.next();
				sb.append("\t" + ch + " -> " + d_digitalOutHandles[ch] + "\n");
			}
		}
		else {
			sb.append("\tNONE");
		}
		sb.append("\n");
		
		sb.append("Analog Inputs: \n");
		if (d_analogInChannels.size() > 0) {
			Iterator<Integer> itr = d_analogInChannels.iterator();
			while (itr.hasNext()) {
				int ch = itr.next();
				sb.append("\t" + ch + " -> " + d_analogInHandles[ch] + "\n");
			}
		}
		else {
			sb.append("\tNONE");
		}
		sb.append("\n");
		
		sb.append("PWM Outputs: \n");
		if (d_pwmOutChannels.size() > 0) {
			Iterator<Integer> itr = d_pwmOutChannels.iterator();
			while (itr.hasNext()) {
				int ch = itr.next();
				sb.append("\t" + ch + " -> " + d_pwmOutHandles[ch] + "\n");
			}
		}
		else {
			sb.append("\tNONE");
		}
		sb.append("\n");
		return sb.toString();
	}
}
