package ms;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import redis.clients.jedis.Jedis;

/*
 * Master接收UserClient发来的目标存储节点询问请求
 * 线程由RespondToClient创建并启动
 */

public class ReToClientThread extends Thread {
	private Socket connectToClient;
	private DataInputStream fromUClient;
	private DataOutputStream toUClient;
	private RedisOperation redisOp;
	private Jedis jedis;
	
	private String c0 = "newCache";
	private String c1 = "lowCache";
	private String c2 = "mediumCache";
	private String c3 = "highCache";
	private String c4 = "overCache";
	private String c5 = "notAvailCache";
	
	private String ackToClient1 = "OK";
	private String ackToClient2 = "NO";
	
	private String askType = "askIP";
	private String ackType = "ack";
	
	// 构造方法：为每个套接字连接输入和输出流
	public ReToClientThread(Socket socket,RedisOperation redisOp) throws IOException {
		super();
		connectToClient = socket;
		this.redisOp = redisOp;
		this.jedis = this.redisOp.redisGetResource();
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
	
	
	
	// 在run()方法中与客户端通信
	public void run() {
		try {
			String requestType;
			String targetIP;
			requestType = fromUClient.readUTF();
			if(requestType.equals(askType)){
				targetIP = msFindTargetIP(jedis);
				if(targetIP != null){
				    toUClient.writeUTF(targetIP);
				    toUClient.flush();
				    System.out.println("已向客户端: " + connectToClient.getInetAddress().getHostAddress() + " 返回目标存储节点！");
				} else {
					toUClient.writeUTF("error");
					toUClient.flush();
					System.out.println("客户端: " + connectToClient.getInetAddress().getHostAddress() + " 没有可以使用的目标存储节点！请扩展集群！");
				}
			}
			
			fromUClient.close();
			toUClient.close();
			connectToClient.close();
			redisOp.redisReturnResource(jedis);
			
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	

}
