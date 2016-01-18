package com.zhiquayeo.skynet.endpoint.vrep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import coppelia.FloatWA;
import coppelia.remoteApi;

public class VREPEndpoint {
	// Regex Utils
	private final static Pattern SKYNET_CONTROL_MESSAGE_TYPE_REGEX =
			Pattern.compile("skynet/control/([a-z]+)/([0-9]+)$");
	private final static Matcher CONTROL_MESSAGE_TYPE_MATCHER = SKYNET_CONTROL_MESSAGE_TYPE_REGEX.matcher("");
	
	public static void main(String[] args) throws InterruptedException {
		if (args.length < 1) {
			System.err.println("[VREPEndpoint] Please provide a path to a robot config file");
			return;
		}
		
		int remoteApiPort = 0;
		
		System.out.println("=== S T A R T I N G ===");
		
		JSONParser parser = new JSONParser();
		JSONObject baseConfigJson = null;
		try {
			FileReader fr = new FileReader(args[0]);
			BufferedReader br = new BufferedReader(fr);
			
			baseConfigJson = (JSONObject)parser.parse(br);
			
			// Check for a port
			if (baseConfigJson.containsKey("apiPort")) {
				remoteApiPort = ((Number)baseConfigJson.get("apiPort")).intValue();
				System.out.println("[VREPEndpoint] Using V-REP Remote API Port " + remoteApiPort);
			}
		}
		catch (ParseException e) {
			System.err.println("[VREPEndpoint] Cannot parse config file");
			System.err.println(e);
			return;
		}
		catch (InvalidPathException e) {
			System.err.println("[VREPEndpoint] Invalid path specified");
			e.printStackTrace();
			return;
		}
		catch (IOException e) {
			System.err.println("[VREPEndpoint] Could not read config file");
			e.printStackTrace();
			return;
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		remoteApi vrep = new remoteApi();
		int clientId;
		
		// Simulation Vars
		float[] pwmOutVals = new float[12];
		boolean[] pwmOutValChanged = new boolean[12];
		float[] analogInVals = new float[12];
		boolean[] analogInValChanged = new boolean[12];
		boolean[] analogFirstRead = new boolean[12];
		
		// Initialize
		for (int i = 0; i < 12; i++) {
			pwmOutVals[i] = 0.0f;
			pwmOutValChanged[i] = true;
			
			analogInVals[i] = 0.0f;
			analogInValChanged[i] = false;
			analogFirstRead[i] = true;
		}
		
		// Connect to the simulator
		System.out.println("[VREPEndpoint] Connecting to V-REP");
		vrep.simxFinish(-1);
		clientId = vrep.simxStart("127.0.0.1", remoteApiPort, true, true, 5000, 5);
		
		if (clientId == -1) {
			System.err.println("[VREPEndpoint] Cannot connect to V-REP");
			return;
		}
		
		// Create our robot
		VREPRobot robot = new VREPRobot(vrep, clientId, baseConfigJson);
		System.out.println("[VREPEndpoint] Robot created: ");
		System.out.println(robot + "\n");
		
		// Start up the MQTT Client
		MqttClient mqttClient;
		
		try {
			mqttClient = new MqttClient("tcp://localhost:1883", "skynet_vrep_ep");
			mqttClient.connect();
			mqttClient.setCallback(new MqttCallback() {

				@Override
				public void connectionLost(Throwable cause) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					if (topic.equals("skynet/control/disable")) {
						System.out.println("[VREPEndpoint] DISABLE received. Shutting down outputs");
						// Shutdown all PWM outputs
						Iterator<Integer> pwmChItr = robot.getPwmOutChannels().iterator();
						while (pwmChItr.hasNext()) {
							int pwmCh = pwmChItr.next();
							if (pwmOutValChanged[pwmCh]) {
								pwmOutVals[pwmCh] = 0;
								pwmOutValChanged[pwmCh] = true;
							}
						}
					}
					else {
						CONTROL_MESSAGE_TYPE_MATCHER.reset(topic);
						if (CONTROL_MESSAGE_TYPE_MATCHER.matches()) {
							String controlType = CONTROL_MESSAGE_TYPE_MATCHER.group(1);
							int channel = Integer.parseInt(CONTROL_MESSAGE_TYPE_MATCHER.group(2));
							
							if (controlType.equals("pwm")) {
								// Do a conversion first
								// incoming is 0 - 255
								// Convert to [-1, 1]
								float incomingVal = Float.parseFloat(new String(message.getPayload()));
								float pct = (incomingVal - 127.0f) / 127.0f;
								float outVal = pct * robot.getPwmMultiplier(channel);
								if (pwmOutVals[channel] != outVal) {
									pwmOutVals[channel] = outVal;
									pwmOutValChanged[channel] = true;
								}
								
							}
						}
					}
				}

				
			});
			
			mqttClient.subscribe("skynet/control/#");
			mqttClient.subscribe("skynet/clients/#");
			
			System.out.println("[MQTT] Connected to broker");
			
			
			while (vrep.simxGetConnectionId(clientId) != -1) {
				// Main Loop
				// Get sensor values from VREP and push output vals to VREP
				// Input first
				// TODO Implement input
				// Get Analog Inputs
				
				// Outputs
				// PWM
				Iterator<Integer> pwmChItr = robot.getPwmOutChannels().iterator();
				while (pwmChItr.hasNext()) {
					int pwmCh = pwmChItr.next();
					if (pwmOutValChanged[pwmCh]) {
						vrep.simxSetJointTargetVelocity(clientId, robot.getPwmOutHandle(pwmCh), pwmOutVals[pwmCh], remoteApi.simx_opmode_oneshot);
						pwmOutValChanged[pwmCh] = false;
					}
				}
				
				Thread.sleep(5);
			}
			
			
			System.out.println("[VREPEndpoint] Connection to V-REP lost. Shutting down");
			vrep.simxFinish(clientId);
			mqttClient.disconnect();
			System.exit(0);
		}
		catch (MqttException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
