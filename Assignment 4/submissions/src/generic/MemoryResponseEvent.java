package generic;

public class MemoryResponseEvent extends Event {

	int value;
	int[] line;

	public MemoryResponseEvent(long eventTime, Element requestingElement, Element processingElement, int value , int[] line) {
		super(eventTime, EventType.MemoryResponse, requestingElement, processingElement);
		this.value = value;
		this.line = line;
	}

	public int getValue() {
		return value;
	}

	public int[] getLine() {
		return line;
	}

	public void setValue(int value) {
		this.value = value;
	}

}