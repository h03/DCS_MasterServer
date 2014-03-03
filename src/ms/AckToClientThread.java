package ms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import redis.clients.jedis.Jedis;


/*
 * Master接收UserClient发来的确认目标存储节点状态请求，端口号为：5800
 * 线程由AckToClient创建并启动
 */

public class AckToClientThread extends Thread {
	private Socket connectToClient;
	private DataInputStream fromUClient;
	private DataOutputStream toUClient;
	private Jedis jedis;
	
	private String c0 = "newCache";
	private String c1 = "lowCache";
	private String c2 = "mediumCache";
	private String c3 = "highCache";
	private String c4 = "overCache";
	private String c5 = "notAvailCache";
	
	private String ackToClient1 = "OK";
	private String ackToClient2 = "NO";
	
	private String ackType = "ack";
	
	// 构造方法：为每个套接字连接输入和输出流
	public AckToClientThread(Socket socket,Jedis jedis) throws IOException {
		super();
		connectToClient = socket;
		this.jedis = jedis;
		fromUClient = new DataInputStream(connectToClient.getInputStream());
		toUClient = new DataOutputStream(connectToClient.getOutputStream());
		start();    // 启动run()方法
	}
	
	
	// 从cache分组队列中由低负载到高负载依次轮流出队负责存储工作
	public String msFindTargetIP(Jedis jedis) {
		String targetIP = null;
		if(jedis.llen(c0)>0){
			targetIP = jedis.rpoplpush(c0, c0);
		} else if(jedis.llen(c1)>0){
			targetIP = jedis.rpoplpush(c1, c1);
		} else if(jedis.llen(c2)>0){
			targetIP = jedis.rpoplpush(c2, c2);
		} else if(jedis.llen(c3)>0){
			targetIP = jedis.rpoplpush(c3, c3);
		} else if(jedis.llen(c4)>0){
			targetIP = jedis.rpoplpush(c4, c4);
		}
		return targetIP;
	}
	
	// 确认某个cacheIP是否存在且负载处于较低水平，若不存在或处于高负荷以上则返回“NO”，存在且符合在中下水平，则返回“OK”。
	public String msAckTargetIP(String targetIP,Jedis jedis){
		String ack = null;
		if(!jedis.exists(targetIP)){
			ack = ackToClient2;
		}
		else if(jedis.get(targetIP).equals(c0) || jedis.get(targetIP).equals(c1) || jedis.get(targetIP).equals(c2)){
			ack = ackToClient1;
		} 
		else if(jedis.get(targetIP).equals(c3) || jedis.get(targetIP).equals(c4) || jedis.get(targetIP).equals(c5)){
			ack = ackToClient2;
		} 
		else {
			ack = ackToClient2;
		}
		return ack;
		
	}
	
	
	// 在run()方法中与客户端通信
	public void run() {
		try {
			String requestType;
			String targetIP;
			requestType = fromUClient.readUTF();
            if(requestType.equals(ackType)){
				targetIP = fromUClient.readUTF();
				System.out.println("接收到client的确认cache状态请求...");
				String ack = msAckTargetIP(targetIP,jedis);
				System.out.println("正在返回确认信息..." + ack);
				if(ack.equals(ackToClient1)){
					toUClient.writeUTF(ack);
					toUClient.flush();
				} else if(ack.equals(ackToClient2)){
					targetIP = msFindTargetIP(jedis);
					if(targetIP != null){
					toUClient.writeUTF(targetIP);
					toUClient.flush();
					}
					else {
						toUClient.writeUTF(ack);
						toUClient.flush();
					}
				} else {
					toUClient.writeUTF(ackToClient2);
					toUClient.flush();
				}

			}	
            requestType = fromUClient.readUTF();
            if(requestType.equals("OK")){
    			fromUClient.close();
    			toUClient.close();
    			connectToClient.close();
            }else if(connectToClient.isClosed()){
    			fromUClient.close();
    			toUClient.close();
    			connectToClient.close();
            }else{
            	
            }

			
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	

}
