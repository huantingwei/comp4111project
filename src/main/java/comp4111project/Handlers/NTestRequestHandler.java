package comp4111project.Handlers;

import java.io.File;
import java.io.IOException;

import org.apache.http.Consts;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NTestRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

	// Triggered when an incoming request is received. 
	// This method should return a HttpAsyncRequestConsumer that will be used to process the request and consume message content if enclosed. 
	// The consumer can optionally parse or transform the message content into a structured object which is then passed onto the handle(Object,
	// HttpAsyncExchange, HttpContext) method for further processing.
	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest data, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                }
                catch(InterruptedException ie) {}
                HttpResponse response = httpExchange.getResponse();
                response.setStatusCode(HttpStatus.SC_OK);
                NFileEntity body = new NFileEntity(new File("static.html"),
                        ContentType.create("text/html", Consts.UTF_8));
                response.setEntity(body);
                httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
            }
        }.start();
		

		/**
		 *
		 * @author Ethan
		 */

        // 實例化一個 CyclicBarrier 物件，屏障數量為 5 
        // 必須要等待最後一個執行緒到達屏障時，
        // 所有被屏障攔截的執行緒才會回到"可執行池"中
        CyclicBarrier cb = new CyclicBarrier(5);
        
        // 建立緩存的執行緒池
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // 在執行緒池中加入 Runnable 或 Callable, 並執行
        executor.execute(new Runner(cb, "A"));
        executor.execute(new Runner(cb, "B"));
        executor.execute(new Runner(cb, "C"));
        executor.execute(new Runner(cb, "D"));
        executor.execute(new Runner(cb, "E"));
        
        // 沒有使用時，要把執行緒池關閉
        executor.shutdown();
    }
}

/**
 *
 * @author Ethan
 */
class Runner implements Runnable{
    private CyclicBarrier cb;
    private String name;

    public Runner(CyclicBarrier cb, String name) {
        this.cb = cb;
        this.name = name;
    }
    
    @Override
    public void run() {
        try {
            // 隨機睡眠 0 ~ 5 秒
            //Thread.sleep(1000 * new Random().nextInt(5));
            System.out.println(this.name + " is ready now...");
            // 已到達屏障
            cb.await();
            // 當最後一個執行緒到達屏障，才能回到"可執行池"中
        } catch (InterruptedException e1) {
           e1.printStackTrace();
        } catch (BrokenBarrierException e2) {
           e2.printStackTrace();
        }
        System.out.println(this.name + " go!!");
    }
    
}

