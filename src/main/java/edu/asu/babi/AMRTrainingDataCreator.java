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
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class AMRTrainingDataCreator {
	static StanfordCoreNLP pipeline = new StanfordBerkeleyNlpPipeline(true).getPipeline();
	static int count =0;
	public static void main(String[] args) throws IOException {

		// get all the train sentences and truth
		File file =new File("src\\main\\resources\\amrdb");
		String dest="src\\main\\resources\\data\\AMR-training\\";
		List<String> lines = new ArrayList<String>();
		List<Set<String>> truths = new ArrayList<Set<String>>();
		List<Set<String>> relationList  = new ArrayList<Set<String>> ();
		List<Set<String>> conceptList  = new ArrayList<Set<String>> ();
		Set<String> allConcepts = new HashSet<String>();
		Set<String> allRelations = new HashSet<String>();
		Set<String> constantModes = new HashSet<String>();
		
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
				Set<String> extruth = getTruth(line,relations, concepts);
				for(String s: extruth){
					if(true||s.startsWith("degree")||s.startsWith("direction")||s.startsWith("polarity")){
						constantModes.add(s);
					}
				}
				truths.add(extruth);
				relationList.add(relations);
				conceptList.add(concepts);
			}			
			odd = !odd;			
		}
		
		System.out.println(constantModes.size());
		
		/**
		 * Read the training files
		 */
		File folder = new File("src\\main\\resources\\data\\babi-small\\");
		List<String> traingdata = new LinkedList<String>();
		Set<String> tData = new HashSet<String>();
		for(File f: folder.listFiles()){
			for(String line: FileUtils.readLines(f,Charset.defaultCharset())){
				String sen = line.substring(line.indexOf(' ')+1, line.length());

				if(!line.contains("?")&& !tData.contains(sen)){
					traingdata.add(sen);
					tData.add(sen);
				}
			}
		}

		counter=0;
		for(String sen: traingdata){
			if(sen.contains("?")) continue;

			int i = map.get(getAMRId(sen));
			allConcepts.addAll(conceptList.get(i));
			allRelations.addAll(relationList.get(i));
		}

		/**
		 * non lemma based concepts
		 */
		Set<String> nonLemmaConcepts = new HashSet<String>();
		nonLemmaConcepts.add("relativeposition01");
		nonLemmaConcepts.add("negative");
		nonLemmaConcepts.add("belocatedat91");
		nonLemmaConcepts.add("fear");
		nonLemmaConcepts.add("domain");
		
		allConcepts.removeAll(nonLemmaConcepts);
		
		ArrayList<String> lemaBasedRootConcepts = new ArrayList<String>();
		for(String c:allConcepts){
			if(c.contains("1")||c.contains("2")||c.contains("0")||c.equalsIgnoreCase("either")){
				lemaBasedRootConcepts.add(c);
			}
		}
		
		Set<String> posTags = new HashSet<String>();
		Set<String> words = new HashSet<String>();

		System.out.println("Unique"+traingdata.size());

		for(String sen: traingdata){
			if(sen.contains("?")) continue;

			int i = map.get(getAMRId(sen));
			String line = sen.toLowerCase().replace(".", ". ");

			Set<String> outRelations = new HashSet<String>(allRelations);
			outRelations.removeAll(relationList.get(i));
			List<List<String>> examples = createExamples(relationList.get(i), conceptList.get(i),
					truths.get(i),outRelations, nonLemmaConcepts, lemaBasedRootConcepts,constantModes);

			List<String> data = getSentenceRep(line,posTags,words);
			for(List<String> ex:examples ){
				List<String> rep = new LinkedList<String>(data);
				for(String c: nonLemmaConcepts){
					rep.add("concept("+c+").");
				}
				for(String c: conceptList.get(i)){
					if(!nonLemmaConcepts.contains(c))
						rep.add("concept("+c+").");
				}
								
				rep.addAll(ex);
				FileUtils.writeLines(new File(dest+(counter+1)+".sample"), rep);
				counter++;
			}
		}

		List<String> bk = new LinkedList<String>();

		

		for(String c: posTags){
			bk.add("postag("+c+").");
		}

		words.removeAll(allConcepts);
		words.clear();
		words.add("back");
		words.add("north");
		words.add("south");
		words.add("east");
		words.add("west");
		words.add("yesterday");
		words.add("afternoon");
		words.add("morning");
		words.add("evening");
		words.add("left");
		words.add("right");
		words.add("above");
		words.add("below");

		for(String c: words){
			bk.add("token("+c+").");
		}

		bk.add("next(J,I):- index(I),index(J), I=J+1.");
		bk.add("token(X):- lemma(I,X), not concept(X), index(I).");
		//bk.add("postag(X):-pos(I,X), index(I).");

		FileUtils.writeLines(new File(dest+"domain.bk"), bk);
	}


	private static String getAMRId(String stmt){
		//System.out.println(stmt.getText());
		String amrin = stmt.toLowerCase().trim();
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
		line = line.replaceAll("compared-to", "comparedto");

		line = line.replace("p / person :name n / name :op", "")
				.replace("d / person :name m / name :op", "")
				.replaceAll("[a-z] / ", " ")
				.trim();

		String[] tokens = line.split("\t");

		String main = tokens[0].trim().replaceAll("-", "").replaceAll("_", "");

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
		String child = parts[1].trim().replaceAll("\"", "").toLowerCase().replaceAll("-", "").replaceAll("_", "");
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


	private static List<String> getSentenceRep(String line, Set<String> postags, Set<String> words){
		List<String> ret = new LinkedList<String>();
		line = line.replaceAll("red sphere", "redsphere")
				.replaceAll("blue sphere", "bluesphere")
				.replaceAll("yellow sphere", "yellowsphere")
				.replaceAll("pink sphere", "pinksphere")
				.replaceAll("red rectangle", "redrectangle")
				.replaceAll("blue rectangle", "bluerectangle")
				.replaceAll("yellow rectangle", "yellowrectangle")
				.replaceAll("pink rectangle", "pinkrectangle")
				.replaceAll("red square", "redsquare")
				.replaceAll("blue square", "bluesquare")
				.replaceAll("yellow square", "yellowsquare")
				.replaceAll("pink square", "pinksquare")
				.replaceAll("box of chocolate", "boxofchocolate");
		Annotation asen = pipeline.process(line);

		AnnotatedSentence sen = new 
				AnnotatedSentence(line,asen.get(SentencesAnnotation.class).get(0));
		int pos=1;

		for(CoreLabel token : sen.getTokenSequence()){

			if(token.lemma().equalsIgnoreCase(".")) continue;
			words.add(token.lemma().toLowerCase());
			ret.add("lemma("+pos+","+token.lemma().toLowerCase().
					replaceAll("not", "no").replaceAll("tire", "tired").replaceAll("bore", "bored")+").");
			ret.add("glemma("+pos+","+token.lemma().toLowerCase().replaceAll("not", "no").replaceAll("tire", "tired").replaceAll("bore", "bored")+").");
			ret.add("pos("+pos+","+token.getString(PartOfSpeechAnnotation.class).toLowerCase()+").");
			//ret.add("gpos("+pos+","+token.getString(PartOfSpeechAnnotation.class).toLowerCase()+").");
			pos++;
			postags.add(token.getString(PartOfSpeechAnnotation.class).toLowerCase());
		}

		ret.add("index(1.."+(pos-1)+").");
		if(line.contains(" is ")){
			ret.add("length("+(pos-1)+").");
		}
		return ret;
	}

	private static List<List<String>> createExamples(Set<String> relations, Set<String> concepts,
			Set<String> truth, Set<String> extraRelations, Set<String> nonLemmaConcepts, 
			ArrayList<String> lemmaBasedRootConcepts, Set<String> constantModes){
		List<List<String>> out = new LinkedList<List<String>>();
		List<String> ret = new LinkedList<String>();
		List<String> negetives = new LinkedList<String>();
		Set<String> possibleConcepts = new HashSet<String>(concepts);
		possibleConcepts.addAll(nonLemmaConcepts);
		
		
		int rand = (int) (Math.random()*lemmaBasedRootConcepts.size());
		possibleConcepts.add(lemmaBasedRootConcepts.get(rand));

		
		
		Set<String> allRelations = new HashSet<String>(relations);
		allRelations.addAll(extraRelations);


		Set<String> rootConcepts = new HashSet<String>();
		for(String s: possibleConcepts)
			if(s.contains("0")||s.equalsIgnoreCase("either")||s.contains("1")
					||s.equalsIgnoreCase("domain")||s.equalsIgnoreCase("fear"))
				rootConcepts.add(s);
		
		Set<String> nonRootConcepts = new HashSet<String>(possibleConcepts);
		nonRootConcepts.removeAll(rootConcepts);
		nonRootConcepts.add("either");

		
		/**
		 * positives and negatives based on only input
		 */
		for(String rel : allRelations){
			for(String c1: rootConcepts){
				for(String c2: nonRootConcepts){
					if(c1==c2)
						continue;
					String eg = rel+"(" + c1 + "," + c2 +")";
					if(truth.contains(eg)){
						ret.add("#example "+eg +" .");
					}else{
						negetives.add("#example not "+eg +" .");
					}
				}
			}
		}
		
		
		
		int size=500;
		for(int i=0;i<negetives.size();){
			List<String> exp = new LinkedList<String>();
			if(i==0)
				exp.addAll(ret);
			exp.addAll(negetives.subList(i, Math.min(negetives.size(), i+ size)));
			exp.add("concept("+lemmaBasedRootConcepts.get(rand)+").");
			i=i+size;
			out.add(exp);
		}
		
		List<String> exp = new LinkedList<String>();
		for(String s : constantModes){
			if(!truth.contains(s)){
				exp.add("#example not "+s +" .");
			}
		}
		out.add(exp);
		
		return out;
	}

}
