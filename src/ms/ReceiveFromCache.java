package ms;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/*
 * Master等待CacheServer的连接请求，并创建接收线程。
 * 端口号：5700
 * 与CacheServer中的FeedbackToMaster对应。
 */

public class ReceiveFromCache extends Thread{
	
	private RedisOperation redisOp;
	
	public static void main(String[] args) throws InterruptedException{

	}
	
	public ReceiveFromCache(RedisOperation redisOp){
		this.redisOp = redisOp;
	}
	
	public void run(){
		 try {
			msReceiveFromCache(redisOp);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void msReceiveFromCache(RedisOperation redisOp) throws InterruptedException {
		try {
			System.out.println("Waiting for cache server connection...");
			ServerSocket serverSocket = new ServerSocket(5700);
			Socket connectToCache = null;
			
			while(true) {
				connectToCache = serverSocket.accept();				
				new ReFromCacheThread(connectToCache,redisOp);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}

}
