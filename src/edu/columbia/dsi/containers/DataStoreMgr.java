/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.Component.CompNames;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Sep 17, 2019
 */
public class DataStoreMgr {
	private static Logger logger = Logger.getLogger(DataStoreMgr.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private boolean validityFlag=false;
	private WorkDirHdlr workDirHdlr;
	private static final String dataStoreDir="DataStoreWorkDir";
	private File dataStoreHdlr;
	private PrintWriter writer;
	private Header header;
	private String dataStoreFileName="data_store_structure.txt";
	private String toolsVerSeparator;
	private String absolutePath=null;
	
	public class Header {
		private String relativeDirectory;
		private HashSet<Language> langs = new HashSet<Language>();
		private HashSet<Type> types = new HashSet<Type>();
		private String creationTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		private String createdBy = "SCRIPTS";
		
		
		public Header(String relativeDirectory) {	
			this.relativeDirectory=relativeDirectory;
		}
		
		public void add(Language lang, Type type) {
			langs.add(lang);
			types.add(type);		
		}
		
		public String getRelativeDirectory() {
			return this.relativeDirectory;
		}
		
		public String getTimeStamp() {
			return this.creationTimeStamp;
		}
		
		public String getCreatedBy() {
			return this.createdBy;
		}
		
		public String getLangs() {
			StringBuilder s= new StringBuilder();
			for(Language l:langs) {
				s.append(l.toString()+"\t");
			}
			return s.toString().trim().replaceAll("\t", ";;");
		}
		
		public String getTypes() {
			StringBuilder s= new StringBuilder();
			for(Type t:types) {
				s.append(t.toString()+"\t");
			}
			return s.toString().trim().replaceAll("\t", ";;");
		}
	}
	
	/**
	 * 
	 */
	public DataStoreMgr(Corpora corpora) {
		this.absolutePath=resourceFactory.getScriptsCorporaPath();
		ArrayList<String>folders=new ArrayList<String>();
		folders.add(dataStoreDir);
		workDirHdlr = new WorkDirHdlr(folders);
		try {
			dataStoreHdlr=new File(workDirHdlr.getSubFolderHdlr(dataStoreDir).getCanonicalPath()+File.separator+dataStoreFileName);
			header=new Header(corpora.getRelativeDirectory());
			for(Corpus c:corpora.getAllCorpora()) {
				header.add(c.getLanguage(), c.getType());
			}	
			this.toolsVerSeparator=corpora.getAllCorpora().get(0).getToolsVerSeparator();
			validityFlag=export(corpora);
			if(validityFlag) {
				logger.debug(dataStoreFileName+" has been created successfully");
			}else {
				logger.debug("Couldn't create the "+dataStoreFileName);
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public boolean isValid() {
		return validityFlag;
	}
	public File getDataStoreHdlr() {
		return dataStoreHdlr;		
	}
	
	private boolean export(Corpora corpora) {
		try {
			writer = new PrintWriter(dataStoreHdlr.getCanonicalPath());
			writer.println("[corpora_specs]");
			
			
			writer.println("relative_directory="+header.getRelativeDirectory());
			writer.println("all_languages_included="+header.getLangs()); //This is to identify all the languages you are going to index
			writer.println("all_types_included="+header.getTypes()); //This is to identify all the types the included corpora			
			writer.println("number_of_queries="+corpora.getAllQueries().size());  		
			writer.println("number_of_corpora="+corpora.getAllCorpora().size());  //This is to let you know how many indices you are going to create in this run.
			writer.println("created="+header.getTimeStamp());
			writer.println("created-by="+header.getCreatedBy());
			
			int counter=1;
			for(Query query:corpora.getAllQueries()) {
				writer.println("[query_"+String.valueOf(counter)+"]");
				counter++;
				writer.println("name="+ query.getQueryName());
				writer.println("language="+ query.getQueriesLang().toString());
				writer.println("QueryProcessing_location="+query.getOutQueryProcessingDir().getAbsolutePath());
				writer.println("QueryProcessor_version="+query.getQuerProcessorVerison());				
			}
			
			counter=1;
			for(Corpus corpus:corpora.getAllCorpora()) {
				String cName=corpus.getCorpusName();
				writer.println("[corpus_"+String.valueOf(counter)+"]");
				counter++;
				writer.println("name="+ corpus.getCorpusName());
				writer.println("language="+ corpus.getLanguage().toString());
				writer.println("type="+corpus.getType().toString());
				writer.println("[location]");
				writer.println("source_location="+corpus.getSourceFilesLocation());
				if(corpus.getManualTranslationLocation()!=null) {
					writer.println("[location]");
					writer.println("manual_translation_location="+corpus.getManualTranslationLocation());
				}
				if(corpus.getManualTranscriptionLocation()!=null) {
					writer.println("[location]");
					writer.println("manual_transcription_location="+corpus.getManualTranscriptionLocation());
				}
				
				Type type=corpus.getType();
				Language lang=corpus.getLanguage();
				if(type==Type.audio) {
					Component asrPipelineComp=corpus.getComponent(CompNames.asr);
					exportComponent(asrPipelineComp, cName, "ASR", null);
					
					Component kwsPipelineComp=corpus.getComponent(CompNames.kws);
					exportComponent(kwsPipelineComp, cName, "KWS", null);
					
					ArrayList<Double> nbThresholds=null;
					ArrayList<Double> wbThresholds=null;
					// if(lang==Language.sw) {
					// 	nbThresholds=resourceFactory.getSWNBAudioLangIdThrsholds();
					// 	wbThresholds=resourceFactory.getSWWBAudioLangIdThrsholds();
					// }else if(lang==Language.tl) {
					// 	nbThresholds=resourceFactory.getTLNBAudioLangIdThrsholds();
					// 	wbThresholds=resourceFactory.getTLWBAudioLangIdThrsholds();
					// }else if(lang==Language.so) {
					// 	nbThresholds=resourceFactory.getSONBAudioLangIdThrsholds();
					// 	wbThresholds=resourceFactory.getSOWBAudioLangIdThrsholds();
					// }
					ArrayList<String> langIdLocationSuffix=null;
					if(wbThresholds!=null && nbThresholds!=null && nbThresholds.size()==wbThresholds.size()) {
						langIdLocationSuffix=new ArrayList<String>();
						for(int i=0;i<nbThresholds.size();i++) {
							double nbThreshold=nbThresholds.get(i).doubleValue();
							double wbThreshold=wbThresholds.get(i).doubleValue();
							langIdLocationSuffix.add("-nbThreshold-"+String.valueOf(nbThreshold)+"-wbThreshold-"+String.valueOf(wbThreshold));
						}
					}
					Component audLangIdPipelineComp=corpus.getComponent(CompNames.audLangId);
					exportComponent(audLangIdPipelineComp, cName, "Language_Identification",langIdLocationSuffix);
					
					Component audioSentSpPipelineComp=corpus.getComponent(CompNames.audSentSp);
					exportComponent(audioSentSpPipelineComp, cName, "SentSplitter", null);
					
					
//					Hashtable<String, String> asrDocExpVerToShortLocationMap = corpus.getDocExpVerToShortLocationMap();
//					for(Entry<String, String> e:asrDocExpVerToShortLocationMap.entrySet()) {
//						String[]toolsVersions=e.getKey().split(corpus.getToolsVerSeparator(),2);
//						ArrayList<Double> thresholds=resourceFactory.getDocExpThreshold();
//						for(Double threshold:thresholds) {
//							writer.println("[location]");
//							writer.println("ASR_Document_Expansion_location="+e.getValue()+"-threshold-"+threshold.toString());
//							writer.println("ASR_Document_Expansion_version="+toolsVersions[0]);
//							
//							String docExpSource=null;
//							if(toolsVersions.length==2) {
//								docExpSource=corpus.getAsrShortLocation(toolsVersions[1]);						
//							}
//							writer.println("ASR_Document_Expansion_source="+docExpSource);							
//						}						
//					}
					
				}else if(type==Type.text) {
					Component sentSpPipelineComp=corpus.getComponent(CompNames.sentSp);
					exportComponent(sentSpPipelineComp, cName, "SentSplitter", null);	
					
					Component langIdPipelineComp=corpus.getComponent(CompNames.langId);
					exportComponent(langIdPipelineComp, cName, "Language_Identification", null);		
				}
				
				Component morphologyPipelineComp=corpus.getComponent(CompNames.morph);
				exportComponent(morphologyPipelineComp, cName, "Morpohological_Analysis", null);
				
				Component mtPipelineComp=corpus.getComponent(CompNames.mt);
				exportComponent(mtPipelineComp, cName, "MT", null);
				
				Component postEditPipelineComp=corpus.getComponent(CompNames.postEd);
				exportComponent(postEditPipelineComp, cName, "MT_Postedit", null);
				
				Component domIdPipelineComp=corpus.getComponent(CompNames.domId);
				exportComponent(domIdPipelineComp, cName, "Domain_Identification", null);

				Component stemmerPipelineComp=corpus.getComponent(CompNames.stmr);
				exportComponent(stemmerPipelineComp, cName, "Stemmer", null);
				
				
				
				writer.println("[index]");
				writer.println("index_out_root_location="+corpus.getIndexLocation());
				writer.println("indexer_version="+resourceFactory.getUmdIndexerVersion());
			}			
			writer.close();		
		} catch (IOException e) {
			logger.error(e.getMessage());
			writer.close();
			return false;
		}
		return true;		
	}
	
	private void exportComponent(Component comp, String corpusName, String name, ArrayList<String> suffix) {
		Hashtable<String, String> verToShortLocationMap = new Hashtable<String, String>();
		if(comp!=null) {
			verToShortLocationMap=comp.getVerToShortLocationMap();
		}	
		
		ArrayList<String> locationSuffix=new ArrayList<String>();
		if(suffix!=null) {
			locationSuffix.addAll(suffix);
		}else {
			locationSuffix.add("");//empty suffix
		}
		for(Entry<String, String> e:verToShortLocationMap.entrySet()) {
			String[]toolsVersions=e.getKey().split(this.toolsVerSeparator,2);
			String compSrc=comp.getSourceShortLocation(e.getKey());
			for(int i=0;i<locationSuffix.size();i++) {
				if(comp.getName()==CompNames.postEd) {
					File pathTmp=new File(this.absolutePath+File.separator+corpusName+File.separator+(e.getValue()+locationSuffix.get(i)).trim());
					ArrayList<String> opDrNames = IoHdlr.getInstance().getListOfDirsNames(pathTmp);
					for(String opName:opDrNames) {
						writer.println("[location]");
						writer.println(name+"_location="+(e.getValue()+locationSuffix.get(i)).trim()+File.separator+opName);
						writer.println(name+"_version="+toolsVersions[0]);
						writer.println(name+"_source="+compSrc);						
					}					
				}else {
					writer.println("[location]");
					writer.println(name+"_location="+(e.getValue()+locationSuffix.get(i)).trim());
					writer.println(name+"_version="+toolsVersions[0]);
					writer.println(name+"_source="+compSrc);
					
					if(comp.getName()==CompNames.asr) {
						File pathTmp=new File(this.absolutePath+File.separator+corpusName+File.separator+(e.getValue()+locationSuffix.get(i)).trim()+File.separator+"KWS-FILES");
						if(pathTmp.exists()) {
							writer.println("[location]");
							writer.println(name+"_location="+(e.getValue()+locationSuffix.get(i)).trim()+File.separator+"KWS-FILES");
							writer.println(name+"_version="+toolsVersions[0]);
							writer.println(name+"_source="+compSrc);
						}
					}					
				}				
			}		
		}
	}
	
	
	public void cleanUp() {
		workDirHdlr.cleanup();
	}
}
