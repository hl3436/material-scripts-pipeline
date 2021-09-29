/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import org.apache.log4j.Logger;
import edu.columbia.dsi.CLIRExperimenter;
import edu.columbia.dsi.utils.TuningConfigBuilder.MatcherConfigurationObj;

/**
 * @author badrashiny
 * Dec 3, 2018
 */
public class MatcherTuner {
	private static Logger logger = Logger.getLogger(CLIRExperimenter.class.getSimpleName());
	ArrayList<MatcherConfigurationObj> matchersConfigsList=new ArrayList<MatcherConfigurationObj>();
	TuningConfigBuilder configBuilder=null;
	/**
	 * 
	 */
	public MatcherTuner(File configTemplate, File mathcersList) {
		logger.debug("Initializing the matcher tuning algorithm...");
		configBuilder= new TuningConfigBuilder(configTemplate, mathcersList);
		matchersConfigsList.addAll(configBuilder.getMatchersList());
	}
	
	public void tune(File outDir, int maxMatchersLimit) {
		int total=(int) Math.pow(2.0, (double)matchersConfigsList.size())-1;
		int i=1;//we don't want all zeros case		
		ArrayList<String>results=new ArrayList<String>();
		String header="Exp-name"+"\t"+"Location"+"\t"+"Score";
		results.add(header);
		while(i<=total){
			ArrayList<MatcherConfigurationObj> expMatchers = new ArrayList<MatcherConfigurationObj>();
			expMatchers.addAll(getMatchersList(i, matchersConfigsList));
			if(expMatchers.size()>maxMatchersLimit) {
				i++;
				continue;
			}			
			StringBuilder description =new StringBuilder();
			for(MatcherConfigurationObj expM:expMatchers) {
				description.append(expM.getConfigName()+"\t");
			}
			
			ArrayList<String>folders=new ArrayList<String>();
			String wrkDirName="tuner"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
			folders.add(wrkDirName);				
			WorkDirHdlr workDirHdlr = new WorkDirHdlr(folders);
			String expName=description.toString().trim().replaceAll("\t", "_");
			String configFilePath=workDirHdlr.getSubFolderHdlr(wrkDirName).getAbsolutePath()+File.separator+expName+".json";
			try {
				String expOutDir=outDir.getAbsolutePath()+File.separator+configBuilder.build(expMatchers, expName, configFilePath);
				File outEvalFile=new File(expOutDir+File.separator+"CLIR-Eval"+File.separator+"scores_cambridge_AQWV.csv");
				System.out.println("Processing["+String.valueOf(i)+"/"+String.valueOf(total)+"]: "+expName+"-->"+outEvalFile.getAbsolutePath());
				if(!outEvalFile.exists()) {
//					CLIRExperimenter experimenter= new CLIRExperimenter();
//	    			experimenter.runExperiment(new File(configFilePath), outDir);
//	        		experimenter.shutdown(); 
				}
				
				if(!outEvalFile.exists()) {
					logger.fatal("Error while processing the expeiment number: "+String.valueOf(i)+":"+ expName); 
				}else {
					ArrayList<String> scoresFile=IoHdlr.getInstance().readFileLinesWithoutHeader(outEvalFile);
					String[] tmp=scoresFile.get(0).split(",");
					String score=null;
					if(tmp.length==4) {
						score=tmp[3];
					}					
					String result=expName+"\t"+expOutDir+"\t"+score;
					results.add(result);
				}				
			} catch (IOException e) {
				logger.fatal("Error while processing the expeiment number: "+String.valueOf(i)+":"+ expName);
				logger.fatal(e.getMessage());
			}
			workDirHdlr.cleanup();
			i++;
		}
		IoHdlr.getInstance().exportFile(results, new File(outDir.getAbsolutePath()+File.separator+"results.tsv"));		
	}
	
	private ArrayList<MatcherConfigurationObj> getMatchersList(int n, ArrayList<MatcherConfigurationObj> allMatchers){
		int[] bits=Dec2Bin(n,allMatchers.size());
		ArrayList<MatcherConfigurationObj> matchers=new ArrayList<MatcherConfigurationObj>();
		for(int j=0;j<bits.length;j++){
			if(bits[j]==1){
				matchers.add(allMatchers.get(j));
			}			
		}
		return matchers;		
	}
	private int [] Dec2Bin(int n,int bits) {
		 int bin[]=Dec2Bin(n);  
	     int s=bin.length;  
	     if(s==bits) return bin;  
	     else if(s>bits) return null;  
	     int x[]=new int[bits];  
	     int i=0;  
	     while(i<(bits-s)) { 
	    	 x[i++]=0;  
	     } 
	     for(int k=0;k<s;k++)
	    	 x[i++]=bin[k];  
		 return x; 	         
	 }  
	 
	private int [] Dec2Bin(int n) {
		 int s=0,k=0;  
		 while(Math.pow(2,s)-1<n) s++;  
	     int x[]=new int[s];  
	     while(n!=0) {
	    	 x[k++]=n%2;  
	         n=n/2;	              
	     }  
	     for(int i=0;i<x.length/2;i++) { 
	    	 int tmp=x[i];  
	         x[i]=x[s-i-1];  
	         x[s-i-1]=tmp;	        	  
	     }  
	     return x;         
	 }

}
