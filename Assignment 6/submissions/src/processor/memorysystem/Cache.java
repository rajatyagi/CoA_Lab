package processor.memorysystem;

import configuration.Configuration;
import generic.Element;
import generic.Event;
import generic.Event.EventType;
import generic.MemoryReadEvent;
import generic.MemoryResponseEvent;
import generic.MemoryWriteEvent;
import generic.Simulator;
import processor.Clock;
import processor.Processor;
import processor.pipeline.MemoryAccess;
import processor.memorysystem.*;
import processor.pipeline.*;

public class Cache implements Element {
	Processor containingProcessor;
	public int latency ;
	int size;
	int bitOffset;
	int setOffset;
	int numberOfLines;
	int lineSize = 4;
	int associativity;
	int lruPos;
	int miss_addr;
	int miss_value;
	CacheLine[] cach;
	Element miss_requesting;
	boolean read_miss = false, write_miss = false;

	public Cache(Processor containingProcessor,int latency,int numberOfLines, int associativity) {

		this.containingProcessor = containingProcessor;
		this.latency=latency;
		this.size = lineSize*numberOfLines;
		this.numberOfLines = numberOfLines;
		this.associativity = associativity;

		bitOffset = (int)(Math.log(lineSize)/Math.log(2));
		setOffset =  (int)(Math.log(numberOfLines/associativity)/Math.log(2));

		cach = new CacheLine[numberOfLines];
		
		for(int i=0;i<numberOfLines;i++) {
			cach[i]=new CacheLine();
		}

		System.out.println("Hello");

	}
	
	public void handleCacheMiss(int addr ,Element miss_req) {

		System.out.println("Handling miss");
		Simulator.getEventQueue().addEvent(
				new MemoryReadEvent(
						Clock.getCurrentTime()+Configuration.mainMemoryLatency,
						this,containingProcessor.getMainMemory(),
						addr));


		miss_requesting = miss_req;
		miss_addr = addr;
	}

	public void handleRead(int addr,Element req) {

		System.out.println("Reading");

		String a = String.format("%32s", Integer.toBinaryString(addr)).replaceAll(" ","0");
		int setAddr,tagReq,byteAddr;

		byteAddr = Integer.parseInt(a.substring(32 - bitOffset,32),2);
		setAddr = Integer.parseInt(a.substring(32 - bitOffset - setOffset , 32 - bitOffset),2);
		tagReq = Integer.parseInt(a.substring(0,32 - bitOffset - setOffset),2);

		int tag1 = cach[setAddr*associativity].tag;
		int tag2 = cach[setAddr*associativity + 1].tag;

		if(tag1 == tagReq) {
			System.out.println(bitOffset);
			Simulator. getEventQueue().addEvent(
					new MemoryResponseEvent(
					Clock.getCurrentTime () ,
					this ,req ,cach[setAddr*associativity].data[byteAddr] , cach[setAddr*associativity].data));				
		}

		else if(tag2 == tagReq) {
			System.out.println("Hit");
			Simulator. getEventQueue().addEvent(
					new MemoryResponseEvent (
					Clock.getCurrentTime () ,
					this ,req ,cach[setAddr*associativity + 1].data[byteAddr],cach[setAddr*associativity + 1].data));				
			
			CacheLine temp = new CacheLine();
			temp = cach[setAddr*associativity];

			cach[setAddr*associativity] = cach[setAddr*associativity + 1];
			cach[setAddr*associativity + 1] = temp;
		}

		else{
			System.out.println("Miss " + addr);
			read_miss = true;
			lruPos = setAddr*associativity + 1;
			handleCacheMiss(addr,req);
		}
	}

	public void handleWrite(int addr, int value,Element req) {

		System.out.println("Writing");
		
		String a = String.format("%32s", Integer.toBinaryString(miss_addr)).replaceAll(" ","0");
		int setAddr,tagReq,byteAddr;

		byteAddr = Integer.parseInt(a.substring(32 - bitOffset,32),2);
		setAddr = Integer.parseInt(a.substring(32 - bitOffset - setOffset , 32 - bitOffset),2);
		tagReq = Integer.parseInt(a.substring(0,32 - bitOffset - setOffset),2);

		int tag1 = cach[setAddr*associativity].tag;
		int tag2 = cach[setAddr*associativity + 1].tag;

		if(tag1 == tagReq) {
			System.out.println("Hit");
			cach[setAddr*associativity].setDataElement(byteAddr,value);
			Simulator. getEventQueue().addEvent(
					new MemoryWriteEvent (
					Clock.getCurrentTime () ,
					this , containingProcessor.getMainMemory(),addr,value));

			((MemoryAccess) req).EX_MA_Latch.setMA_enable(false);
			((MemoryAccess) req).EX_MA_Latch.setMA_busy(false);
			((MemoryAccess) req).MA_RW_Latch.setRW_enable(true);
		}
		else if(tag2 == tagReq) {				
			System.out.println("Hit");
			CacheLine temp = new CacheLine();
			temp = cach[setAddr*associativity];

			cach[setAddr*associativity] = cach[setAddr*associativity + 1];
			cach[setAddr*associativity + 1] = temp;
		
			cach[setAddr*associativity].setDataElement(byteAddr,value);
		
			Simulator. getEventQueue().addEvent(
					new MemoryWriteEvent (
					Clock.getCurrentTime (),
					this , containingProcessor.getMainMemory(),addr,value));
			((MemoryAccess) req).EX_MA_Latch.setMA_busy(false);
			((MemoryAccess) req).MA_RW_Latch.setRW_enable(true);
		}

		else{
			System.out.println("Miss");
			miss_value = value;
			write_miss = true;
			lruPos = setAddr*associativity + 1;
			handleCacheMiss(addr,req);
		}

	}

	public void handleResponse(int[] data) {

		System.out.println("Handling Response");

		CacheLine newLine = new CacheLine();
		int tag = Integer.parseInt(String.format("%32s", Integer.toBinaryString(miss_addr)).replaceAll(" ","0").substring(0,32 - bitOffset - setOffset));

		System.out.println("tag");

		newLine.settag(tag);
		newLine.setData(data);

		for(int i = 0 ; i < data.length ;i++){
			System.out.println(data[i]);
		}

		cach[lruPos] = newLine;

		if(read_miss == true){
			read_miss = false;
			handleRead(miss_addr,miss_requesting);
		}
		else if(write_miss == true){
			write_miss = false;
			handleWrite(miss_addr,miss_value,miss_requesting);	
		}
		
	}

	@Override
	public void handleEvent(Event e) {
		
		if ( e.getEventType ( ) == EventType.MemoryRead )
		{
			MemoryReadEvent event = ( MemoryReadEvent ) e ;
			handleRead(event.getAddressToReadFrom(), event.getRequestingElement());
		}
		else if(e.getEventType ( ) == EventType.MemoryResponse ) {
			MemoryResponseEvent event = ( MemoryResponseEvent ) e ;
			handleResponse(event.getLine());
		}
		else if(e.getEventType ( ) == EventType.MemoryWrite) {
			MemoryWriteEvent event = ( MemoryWriteEvent ) e ;
			handleWrite(event.getAddressToWriteTo(),event.getValue(),event.getRequestingElement());
			
		}
			
	}
	
}
