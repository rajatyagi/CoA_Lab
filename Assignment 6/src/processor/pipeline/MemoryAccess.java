package processor.pipeline;

import configuration.Configuration;
import generic.Element;
import generic.Event;
import generic.MemoryReadEvent;
import generic.MemoryResponseEvent;
import generic.MemoryWriteEvent;
import generic.Simulator;
import processor.Clock;
import processor.Processor;

public class MemoryAccess implements Element{
	Processor containingProcessor;
	ControlUnit controlunit = new ControlUnit();
	public EX_MA_LatchType EX_MA_Latch;
	public MA_RW_LatchType MA_RW_Latch;
	boolean is_end = false;
	
	public MemoryAccess(Processor containingProcessor, EX_MA_LatchType eX_MA_Latch, MA_RW_LatchType mA_RW_Latch)
	{
		this.containingProcessor = containingProcessor;
		this.EX_MA_Latch = eX_MA_Latch;
		this.MA_RW_Latch = mA_RW_Latch;
	}
	
	public void performMA()
	{
		System.out.println("Entered MA");
		//TODO
		if(EX_MA_Latch.isMA_enable() && !is_end)
		{
			if(EX_MA_Latch.isMA_busy()){
				System.out.println("MA Busy");
				return;
			}

			System.out.println("Performing MA");
			MA_RW_Latch.setRd(EX_MA_Latch.getRd());
			MA_RW_Latch.setALUResult(EX_MA_Latch.getALUResult());
			this.controlunit = EX_MA_Latch.getControlUnit();
			MA_RW_Latch.setControlUnit(EX_MA_Latch.getControlUnit());
			// System.out.println(EX_MA_Latch.getControlUnit().isStore() || EX_MA_Latch.getControlUnit().isLoad());
			System.out.println("Opcode :  " + controlunit.getOpcode());
			if (controlunit.isEnd()){
				is_end = true;
			}

			if(controlunit.isLoad())
			{
				System.out.println("LOAD");
				// int ldRes = containingProcessor.getMainMemory().getWord(EX_MA_Latch.getALUResult());
				// MA_RW_Latch.setLoadResult(ldRes);
				Simulator.getEventQueue().addEvent(new MemoryReadEvent(Clock.getCurrentTime() + Configuration.L1d_latency , this , containingProcessor.getDataCache() , EX_MA_Latch.getALUResult()));
				EX_MA_Latch.setMA_busy(true);
			}
			else if(controlunit.isStore())
			{
				System.out.println("STORE");
				int location = EX_MA_Latch.getALUResult();
				int data = EX_MA_Latch.getStoreData();
				//containingProcessor.getMainMemory().setWord(location,data);
				Simulator.getEventQueue().addEvent(new MemoryWriteEvent(Clock.getCurrentTime() + Configuration.L1d_latency , this , containingProcessor.getDataCache() , location , data));
				// System.out.println("Location : " + location + "DATA: " + data);
				EX_MA_Latch.setMA_busy(true);
			}

			else{

				MA_RW_Latch.setRW_enable(true);
				EX_MA_Latch.setMA_enable(false);				

			}

		}
		else{

			controlunit = new ControlUnit();

		}
	}

	@Override
	public void handleEvent(Event e) {

		System.out.println("Handling");
		MemoryResponseEvent event = (MemoryResponseEvent) e;
		MA_RW_Latch.setLoadResult(event.getValue());
		EX_MA_Latch.setMA_enable(false);
		MA_RW_Latch.setRW_enable(true);
		EX_MA_Latch.setMA_busy(false);

	}	

}