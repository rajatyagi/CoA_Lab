package processor.memorysystem;

public class CacheLine {
	
	int tag;
	int[] data;
	int lineSize = 4;

	public CacheLine() {

		data = new int[lineSize];
		tag=-1;

		for(int i = 0 ; i < lineSize ; i++){
			data[i] = -1;
		}
		
	}
	
	public void setData(int[] value) {
		data = value ;
	}

	public void setDataElement(int i , int value) {
		data[i] = value ;
	}

	public int[] getdata() {
		return data;
	}
	
	public void settag(int addr) {
		tag = addr;
	}
	public int gettag() {
		return tag;
	}
}