package ms;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import redis.clients.jedis.Jedis;

/*
 * Master接收CacheServer发送的状态信息
 * 线程由ReceiveFromCache创建并启动
 */

public class ReFromCacheThread extends Thread {
	private Socket connectToCache;
	private DataInputStream fromCache;
	private DataOutputStream toCache;
	private RedisOperation redisOp;
	private Jedis jedis;
	
	private String ack0= "OK";
	private String ack3 = "yes";
	private String ack4 = "wait";
	private String ack5 = "save";
	private String cacheState = null;
	private String cacheIP = null;
	private String lastType = null;
	
	private String c0 = "newCache";
	private String c1 = "lowCache";
	private String c2 = "mediumCache";
	private String c3 = "highCache";
	private String c4 = "overCache";
	private String c5 = "notAvailCache";
	
	public ReFromCacheThread(Socket socket,RedisOperation redisOp) throws IOException {
		super();
		connectToCache = socket;
		this.redisOp = redisOp;
		this.jedis = this.redisOp.redisGetResource();
		fromCache = new DataInputStream(connectToCache.getInputStream());
		toCache = new DataOutputStream(connectToCache.getOutputStream());
		start();
	}
	
	/*
	 * 0表示新加入服务器；1表示低负载；2表示中负载；3表示高负载；4表示超高负载；5表示请求下线服务器
	 */
	
	public void run() {
		try {
			
			cacheState = fromCache.readUTF();
			cacheIP = fromCache.readUTF();
			
			if(jedis.exists(cacheIP))
			lastType = jedis.get(cacheIP);
			
			if(cacheState.equals("0")) {
				System.out.println("The cache server " + cacheIP + " is new !");	

				if( lastType == null ){
					jedis.set( cacheIP,c0); // 保存为string类型的key-value供client询问
					jedis.rpush(c0, cacheIP);// 保存在newcache的list中供出队服务
				} else if (lastType.equals(c0)){
					
				} else {
					jedis.set(cacheIP,c0); 
					jedis.rpush(c0, cacheIP);
					jedis.lrem(lastType, 1, cacheIP);
				}
				toCache.writeUTF(ack0);
				toCache.flush();
				
			} else if (cacheState.equals("1")){
				System.out.println("The cache server " + cacheIP + " load is low !");	

				if( lastType == null ){
					jedis.set( cacheIP,c1); // 保存为string类型的key-value供client询问
					jedis.rpush(c1, cacheIP);// 保存在lowCache的list中供出队服务
				} else if (lastType.equals(c1)){
					
				} else {
					jedis.set(cacheIP,c1); 
					jedis.rpush(c1, cacheIP);
					jedis.lrem(lastType, 1, cacheIP);
				}				
				toCache.writeUTF(ack0);
				toCache.flush();
				
			} else if (cacheState.equals("2")){
				System.out.println("The cache server " + cacheIP + " load is medium !");	
				
				if( lastType == null ){
					jedis.set( cacheIP,c2); // 保存为string类型的key-value供client询问
					jedis.rpush(c2, cacheIP);// 保存在mediumCache的list中供出队服务
				} else if (lastType.equals(c2)){
					
				} else {
					jedis.set(cacheIP,c2); 
					jedis.rpush(c2, cacheIP);
					jedis.lrem(lastType, 1, cacheIP);
				}				
				toCache.writeUTF(ack0);			
				toCache.flush();
				
			} else if(cacheState.equals("3")) {
				System.out.println("The cache server " + cacheIP + " is not OK !");

				if( lastType == null ){
					jedis.set( cacheIP,c3); // 保存为string类型的key-value供client询问
					jedis.rpush(c3, cacheIP);// 保存在highCache的list中将暂不提供出队服务
				} else if (lastType.equals(c3)){
					
				} else {
					jedis.set(cacheIP,c3); 
					jedis.rpush(c3, cacheIP);
					jedis.lrem(lastType, 1, cacheIP);
				}								
				toCache.writeUTF(ack3);
				toCache.flush();
				
			} else if(cacheState.equals("4")){
				System.out.println("The cache server " + cacheIP + " is so bad !");

				if( lastType == null ){
					jedis.set( cacheIP,c4); // 保存为string类型的key-value供client询问
					jedis.rpush(c4, cacheIP);// 保存在overCache的list中将不会提供出队服务
				} else if (lastType.equals(c4)){
					
				} else {
					jedis.set(cacheIP,c4); 
					jedis.rpush(c4, cacheIP);
					jedis.lrem(lastType, 1, cacheIP);
				}
				toCache.writeUTF(ack4);
				toCache.flush();
				
			} else if(cacheState.equals("5")){
				System.out.println("The cache server " + cacheIP + " is going to be off !");

				if( lastType == null ){
					jedis.set( cacheIP,c5); // 保存为string类型的key-value供client询问
					jedis.rpush(c5, cacheIP);// 保存在notAvailCache的list中将在一定时间后被移出数据库
				} else if (lastType.equals(c5)){
					
				} else {
					jedis.set(cacheIP,c5); 
					jedis.rpush(c5, cacheIP);
					jedis.lrem(lastType, 1, cacheIP);
				}

				toCache.writeUTF(ack5);
				toCache.flush();
				
			}
			fromCache.close();
			toCache.close();
			connectToCache.close();
			redisOp.redisReturnResource(jedis);
			
		} catch(IOException e){
			e.printStackTrace();
		}
	}

}
