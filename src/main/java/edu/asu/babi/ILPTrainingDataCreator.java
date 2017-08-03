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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Joiner;

import edu.asu.babi.entity.Question;
import edu.asu.babi.entity.Sentence;
import edu.asu.babi.entity.Statement;
import edu.asu.babi.entity.Story;
import edu.asu.babi.parser.JAMRParser;
import edu.asu.babi.parser.ParseOutput;
import edu.asu.babi.parser.Parser;



public class ILPTrainingDataCreator {

	public static void main(String[] args) throws IOException {
		String dest="src\\main\\resources\\data\\ILP-training\\task1\\";
		String dir = "src\\main\\resources\\data\\en-10k\\";
		String task = "qa1_single-supporting-fact_train.txt";
		TrainingCorpusReader tr = new TrainingCorpusReader(dir+task);
		Iterator<Story> it = tr.iterator();
		Parser parser = new Parser(false);
		JAMRParser jamr = new JAMRParser();
		
		//create a folder if not exists
		//create a subfolder for task
		//create .sample files for each story
		Map<String,Set<String>> domains = createEmptyArgsMap();
		Map<String,Set<String>> prevValues = new HashMap<String,Set<String>>();
		int maxT = 1;
		int counter=1;
		while(it.hasNext()){
			Story story = it.next();
			List<String> data = new LinkedList<String>();
			prevValues = new HashMap<String,Set<String>>();
			int T = 1;
			for(Sentence sen: story.getSentences()){
				ParseOutput parse = null;
				if(sen instanceof Statement){
					//parse the sentence
					parse = jamr.getFact((Statement)sen,T);
					//add domain enties
					mergeDomains(domains,parse.getArgs());
				}else{
					parse = parser.getConstraint((Question)sen, domains,T,false, prevValues);
				}
				
				//add to the program
				if(parse.getLogicalRepresentation()==null)
					parse.getLogicalRepresentation();
				data.add(parse.getLogicalRepresentation());
				//if(parse.isShouldIncrement())
					//T++;
				T++;
				if(T>maxT) maxT = T;
			}
			
			//print the sample
			FileUtils.writeLines(new File(dest+counter+".sample"), data);
			counter++;
		}
		
		String domain = "time(1.."+maxT+").";
		
		for(Entry<String, Set<String>> entry : domains.entrySet()){
			if(entry.getValue().isEmpty())
				continue;
			if(!entry.getKey().equalsIgnoreCase("eventId"))
				domain+="\n"+entry.getKey()+"(" + Joiner.on(";").join(entry.getValue())+").";
			else
				domain+="\n"+entry.getKey()+"(" + Joiner.on(";;").join(entry.getValue())+").";
		}
		
		FileUtils.writeStringToFile(new File(dest+"domain.bk"), domain, Charset.defaultCharset());
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
