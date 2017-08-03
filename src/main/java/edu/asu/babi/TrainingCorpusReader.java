/**TrainingCorpusReader.java
 * 4:16:53 PM @author Arindam
 */
package edu.asu.babi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import edu.asu.babi.entity.*;




public class TrainingCorpusReader implements Iterable<Story>{
	private ArrayList<Story> corpus;

	public TrainingCorpusReader(String filename) throws IOException{
		Story story = null;
		this.corpus = new ArrayList<Story>();
		for (String line : Files.readAllLines(Paths.get(filename))) {
			Sentence s = createStatement(line);
			if(s.getLineNumber()==1){
				story = new Story();
				this.corpus.add(story);
				story.AddSentence(s);
			}else{
				story.AddSentence(s);
			}
		}
		
		Collections.sort(corpus);
	}

	private Sentence createStatement(String line){
		Sentence s = null;
		int index = line.indexOf(' ');
		String id = line.substring(0, index);
		String data[] = line.substring(index+1).split("\t");
		String text = data[0].replace(".", "").replace("?", "");

		if(data.length==1){
			s = new Statement(id,text);
		}else{
			String sfl[] = data[2].split(" ");
			String ans = data[1];
			s = new Question(id, text, ans, sfl);
		}
		return s;
	}

	public Iterator<Story> iterator() {
		return this.corpus.iterator();
	}
	
	public static void main(String args[]) throws IOException{
		String dir = "src\\main\\resources\\data\\en-10k\\";
		String task1 = "qa7_counting_train.txt";
		TrainingCorpusReader tr = new TrainingCorpusReader(dir+task1);
		Iterator<Story> it = tr.iterator();
		
		while(it.hasNext()){
			System.out.println(it.next().getSentences().size());
		}
	}
	
}
