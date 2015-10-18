package de.saxsys.pitfalls.services;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import javafx.concurrent.WorkerStateEvent;

import org.jacpfx.concurrency.FXWorker;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.saxsys.javafx.test.JfxRunner;
import de.saxsys.javafx.test.TestInJfxThread;

/**
 * A test for {@link ClassToTest} using the solution pattern given by
 * http://blog.buildpath.de/how-to-test-javafx-services/.
 */
@RunWith(JfxRunner.class)
public class ClassToTestTest {
	
	@Test
	@TestInJfxThread
	public void testLongLastingOperation() throws ExecutionException, InterruptedException, TimeoutException {
		ClassToTest service = new ClassToTest();
		
		CompletableFuture<String> future = new CompletableFuture<>();
		
		service.valueProperty().addListener((b, o, n) -> {
			if (n != null) {
				future.complete(n);
			}
		});
		
		// STARTET ONCE....
		service.start();
		
		// EXC
		System.out.println(service.valueProperty());
		// assertEquals("I'm an expensive result", cut.getValue());
		assertEquals("I'm an expensive result.", future.get(5, TimeUnit.SECONDS));
	}
	
	@Test(timeout = 60000)
	public void testLongLastingOperationWithFXWorker() throws ExecutionException, InterruptedException {
		final ClassToTest service = new ClassToTest();
		
		IntStream.range(0, 5).forEach(v -> {
			CountDownLatch stop = new CountDownLatch(1);
			FXWorker<?> handler = FXWorker.instance();
			CountDownLatch waitForAsynyResult = new CountDownLatch(1);
			handler.supplyOnFXThread(() -> {
				
				if (!service.isRunning()) {
					service.reset();
					service.start();
				}
				
				service.valueProperty().addListener((b, o, n) -> {
					if (n != null) {
						assertEquals("I'm an expensive result.", n);
						waitForAsynyResult.countDown();
					}
				});
				return service;
			}).functionOnExecutorThread((cut1) -> {
				// wait outside FX application thread !!!
					try {
						waitForAsynyResult.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					return cut1;
				}).functionOnFXThread(cut2 -> {
				System.out.println(cut2.valueProperty().getValue());
				return cut2.getValue();
			}).execute(val -> {
				assertEquals("I'm an expensive result.", val);
				stop.countDown();
			});
			
			try {
				stop.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		
		
		assertEquals(true, true);
		
	}
	
	
	@Test(timeout = 60000)
	public void testLongLastingOperationWithFXWorker2() throws ExecutionException, InterruptedException {
		CountDownLatch stop = new CountDownLatch(1);
		final FXWorker<?> handler = FXWorker.instance();
		CountDownLatch waitForAsynyResult = new CountDownLatch(1);
		handler.supplyOnFXThread(() -> {
			ClassToTest service = new ClassToTest();
			service.start();
			service.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, (val) -> {
				assertEquals("I'm an expensive result.", service.getValue());
				waitForAsynyResult.countDown();
			});
			return service;
		}).functionOnExecutorThread((cut) -> {
			// wait outside FX application thread !!!
				try {
					waitForAsynyResult.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				return cut;
			}).functionOnFXThread(cut -> {
			System.out.println(cut.valueProperty().getValue());
			return cut.getValue();
		}).execute(val -> {
			assertEquals("I'm an expensive result.", val);
			stop.countDown();
		});
		assertEquals(true, true);
		stop.await();
	}
	
	@Test(timeout = 60000)
	public void testLongLastingOperationWithFXWorker3() throws ExecutionException, InterruptedException {
		CountDownLatch stop = new CountDownLatch(1);
		final FXWorker<?> handler = FXWorker.instance();
		
		handler.supplyOnExecutorThread(() -> {
			SomeClassToTest service = new SomeClassToTest();
			String result = service.longRunning();
			System.out.println("result step1: " + result);
			handler.updateMessage(result);
			return result;
		}).functionOnFXThread((cut) -> {
			System.out.println("result step2: " + cut);
			assertEquals("I'm an expensive result.", cut);
			assertEquals("I'm an expensive result.", handler.messageProperty().getValue());
			System.out.println("result step3: " + handler.messageProperty().getValue());
			return cut;
		}).execute(val -> {
			assertEquals("I'm an expensive result.", val);
			System.out.println("result step4: " + val);
			stop.countDown();
		});
		stop.await();
		assertEquals(true, true);
	}
	
	@Test
	@TestInJfxThread
	/**
	 * Das kann nicht funktionieren aus dem selben Grund warum CountDown latch nicht funktioniert.
	 * Du blockierst den FX application Thread (bzw. per Zufall kann es klappen (wenn der Service kürzer Arbeitet als die Excecution der Testmethode dauert))
	 */
	public void testLongLastingOperationInFXThread() throws ExecutionException, InterruptedException, TimeoutException {
		
		ClassToTest service = new ClassToTest();
		
		CompletableFuture<String> future = new CompletableFuture<>();
		
		service.valueProperty().addListener((b, o, n) -> {
			if (n != null) {
				future.complete(n);
			}
		});
		
		// STARTET ONCE....
		service.start();
		
		// EXC
		// cut.valueProperty();
		// assertEquals("I'm an expensive result", cut.getValue());
		assertEquals("I'm an expensive result.", future.get(5, TimeUnit.SECONDS));
		
	}
	
	
	
	/**
	 * Das kann nicht funktionieren aus dem selben Grund warum CountDown latch nicht funktioniert. Du blockierst den FX
	 * application Thread (bzw. per Zufall kann es klappen (wenn der Service kürzer Arbeitet als die Excecution der
	 * Testmethode dauert))
	 * 
	 * @throws TimeoutException
	 */
	@TestInJfxThread
	@Test
	public void testLongLastingOperationInFXThreadDirty() throws ExecutionException, InterruptedException,
			TimeoutException {
		
		ClassToTest service = new ClassToTest();
		
		CompletableFuture<String> future = new CompletableFuture<>();
		
		service.valueProperty().addListener((b, o, n) -> {
			if (n != null) {
				future.complete(n);
			}
		});
		
		// STARTET ONCE....
		service.start();
		// bei mir kommt hier java.lang.IllegalStateException: Service must only be used from the FX Application Thread
		// das sollte doch aber mit deiner Annotation nicht sein?!?
		while (service.isRunning()) {
			System.out.println("wait");
		}
		
		// EXC
		// cut.valueProperty();
		// assertEquals("I'm an expensive result", cut.getValue());
		assertEquals("I'm an expensive result.", future.get(5, TimeUnit.SECONDS));
		
	}
}
