package com.enxime.entail.experiment;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.enxime.entail.share.EntailProto.ToClient;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.MultiThreadedClaimStrategy;
import com.lmax.disruptor.NoOpEventProcessor;
import com.lmax.disruptor.RingBuffer;

public class RingBufferManager {
	private volatile long cursorBefore;
	private static final int RING_BUFFER_SIZE = 4;
	private static final MultiThreadedClaimStrategy claimStrategy = new MultiThreadedClaimStrategy(RING_BUFFER_SIZE);
	private static final BusySpinWaitStrategy waitStrategy = new BusySpinWaitStrategy();
//	private static final RingBuffer<ServerEvent> ringBuffer = new RingBuffer<ServerEvent>(
//			ServerEvent.EVENT_FACTORY, RING_BUFFER_SIZE);
	
	private static final RingBuffer<ServerEvent> ringBuffer = new RingBuffer<ServerEvent>(ServerEvent.EVENT_FACTORY, claimStrategy, waitStrategy);
	
    private final ExecutorService EP_EXECUTOR = Executors.newSingleThreadExecutor();
	
	Thread eventProcessorThread;
	private EventProcessor eventProcessor;
	
	public RingBuffer<ServerEvent> getRingBuffer() {
		return ringBuffer;
	}
	
	public RingBufferManager() {
		

//		//BatchEventProcessor<ServerEvent> eventProcessor = new BatchEventProcessor<ServerEvent>(ringBuffer, barrier, handler);
//		WorkerProcessor<ServerEvent> eventProcessor = new WorkerProcessor<ServerEvent>
		eventProcessor = new NoOpEventProcessor(ringBuffer);
		
		ringBuffer.setGatingSequences(eventProcessor.getSequence());		
		cursorBefore = ringBuffer.getCursor();
//		eventProcessorThread = new Thread(eventProcessor);
//		eventProcessorThread.start();
		EP_EXECUTOR.submit(eventProcessor);
	}
	
	public void haltEventProcessor() {
		this.eventProcessor.halt();
	}
	
	public void publishServerEvent(ToClient fromServer) {
		long sequence = ringBuffer.next();
		ServerEvent serverEvent = ringBuffer.get(sequence);
		serverEvent.setServerEvent(fromServer);
		ringBuffer.publish(sequence);
	}
	
	public List<ToClient> getServerEvents() {
		//long sequence = eventProcessor.getSequence();
		long cursorCurrent = ringBuffer.getCursor();
		//long size = cursorCurrent - cursorBefore;
		List<ToClient> toClientList = new ArrayList<ToClient>();
		if(cursorCurrent > cursorBefore ) {
			
			for(long l = (cursorBefore + 1); l <= cursorCurrent; l++) {
				toClientList.add(ringBuffer.get(l).getServerEvent());
			}
		}
		cursorBefore = cursorCurrent;
		return toClientList;
	}

}
