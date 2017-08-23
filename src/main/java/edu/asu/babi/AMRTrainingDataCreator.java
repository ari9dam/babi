package edu.asu.babi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import edu.asu.nlu.common.ds.AnnotatedSentence;
import edu.asu.nlu.common.parsing.StanfordBerkeleyNlpPipeline;
import edu.stanford.nlp.ling.CoreLabel;


public class AMRTrainingDataCreator {
	static edu.stanford.nlp.pipeline.StanfordCoreNLP pipeline = new StanfordBerkeleyNlpPipeline(true).getPipeline();
	
	public static void main(String[] args) throws IOException {
		 
		// get all the train sentences and truth
		File file =new File("src\\main\\resources\\amrdb");
		String dest="src\\main\\resources\\data\\AMR-training\\";
		List<String> lines = new ArrayList<String>();
		List<Set<String>> truths = new ArrayList<Set<String>>();
		List<Set<String>> relationList  = new ArrayList<Set<String>> ();
		List<Set<String>> conceptList  = new ArrayList<Set<String>> ();
		
		boolean odd = true;
		Map<String,Integer> map = new HashMap<String,Integer>();
		
		int counter=0;
		for(String line: FileUtils.readLines(file, Charset.defaultCharset())){
			
			Set<String> relations = new HashSet<String>();
			Set<String> concepts = new HashSet<String>();
			if(line.trim().isEmpty()) continue;

			if(odd){
				lines.add(line.replace("# ::snt ", ""));
				map.put(line.replace("# ::snt ", ""), counter);
				counter++;
			}else{
				truths.add(getTruth(line,relations, concepts));
				relationList.add(relations);
				conceptList.add(concepts);
			}			
			odd = !odd;			
		}
		
		/**
		 * Read the training files
		 */
		File folder = new File("src\\main\\resources\\data\\babi-small\\");
		Set<String> traingdata = new HashSet<String>();
		for(File f: folder.listFiles()){
			traingdata.addAll(FileUtils.readLines(f, Charset.defaultCharset()));
		}

		for(String sen: traingdata){
			if(sen.contains("?")) continue;
			
			int i = map.get(getAMRId(sen));
			String line = lines.get(i);
			List<String> examples = createExamples(relationList.get(i),
					conceptList.get(i), truths.get(i));
			List<String> data = getSentenceRep(line);
			data.addAll(examples);
			FileUtils.writeLines(new File(dest+(i+1)+".sample"), data);
		}
	}
	
	private static String getAMRId(String stmt){
		//System.out.println(stmt.getText());
		String amrin = stmt.toLowerCase().trim().substring(stmt.indexOf(' ')+1,stmt.length());
		if(amrin.matches(".*? is (north|south|east|west) of .*?")){
			amrin = amrin.replace(" is ", " is_of ");
		}else if(amrin.contains(" left ")||amrin.contains(" right ")||amrin.contains(" above ")||amrin.contains(" below ")){
			amrin = amrin.replace(" is ", " is_of ")
					.replaceAll("red sphere", "red_sphere")
					.replaceAll("blue sphere", "blue_sphere")
					.replaceAll("yellow sphere", "yellow_sphere")
					.replaceAll("pink sphere", "pink_sphere")
					.replaceAll("red rectangle", "red_rectangle")
					.replaceAll("blue rectangle", "blue_rectangle")
					.replaceAll("yellow rectangle", "yellow_rectangle")
					.replaceAll("pink rectangle", "pink_rectangle")
					.replaceAll("red square", "red_square")
					.replaceAll("blue square", "blue_square")
					.replaceAll("yellow square", "yellow_square")
					.replaceAll("pink square", "pink_square");
		}else if(amrin.matches(".*? is .*?in .*?")){
			amrin = amrin.replace(" is ", " is_in ");
		}else if(amrin.contains("box of chocolates")){
			amrin = amrin.replaceAll("box of chocolates", "box_of_chocolate");
		}
		
		return amrin.replace(".", " .");
	}

	public static Set<String> getTruth(String line, Set<String> relations, Set<String> concepts){
		Set<String> ret = new HashSet<String>();

		line = line.replaceAll("\\(", " ").replaceAll("\\)", " ")
				.replaceAll("  " , " ");

		line = line.replace("p / person :name n / name :op", "")
				.replace("d / person :name m / name :op", "")
				.replaceAll("[a-z] / ", " ")
				.trim();

		String[] tokens = line.split("\t");

		String main = tokens[0].trim().replaceAll("-", "");

		for(int i=1;i<tokens.length;i++){
			process(tokens[i].trim(),main,ret,relations,concepts);
		}

		return ret;
	}

	public static void process(String part, String root, Set<String> truth, Set<String> relations, Set<String> concepts){
		String parts[] = part.replaceAll(" +", " ").split(" ");
		if(parts[1].trim().equalsIgnoreCase("-")){
			parts[1] = "negative";
		}
		String child = parts[1].trim().replaceAll("\"", "").toLowerCase();
		String rel =parts[0].replace(":", "").toLowerCase();
		truth.add(rel+"("
				+root+","+child+")");
		
		relations.add(rel);
		concepts.add(root);
		concepts.add(child);
		
		if(parts.length==2){
			return;
		}else if(parts.length==6){
			process(parts[2]+" "+parts[3],child,truth,relations, concepts);
			process(parts[4]+" "+parts[5],child,truth,relations, concepts);
		}else{
			System.out.println(part);
			System.exit(0);
		}
	}


	private static List<String> getSentenceRep(String line){
		List<String> ret = new LinkedList<String>();
		
		AnnotatedSentence sen = new AnnotatedSentence(line,pipeline.process(line));
		int pos=1;
		for(CoreLabel token : sen.getTokenSequence()){
			ret.add("pos("+pos+","+token.lemma().toLowerCase()+").");
		}
		
		return ret;
	}

	private static List<String> createExamples(Set<String> relations, Set<String> concepts,
			Set<String> truth){

		List<String> ret = new LinkedList<String>();

		for(String rel : relations){
			for(String c1: concepts){
				for(String c2: concepts){
					if(c1==c2)
						continue;
					String eg = rel+"(" + c1 + "," + c2 +")";
					if(truth.contains(eg)){
						ret.add("#example "+eg +" .");
					}else{
						ret.add("#example not "+eg +" .");
					}
				}
			}
		}

		return ret;
	}

}
