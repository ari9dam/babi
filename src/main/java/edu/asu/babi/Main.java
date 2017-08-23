package edu.asu.babi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

public class Main {

	public static void main(String[] args) throws IOException {
		String dir="C:\\Users\\Anisha Mazumder\\Downloads\\relational_mnist\\test";
		
		for(int i=0;i<10;i++){
			String name = dir+"//"+i;
			File f = new File(name);
			if(f.exists()&&!f.isDirectory() || !f.exists()){
				f.mkdir();
			}
		}
		
		File root = new File(dir);
		for(File f: root.listFiles()){
			if(!f.isDirectory()){
				String data = FileUtils.readFileToString(f,Charset.defaultCharset());
				if(data.contains("#example label(digit0).")){
					f.renameTo(new File(dir+"//0//"+f.getName()));
				}
				
				if(data.contains("#example label(digit1).")){
					f.renameTo(new File(dir+"//1//"+f.getName()));
				}
				
				if(data.contains("#example label(digit2).")){
					f.renameTo(new File(dir+"//2//"+f.getName()));
				}
				
				if(data.contains("#example label(digit3).")){
					f.renameTo(new File(dir+"//3//"+f.getName()));
				}
				
				if(data.contains("#example label(digit4).")){
					f.renameTo(new File(dir+"//4//"+f.getName()));
				}
				
				if(data.contains("#example label(digit5).")){
					f.renameTo(new File(dir+"//5//"+f.getName()));
				}
				
				if(data.contains("#example label(digit6).")){
					f.renameTo(new File(dir+"//6//"+f.getName()));
				}
				
				if(data.contains("#example label(digit7).")){
					f.renameTo(new File(dir+"//7//"+f.getName()));
				}
				
				if(data.contains("#example label(digit8).")){
					f.renameTo(new File(dir+"//8//"+f.getName()));
				}
				
				if(data.contains("#example label(digit9).")){
					f.renameTo(new File(dir+"//9//"+f.getName()));
				}
			}
		}
	}

}
