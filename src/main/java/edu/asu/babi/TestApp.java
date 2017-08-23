package edu.asu.babi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import java.util.Map.Entry;

import edu.asu.babi.entity.Question;
import edu.asu.babi.entity.Sentence;
import edu.asu.babi.entity.Statement;
import edu.asu.babi.entity.Story;
import edu.asu.babi.parser.JAMRParser;
import edu.asu.babi.parser.ParseOutput;
import edu.asu.babi.parser.Parser;
import ilpme.entities.PartialHypotheis;
import ilpme.entities.Sample;
import ilpme.utils.SatisfiabilityUtils;

public class TestApp {
	public static void main(String[] args) throws IOException {
		SatisfiabilityUtils asp = 
				new SatisfiabilityUtils( "C:\\Users\\Anisha Mazumder\\Downloads\\gringo-3.0.5-win64\\gringo.exe",
						"C:\\Users\\Anisha Mazumder\\Downloads\\clasp-3.1.0\\clasp.exe");
		
		String dir = "src\\main\\resources\\data\\en-10k\\";
		String task = "qa17_positional-reasoning_test.txt";
		String bg = "src\\main\\resources\\bk.lp";
		TrainingCorpusReader tr = new TrainingCorpusReader(dir+task);
		Iterator<Story> it = tr.iterator();
		Parser parser = new Parser(false);
		JAMRParser jamr = new JAMRParser();
		
		Map<String,Set<String>> domains = createEmptyArgsMap();
		Map<String,Set<String>> prevValues = new HashMap<String,Set<String>>();
		int maxT = 1;
		String bk = FileUtils.readFileToString(new File(bg), Charset.defaultCharset());
		int iter=0;
		while(it.hasNext()){
			iter++;
			Story story = it.next();
			List<String> data = new LinkedList<String>();
			prevValues = new HashMap<String,Set<String>>();
			int T = 1;
			for(Sentence sen: story.getSentences()){
				ParseOutput parse = null;
				if(sen instanceof Statement){
					//parse the sentence
					parse = jamr.getFact((Statement)sen,T, prevValues);
					//add domain enties
					mergeDomains(domains,parse.getArgs());
				}else{
					parse = parser.getConstraint((Question)sen, domains,T,false, prevValues);
				}
				
				//add to the program
				data.add(parse.getLogicalRepresentation());
				if(parse.isShouldIncrement())
					T++;
				
				if(T>maxT) maxT = T;
			}
			File temp = new File("temp.sample");
			FileUtils.writeLines(temp, data);
			if(asp.doesEntail(bk, new Sample(temp.toPath()), new LinkedList<String>())){
				continue;
			}else{
				System.out.println("failed");
			}
			 
		}
		
		
	
		
	}
	
	private static Map<String, Set<String>> createEmptyArgsMap(){
		Map<String, Set<String>> args = new HashMap<String, Set<String>>();
		args.put("arg1", new HashSet<String>());
		args.put("arg2", new HashSet<String>());
		args.put("arg3", new HashSet<String>());
		args.put("arg4", new HashSet<String>());
		args.put("direction", new HashSet<String>());
		args.get("direction").add("east");
		args.put("eventId", new HashSet<String>());
		args.put("id", new HashSet<String>());
		return args;
	}
	
	private static void mergeDomains(Map<String,Set<String>> domains, Map<String,Set<String>> args ){
		for(Entry<String, Set<String>> entry: domains.entrySet()){
			entry.getValue().addAll(args.get(entry.getKey()));
		}
	}
}
