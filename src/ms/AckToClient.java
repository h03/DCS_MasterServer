package ms;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * Master等待UserClient的确认当前cache节点状态的请求
 * 为新的请求建立连接,端口号为5800
 * 并返回结果给对应的UserClient
 */

public class AckToClient extends Thread{
	private RedisOperation redisOp;

	public static void main(String[] args) {

	}
	
	public AckToClient(RedisOperation redisOp){
		this.redisOp = redisOp;
	}
	
	
	public void run(){
		try {
			msAckToClient(redisOp);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void msAckToClient(RedisOperation redisOp) throws InterruptedException {
		try {
			System.out.println("Waiting for user client acknowledgement...");
			ServerSocket serverSocket = new ServerSocket(5800);
			Socket connectToClient = null;
			while (true) {
				connectToClient = serverSocket.accept();
				new AckToClientThread(connectToClient,redisOp.redisGetResource());
			}
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	

}
