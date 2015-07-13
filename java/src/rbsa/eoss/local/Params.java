/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss.local;

/**
 *
 * @author dani
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
public class Params {
    //public static String master_xls;
    //public static boolean recompute_scores;
    
    public static String path;// used
    public static String req_mode;//used
    public static String name;//used
    public static String run_mode;//used
    public static String initial_pop;
    public static String template_definition_xls;// used
    public static String instrument_capability_xls;// used
    public static String requirement_satisfaction_xls;
    public static String aggregation_xls;
    public static String capability_rules_xls;//used
    
     public static String instrument_capability_xml;//used
     public static String aggregation_xml; //used
    
    public static String module_definition_clp;// used
    public static String template_definition_clp;// used
    public static String[] functions_clp = new String[2];// used
    public static String assimilation_rules_clp;// used
    public static String aggregation_rules_clp;// used
    public static String fuzzy_aggregation_rules_clp;//used
    public static String database_completeness_clp;
    public static String attribute_inheritance_clp;
    public static String orbit_rules_clp;
    public static String capability_rules_clp;
    public static String synergy_rules_clp;//Used
    public static String explanation_rules_clp;//Used
    public static String fuzzy_attribute_clp;
    public static String value_aggregation_clp;
    public static String requirement_satisfaction_clp;
    public static String cost_estimation_rules_clp; // used
    public static String fuzzy_cost_estimation_rules_clp; //used
    public static String adhoc_rules_clp;
    public static String search_heuristic_rules_clp;
    
    // Metrics for utility and pareto calculations
    public static ArrayList<String> pareto_metrics;
    public static ArrayList<String> pareto_metric_types;
    public static int pareto_ranking_depth;
    public static int pareto_ranking_threshold;
    public static ArrayList<String> util_metrics;
    public static ArrayList<String> util_metric_types;
    public static ArrayList<Double> util_metric_weights;
    public static double min_science;
    public static double max_science;
    public static double min_cost;
    public static double max_cost;
    public static int num_improve_heuristics;
    public static double prob_accept;
    // Instruments
    public static String[] instrument_list = {"PATH_GEOSTAR","EON_50_1","EON_118_1","EON_183_1","EON_ATMS_1"};
    public static int ninstr;
    public static String[] orbit_list = {"GEO-35788-equat-NA","SSO-600-SSO-DD","LEO-600-ISS-NA","SSO-800-SSO-AM","SSO-800-SSO-PM"};
    public static int norb;
    public static HashMap instrument_indexes;
    public static HashMap orbit_indexes;
    public static int[] nsats = {1,2,3,4,5,6,7,8};
    
    // Results
    public static String path_save_results;
    
    // Intermediate results
    public static HashMap requirement_rules;
    public static HashMap measurements_to_subobjectives;
    public static HashMap measurements_to_objectives;
    public static HashMap measurements_to_panels;
    public static ArrayList parameter_list;
    public static ArrayList objectives;
    public static ArrayList subobjectives;
    public static HashMap instruments_to_measurements;
    public static HashMap instruments_to_subobjectives;
    public static HashMap instruments_to_objectives;
    public static HashMap instruments_to_panels;
    public static HashMap measurements_to_instruments;
    public static HashMap subobjectives_to_instruments;
    public static HashMap objectives_to_instruments;
    public static HashMap panels_to_instruments;
    public static HashMap subobjectives_to_measurements;
    public static HashMap objectives_to_measurements;
    public static HashMap panels_to_measurements;
    
    public static int npanels;
    public static ArrayList<Double> panel_weights;
    public static ArrayList<String> panel_names;
    public static ArrayList obj_weights;
    public static ArrayList<Integer> num_objectives_per_panel;
    public static ArrayList subobj_weights;
    public static HashMap subobj_weights_map;
    public static HashMap revtimes;
    public static HashMap scores;
    public static HashMap subobj_scores;
    public static HashMap capabilities;
    public static HashMap all_dsms;
    //Cubesat costs model
    public static HashMap instrument_masses;
    public static String capability_dat_file;
    public static String revtimes_dat_file;
    public static String dsm_dat_file;
    public static String scores_dat_file;
    public static HashMap<String,Double> instrument_costs;
    public static HashMap<String,Double> instrument_sizes;
    public static TreeMap<Double,Double> bus_costs;
    public static TreeMap<Double,Double> bus_lifetimes;
    public static HashMap<String,Double> launch_costs;
    public static double time_horizon;
    public static HashMap<String,Double> lifetimes;
    
    
    //To access Dropbox files
    public final String APP_KEY = "501z5dhek2czvcm";
    public final String APP_SECRET = "q21mrispb7be3oz";
    
    /**
     * Constructor loads in all the paths from a .properties file stored in the project folder/config/
     * @param p Determines the path to the main project folder. Main project folder needs to contain xls, clp, results folder
     * @param mode Can choose between slow and fast. Slow generates all capabilities. Fast has a precomputed look-up table of the capabilities
     * @param name Name of the run
     * @param run_mode Can choose between normal and debug. Normal will attach all explanation to each architecture. Debug bypasses this. Result file from debug mode will require less storage space
     * @param search_clp clp file that contains the search strategy rules
     */
    public Params( String p , String mode, String name, String run_mode, String search_clp)
    {
        //this.master_xls = master_xls;
        //this.recompute_scores = recompute_scores;
        Params.path = p;
        Params.req_mode = mode;
        Params.name = name;
        Params.run_mode = run_mode;
        
        //reads in config file that contains paths to all the clp, dat, and xls files
        File configFile = new File(path+File.separator+"config"+File.separator+"Params.properties"); 
        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);
            reader.close();
            
            //paths to look up tables
            path_save_results = path + File.separator + props.getProperty("path_save_results");
            capability_dat_file = path_save_results + File.separator + "capabilities2015-nominal.dat";
            revtimes_dat_file = path_save_results + File.separator + "revtimes.dat";
            dsm_dat_file = path_save_results + File.separator + "all_dsms2014-02-05-22-00-15.dat";
            scores_dat_file = path_save_results  + File.separator + "scores2014-02-05-21-53-09.dat";
            initial_pop = path_save_results + File.separator + "2014-07-07_04-49-24_test.rs";

            // Paths for common xls files
            template_definition_xls = path + File.separator + "xls" + File.separator + props.getProperty("template_definition_xls"); //used
            capability_rules_xls = path + File.separator + "xls" + File.separator + props.getProperty("capability_rules_xls");//used
            requirement_satisfaction_xls = path + File.separator + "xls" + File.separator + props.getProperty("requirement_satisfaction_xls");//used
            aggregation_xls = path + File.separator + "xls" + File.separator +  props.getProperty("aggregation_xls");//used
            
            // Paths for common xml files
            instrument_capability_xml = path + File.separator + "config" + File.separator + props.getProperty("capability_rules_xml");//used
            aggregation_xml = path + File.separator + "config" + File.separator +  props.getProperty("aggregation_xml");//used
            
            // Paths for common clp files
            module_definition_clp        = path + File.separator + "clp" + File.separator + props.getProperty("module_definition_clp");//used
            template_definition_clp      = path + File.separator + "clp" + File.separator + props.getProperty("template_definition_clp");//used
            functions_clp[0]           = path + File.separator + "clp" + File.separator + props.getProperty("functions_clp0");//used
            functions_clp[1]           = path + File.separator + "clp" + File.separator + props.getProperty("functions_clp1");//used
            assimilation_rules_clp       = path + File.separator + "clp" + File.separator + props.getProperty("assimilation_rules_clp");//used
            aggregation_rules_clp       = path + File.separator + "clp" + File.separator + props.getProperty("aggregation_rules_clp");//used
            fuzzy_aggregation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("fuzzy_aggregation_rules_clp");//used
            attribute_inheritance_clp    = path + File.separator + "clp" + File.separator + props.getProperty("attribute_inheritance_clp");
            orbit_rules_clp              = path + File.separator + "clp" + File.separator + props.getProperty("orbit_rules_clp");
            synergy_rules_clp            = path + File.separator + "clp" + File.separator + props.getProperty("synergy_rules_clp");//Used
            explanation_rules_clp        = path + File.separator + "clp" + File.separator + props.getProperty("explanation_rules_clp");//Used
            fuzzy_attribute_clp          = path + File.separator + "clp" + File.separator + props.getProperty("fuzzy_attribute_clp");
            value_aggregation_clp        = path + File.separator + "clp" + File.separator + props.getProperty("value_aggregation_clp");
            requirement_satisfaction_clp = path + File.separator + "clp" + File.separator + props.getProperty("requirement_satisfaction_clp");
            cost_estimation_rules_clp    = path + File.separator + "clp" + File.separator + props.getProperty("cost_estimation_rules_clp"); //Used
            fuzzy_cost_estimation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("fuzzy_cost_estimation_rules_clp"); //Used
            adhoc_rules_clp              = path + File.separator + "clp" + File.separator + props.getProperty("adhoc_rules_clp");
            if(search_clp.isEmpty())
                search_heuristic_rules_clp   = path + File.separator + "clp" + File.separator + "search_heuristic_rules_smap_improveOrbit.clp";
            else
                search_heuristic_rules_clp   = path + File.separator + "clp" + File.separator + search_clp + ".clp";
        } catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
      
        // Metrics for utility and pareto calculations
        pareto_metrics = new ArrayList();
        pareto_metrics.add( "lifecycle_cost" );
        pareto_metrics.add( "benefit" );
        pareto_metric_types = new ArrayList();
        pareto_metric_types.add( "SIB" );
        pareto_metric_types.add( "LIB" );
        util_metrics = new ArrayList();
        util_metrics.add( "lifecycle_cost" );
        util_metrics.add( "benefit" );
        util_metric_types = new ArrayList();
        util_metric_types.add( "SIB" );
        util_metric_types.add( "LIB" );
        util_metric_weights = new ArrayList();
        util_metric_weights.add( 0.5 );
        util_metric_weights.add( 0.5 );
        prob_accept = 0.8;
        
        // Instruments  & Orbits
        ninstr = instrument_list.length;
        norb = orbit_list.length;
        instrument_indexes = new HashMap();
        orbit_indexes= new HashMap();
        for (int j = 0;j<ninstr;j++)
            instrument_indexes.put(instrument_list[j], j);
        for (int j = 0;j<norb;j++)
            orbit_indexes.put(orbit_list[j], j);
        // Intermediate results
        measurements_to_subobjectives = new HashMap();
        measurements_to_objectives = new HashMap();
        measurements_to_panels = new HashMap();
        objectives = new ArrayList();
        subobjectives = new ArrayList();
        instruments_to_measurements = new HashMap();
        instruments_to_subobjectives = new HashMap();
        instruments_to_objectives = new HashMap();
        instruments_to_panels = new HashMap();
        
        measurements_to_instruments = new HashMap();
        subobjectives_to_instruments = new HashMap();
        objectives_to_instruments = new HashMap();
        panels_to_instruments = new HashMap();
    
        subobjectives_to_measurements = new HashMap();
        objectives_to_measurements = new HashMap();
        panels_to_measurements = new HashMap();
        
        
        //Load specific adhoc parameters from config folder
        instrument_sizes = new HashMap();
        File instrumentFile = new File(path+File.separator+"config"+File.separator+"candidateInstruments.xml"); 
        DocumentBuilder dBuilder;
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            
            Document doc = dBuilder.parse(instrumentFile);
            doc.getDocumentElement().normalize();
            NodeList instrumentNode = doc.getElementsByTagName("instrument");
            for(int i=0; i<instrumentNode.getLength();i++){
                Element inst = (Element)instrumentNode.item(i);
                String instName = inst.getElementsByTagName("name").item(0).getTextContent();
                String instSize = inst.getElementsByTagName("size").item(0).getTextContent();
                instrument_sizes.put(instName,Double.parseDouble(instSize));
            }
            
        } catch (ParserConfigurationException  ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }catch ( SAXException  ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Loads in scenario parameters such as time horizon
        File scenarioFile = new File(path+File.separator+"config"+File.separator+"scenarioParams.xml"); 
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(scenarioFile);
            doc.getDocumentElement().normalize();
            time_horizon = Double.parseDouble(doc.getElementsByTagName("timeHorizon").item(0).getTextContent());
            
        } catch (ParserConfigurationException  ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }catch ( SAXException  ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            if(!run_mode.equalsIgnoreCase("update_revtimes")) {
                FileInputStream fis = new FileInputStream(revtimes_dat_file);
                System.out.println("Loading revisit time look-up table...");
                try{
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    revtimes = (HashMap) ois.readObject();
                    ois.close();
                }catch(IOException ex){
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(!run_mode.equalsIgnoreCase("update_capabilities")) {

                FileInputStream fis2 = new FileInputStream(capability_dat_file);
                System.out.println("Loading precomputed capabiltiy look-up table...");
                try {
                    ObjectInputStream ois2 = new ObjectInputStream(fis2);
                    capabilities = (HashMap) ois2.readObject();
                    ois2.close();
                }catch(IOException ex){
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(!run_mode.equalsIgnoreCase("update_dsms")) {
                FileInputStream fis3 = new FileInputStream(dsm_dat_file);
                System.out.println("Loading dsm_dat ...");
                try  {
                    ObjectInputStream ois3 = new ObjectInputStream(fis3);
                    all_dsms = (HashMap) ois3.readObject();
                    ois3.close();
                }catch(IOException ex){
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(!run_mode.equalsIgnoreCase("update_scores")) {
                FileInputStream fis4 = new FileInputStream(scores_dat_file);
                System.out.println("Loading scores_dat_file ...");
                try {
                    ObjectInputStream ois4 = new ObjectInputStream(fis4);
                    scores = (HashMap) ois4.readObject();
                    subobj_scores = (HashMap) ois4.readObject();
                    ois4.close();
                }catch(IOException ex){
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }catch (ClassNotFoundException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
       }
    public static String getConfiguration() {
        return "requirement_satisfaction_xls = " + requirement_satisfaction_xls + ";capability_rules_xls = " + capability_rules_xls;
    }

    public static String getName() {
        return name;
    }
    
}