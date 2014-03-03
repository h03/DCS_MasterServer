package ms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;


import redis.clients.jedis.Jedis;



/*
 * Master等待UserClient的询问请求
 * 为新的请求建立连接,端口号为5600
 * 并返回计算结果给对应的UserClient
 */
	
public class RespondToClient extends Thread{
	
	private RedisOperation redisOp;

	
	public RespondToClient(RedisOperation redisOp){
		this.redisOp = redisOp;
	}
	
	
	public void run(){
		try {
			msRespondToClient(redisOp);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void msRespondToClient(RedisOperation redisOp) throws InterruptedException {
		try {
			System.out.println("Waiting for user client asking the targetIP...");
			ServerSocket serverSocket = new ServerSocket(5600);
			Socket connectToClient = null;
			while (true) {
				connectToClient = serverSocket.accept();
				new ReToClientThread(connectToClient,redisOp);
			}
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	

}
