/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

/**
 *
 * @author dani
 */

import rbsa.attributes.EOAttribute;
import rbsa.attributes.AttributeBuilder;
import java.io.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jess.*;
import jxl.*;
import jxl.read.biff.BiffException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rbsa.eoss.local.Params;

public class JessInitializer {
        
    private static JessInitializer instance = null;
    
    private JessInitializer() 
    {
        
    }
    
    public static JessInitializer getInstance()
    {
        if( instance == null ) 
        {
            instance = new JessInitializer();
        }
        return instance;
    }
    
    public void initializeJess( Rete r , QueryBuilder qb, MatlabFunctions m)
    {
        System.out.println("Initializing Jess...");
        try {
            // Create global variable path
            String tmp = Params.path.replaceAll("\\\\", "\\\\\\\\");
            r.eval( "(defglobal ?*app_path* = \"" + tmp + "\")" );
            r.eval("(import rbsa.eoss.*)");
            
            // Load modules
            loadModules( r );
            
            // Load templates
            Workbook templates_xls = Workbook.getWorkbook( new File( Params.template_definition_xls ) );
            loadTemplates( r, templates_xls, Params.template_definition_clp);
            
            // Load functions
            loadFunctions(r,Params.functions_clp);  
            
             // Load instrument database
            Workbook instrument_xls = Workbook.getWorkbook( new File( Params.capability_rules_xls ) );
//            File instFile = new File(Params.capability_rules_xls); 
//            loadInstrumentFacts(r, instFile,"instrument-database-facts","DATABASE::Instrument");
            loadUnorderedDeffacts(r,instrument_xls, "CHARACTERISTICS", "instrument-database-facts","DATABASE::Instrument");
            
            //Load attribute inheritance rules
            loadAttributeInheritanceRules(r, templates_xls, "Attribute Inheritance", Params.attribute_inheritance_clp);
            
            //Load orbit rules;
            loadOrbitRules(r, Params.orbit_rules_clp);
            
            //Load cost estimation rules;
            loadCostEstimationRules(r, new String[]{Params.cost_estimation_rules_clp,Params.fuzzy_cost_estimation_rules_clp});
            
            //Load fuzzy attribute rules
            loadFuzzyAttributeRules(r, templates_xls, "Fuzzy Attributes", "REQUIREMENTS::Measurement");
            
            //Load requirement rules
            Workbook requirements_xls = Workbook.getWorkbook( new File( Params.requirement_satisfaction_xls ) );
            if (Params.req_mode.equalsIgnoreCase("FUZZY-CASES")) {
                loadFuzzyRequirementRules(r, requirements_xls, "Requirement rules");//last parameter is mode: CASES, FUZZY, ATTRIBUTES
            } else if (Params.req_mode.equalsIgnoreCase("CRISP-CASES")) {
                loadRequirementRules(r, requirements_xls, "Requirement rules");//last parameter is mode: CASES, FUZZY, ATTRIBUTES
            } else if (Params.req_mode.equalsIgnoreCase("CRISP-ATTRIBUTES")) {
                loadRequirementRulesAttribsWithContinuousSatisfactionScore(r, requirements_xls, "Attributes", m);//last parameter is mode: CASES, FUZZY, ATTRIBUTES
            } else if (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES")) {
                loadFuzzyRequirementRulesAttribsWithContinousSatisfactionScore(r, requirements_xls, "Attributes", m);
            }
            
            //Load capability rules
            loadCapabilityRules(r,instrument_xls);
            
            //Load synergy rules
            loadSynergyRules(r,Params.synergy_rules_clp);
            
            // Load assimilation rules
            loadAssimilationRules(r,Params.assimilation_rules_clp);
            
            //Ad-hoc rules
            if(!Params.adhoc_rules_clp.isEmpty()) {
                System.out.println("WARNING: Loading ad-hoc rules");
                r.batch(Params.adhoc_rules_clp);
            }
            
            //Load search rules
            loadSearchRules(r,Params.search_heuristic_rules_clp);
            
            // Load explanation rules
            loadExplanationRules(r, Params.explanation_rules_clp);

            //Load aggregation rules
            File aggregationFile = new File(Params.aggregation_xml); 
            loadAggregationRules(r,aggregationFile, new String[]{Params.aggregation_rules_clp,Params.fuzzy_aggregation_rules_clp});
            
            r.reset();
            
            //Create precomputed queries;
            load_precompute_queries(qb);
        } catch (JessException ex) {
            System.err.println( "EXC in InitializerJess ");
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
            }
            catch (IOException ex) {
            System.err.println( "EXC in InitializerJess ");
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
            }
            catch (BiffException ex) {
            System.err.println( "EXC in InitializerJess ");
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
            }
        
            catch (SAXException  ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println( "EXC in InitializerJess ");
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
            }
            catch (ParserConfigurationException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println( "EXC in InitializerJess ");
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
            
        }
    }
    private void load_precompute_queries(QueryBuilder qb) {
        HashMap<String,Fact> db_instruments = new HashMap();
        for (int i = 0;i<Params.ninstr;i++) {
            String instr = Params.instrument_list[i];
            ArrayList<Fact> facts = qb.makeQuery("DATABASE::Instrument (Name " + instr + ")");
            db_instruments.put(instr,facts.get(0));
        }
        qb.addPrecomputed_query("DATABASE::Instrument", db_instruments);
        
    }
    private void loadModules( Rete r )
    {
        try {
            r.batch( Params.module_definition_clp );
        } catch (Exception e) {
            System.out.println( "EXC in loadModules " +e.getMessage() );
        }
    }
    private void loadOrbitRules( Rete r, String clp )
    {
        try {
            r.batch( clp );
        } catch (Exception e) {
            System.out.println( "EXC in loadOrbitRules " +e.getMessage() );
        }
    }
    
    private void loadTemplates( Rete r, Workbook xls, String clp)
    {
        loadMeasurementTemplate(r, xls);
        loadInstrumentTemplate(r, xls);
        loadSimpleTemplate(r, xls, "Mission","MANIFEST::Mission");
        loadSimpleTemplate(r, xls, "Orbit","DATABASE::Orbit");
        loadSimpleTemplate(r, xls, "Launch-vehicle","DATABASE::Launch-vehicle");
        loadTemplatesCLP(r, clp);
    }
    
    private void loadTemplatesCLP( Rete r, String clp) {
        try {
            r.batch(clp);
        }catch(Exception e) {System.out.println("EXC in loadTemplatesCLP " +e.getClass() + " : " + e.getMessage());}
    }
    
    private void loadMeasurementTemplate(Rete r, Workbook xls) {
        try {
            HashMap attribs_to_keys = new HashMap();
            HashMap keys_to_attribs = new HashMap();
            HashMap attribs_to_types = new HashMap();
            HashMap attribSet = new HashMap();
            Params.parameter_list = new ArrayList<String>();
            Sheet meas = xls.getSheet("Measurement");    
            String call = "(deftemplate REQUIREMENTS::Measurement ";
            int nslots = meas.getRows();
            for (int i = 1;i<nslots;i++) {
                Cell[] row = meas.getRow(i);
                String slot_type = row[0].getContents();
                String name = row[1].getContents();
                String str_id = row[2].getContents();
                int id = Integer.parseInt(str_id);
                String type = row[3].getContents();
                
                
                
                attribs_to_keys.put(name,id);
                keys_to_attribs.put(id,name);
                attribs_to_types.put(name,type);
                if (type.equalsIgnoreCase("NL") || type.equalsIgnoreCase("OL")) {
                    String str_num_atts = row[4].getContents();
                    int num_vals = Integer.parseInt(str_num_atts);
                    Hashtable<String, Integer> accepted_values = new Hashtable<String, Integer>();
                    for (int j = 0;j<num_vals;j++) {
                        accepted_values.put(row[j+5].getContents(), new Integer(j));
                    }
                    EOAttribute attrib = AttributeBuilder.make(type,name,"N/A");
                    attrib.acceptedValues = accepted_values;
                    attribSet.put(name, attrib);
                    if(name.equalsIgnoreCase("Parameter")) {
                        Params.parameter_list.addAll(accepted_values.keySet());
                    }
                } else{
                    EOAttribute attrib = AttributeBuilder.make(type,name,"N/A");
                    attribSet.put(name, attrib);
                }
                
                
                call = call.concat(" (" + slot_type + " " + name + ") ");
            }
            GlobalVariables.defineMeasurement(attribs_to_keys,keys_to_attribs,attribs_to_types,attribSet);
            
            call = call.concat(")");
            r.eval(call);         
        } catch (Exception e) {
            System.out.println( "EXC in loadMeasurementTemplate " +e.getMessage() );
        }
    }
    
    private void loadInstrumentTemplate(Rete r, Workbook xls) {
        try {
            HashMap attribs_to_keys = new HashMap();
            HashMap keys_to_attribs = new HashMap();
            HashMap attribs_to_types = new HashMap();
            HashMap attribSet = new HashMap();
            
            Sheet meas = xls.getSheet("Instrument");    
            String call = "(deftemplate CAPABILITIES::Manifested-instrument ";
            String call2 = "(deftemplate DATABASE::Instrument ";
            int nslots = meas.getRows();
            for (int i = 1;i<nslots;i++) {
                Cell[] row = meas.getRow(i);
                String slot_type = row[0].getContents();
                String name = row[1].getContents();
                String str_id = row[2].getContents();
                int id = Integer.parseInt(str_id);
                String type = row[3].getContents();
                
                
                
                attribs_to_keys.put(name,id);
                keys_to_attribs.put(id,name);
                attribs_to_types.put(name,type);
                if (type.equalsIgnoreCase("NL") || type.equalsIgnoreCase("OL")) {
                    String str_num_atts = row[4].getContents();
                    int num_vals = Integer.parseInt(str_num_atts);
                    Hashtable accepted_values = new Hashtable();
                    for (int j = 0;j<num_vals;j++) {
                        accepted_values.put(row[j+5], new Integer(j));
                    }
                    EOAttribute attrib = AttributeBuilder.make(type,name,"N/A");
                    attrib.acceptedValues = accepted_values;
                    attribSet.put(name, attrib);
                } else{
                    EOAttribute attrib = AttributeBuilder.make(type,name,"N/A");
                    attribSet.put(name, attrib);
                }
                
                
                call = call.concat(" (" + slot_type + " " + name + ") ");
                call2 = call2.concat(" (" + slot_type + " " + name + ") ");
            }
            GlobalVariables.defineInstrument(attribs_to_keys,keys_to_attribs,attribs_to_types,attribSet);
            
            call = call.concat(")");
            call2 = call2.concat(")");
            r.eval(call);
            r.eval(call2); 
        } catch (Exception e) {
            System.out.println( "EXC in loadInstrumentTemplate " +e.getMessage() );
        }
    }
     private void loadSimpleTemplate(Rete r, Workbook xls, String sheet, String template_name) {
        try {
         
            Sheet meas = xls.getSheet(sheet);    
            String call = "(deftemplate " + template_name + " ";
            int nslots = meas.getRows();
            for (int i = 1;i<nslots;i++) {
                Cell[] row = meas.getRow(i);
                String slot_type = row[0].getContents();
                String name = row[1].getContents();
                call = call.concat(" (" + slot_type + " " + name + ") ");
            }

            
            call = call.concat(")");
            r.eval(call);
        } catch (Exception e) {
            System.out.println( "EXC in loadSimpleTemplate " +e.getMessage() );
        }
    }
     
     private void loadFunctions(Rete r, String[] clps) {
         try {
             r.addUserfunction(new SameOrBetter());
             r.addUserfunction(new Improve());
             r.addUserfunction(new Worsen());
             
             for (int i = 0;i<clps.length;i++)  {
                 r.batch(clps[i]);
             }    
             r.eval("(deffunction update-objective-variable (?obj ?new-value) \"Update the value of the global variable with the new value only if it is better \" (bind ?obj (max ?obj ?new-value)))");
             r.eval("(deffunction ContainsRegion (?observed-region ?desired-region)  \"Returns true if the observed region i.e. 1st param contains the desired region i.e. 2nd param \" (bind ?tmp1 (eq ?observed-region Global)) (bind ?tmp2 (eq ?desired-region ?observed-region)) (if (or ?tmp1 ?tmp2) then (return TRUE) else (return FALSE)))");
             r.eval("(deffunction ContainsBands (?list-bands ?desired-bands)  \"Returns true if the list of bands contains the desired bands \" (if (subsetp ?desired-bands ?list-bands) then (return TRUE) else (return FALSE)))");

             r.eval("(deffunction numerical-to-fuzzy (?num ?values ?mins ?maxs)"  + 
                "(bind ?ind 1)"  + 
                "(bind ?n (length$ ?values))"  + 
                "(while (<= ?ind ?n)"  + 
                "(if (and (< ?num (nth$ ?ind ?maxs)) (>= ?num (nth$ ?ind ?mins))) then (return (nth$ ?ind ?values))"  + 
                    "else (++ ?ind))))");
             r.eval("(deffunction revisit-time-to-temporal-resolution (?region ?values)"  + 
            "(if (eq ?region Global) then "  + 
                "(return (nth$ 1 ?values))"  + 
            " elif (eq ?region Tropical-regions) then"  +  
                "(return (nth$ 2 ?values))"  + 
            " elif (eq ?region Northern-hemisphere) then"  +  
                "(return (nth$ 3 ?values))"  + 
            " elif (eq ?region Southern-hemisphere) then"  +  
                "(return (nth$ 4 ?values))"  + 
            " elif (eq ?region Cold-regions) then"  +  
                "(return (nth$ 5 ?values))"  + 
            " elif (eq ?region US) then"  +  
                "(return (nth$ 6 ?values))"  + 
            " else (throw new JessException \"revisit-time-to-temporal-resolution: The region of interest is invalid\")" + 
                "))");
        
            r.eval("(deffunction fuzzy-max (?att ?v1 ?v2) "  + 
                "(if (>= (SameOrBetter ?att ?v1 ?v2) 0) then "  + 
                "?v1 else ?v2))");

            r.eval("(deffunction fuzzy-min (?att ?v1 ?v2) "  + 
                "(if (<= (SameOrBetter ?att ?v1 ?v2) 0) then "  + 
                "?v1 else ?v2))");


            r.eval("(deffunction fuzzy-avg (?v1 ?v2) "  + 
                "(if (or (and (eq ?v1 High) (eq ?v2 Low)) (and (eq ?v1 Low) (eq ?v2 High))) then "  + 
                " \"Medium\" "  + 
                " else (fuzzy-min Accuracy ?v1 ?v2)))");

            r.eval("(deffunction member (?elem ?list) "  + 
                "(if (listp ?list) then "  + 
                " (neq (member$ ?elem ?list) FALSE) "  + 
                " else (?list contains ?elem)))");

            r.eval("(deffunction valid-orbit (?typ ?h ?i ?raan) "  + 
                "(bind ?valid TRUE)"  + 
                "(if (and (eq ?typ GEO) (or (neq ?h GEO) (neq ?i 0))) then (bind ?valid FALSE))"  + 
                "(if (and (neq ?typ GEO) (eq ?h GEO)) then (bind ?valid FALSE))"  + 
                "(if (and (eq ?typ SSO) (neq ?i SSO)) then (bind ?valid FALSE))"  + 
                "(if (and (neq ?typ SSO) (eq ?i SSO)) then (bind ?valid FALSE))"  + 
                "(if (and (neq ?typ SSO) (neq ?raan NA)) then (bind ?valid FALSE))"  + 
                "(if (and (eq ?typ SSO) (eq ?raan NA)) then (bind ?valid FALSE))"  + 
                "(if (and (or (eq ?h 1000) (eq ?h 1300)) (neq ?i near-polar)) then (bind ?valid FALSE))"  + 
                "(if (and (< ?h 400) (or (neq ?typ LEO) (eq ?i SSO) (eq ?i near-polar))) then (bind ?valid FALSE))"  + 
                " (return ?valid))");

            r.eval("(deffunction worth-improving-measurement (?meas) "  + 
                "(bind ?worth TRUE)"  + 
                "(bind ?arr (matlabf get_related_suboj ?meas))"  + 
                "(if (eq ?arr nil) then (return FALSE))"  + 
                "(bind ?iter (?arr iterator))"  + 
                "(while (?iter hasNext) "  + 
                "(bind ?subobj (?iter next)) "  + 
                "(if (eq (eval ?subobj) 1) then (bind ?worth FALSE))) "  + 
                "(return ?worth))");

            r.eval("(deffunction meas-group (?p ?gr)"  + 
            "(if (eq (str-compare (sub-string 1 1 ?p) A) 0) then (return FALSE))"  + 
            "(bind ?pos (str-index \" \" ?p)) " + 
            "(bind ?str (sub-string 1 (- ?pos 1) ?p)) " + 
            "(bind ?meas-1 (nth$ 1 (get-meas-group ?str))) " +  
            "(bind ?meas-2 (nth$ 2 (get-meas-group ?str)))"  +  
            "(bind ?meas-3 (nth$ 3 (get-meas-group ?str))) " +  
            "(bind ?gr-1 (nth$ 1 (get-meas-group ?gr))) " +  
            "(bind ?gr-2 (nth$ 2 (get-meas-group ?gr))) " +  
            "(bind ?gr-3 (nth$ 3 (get-meas-group ?gr)))"  +   
            "(if (and (neq (str-compare ?gr-1 ?meas-1) 0) (neq (str-compare ?gr-1 0) 0)) then (return FALSE)) " + 
            "(if (and (neq (str-compare ?gr-2 ?meas-2) 0) (neq (str-compare ?gr-2 0) 0)) then (return FALSE))"  + 
            "(if (and (neq (str-compare ?gr-3 ?meas-3) 0) (neq (str-compare ?gr-3 0) 0)) then (return FALSE)) " + 
                   " (return TRUE))");

        r.eval("(deffunction get-meas-group (?str)"  + 
            "(bind ?pos (str-index . ?str)) " + 
            "(bind ?gr1 (sub-string 1 (- ?pos 1) ?str)) " + 
            "(bind ?new-str (sub-string (+ ?pos 1) (str-length ?str) ?str)) " + 
            "(bind ?pos2 (str-index . ?new-str)) " + 
            "(bind ?gr2 (sub-string 1 (- ?pos2 1) ?new-str)) " + 
            "(bind ?gr3 (sub-string (+ ?pos2 1) (str-length ?new-str) ?new-str)) " + 
            "(return (create$ ?gr1 ?gr2 ?gr3)))");
    
            } catch (Exception e) {
            System.out.println( "EXC in loadFunctions " +e.getMessage() );
        }
     }
    
     private void loadOrderedDeffacts(Rete r, Workbook xls, String sheet, String name, String template) {
         try {
         
            Sheet meas = xls.getSheet(sheet);    
            String call = "(deffacts " + name + " ";
            int nfacts = meas.getRows();
            int nslots = meas.getColumns();
            Cell[] slotNames = meas.getRow(0);
            String[] slot_names = new String[nslots]; 
            for (int i = 0;i<nslots;i++) {
                slot_names[i] = slotNames[i].getContents();
            }
            for (int i = 1;i<nfacts;i++) {
                Cell[] row = meas.getRow(i);
                call = call.concat(" (" + template + " ");  
                for (int j = 0;j < nslots; j++) {
                    String slot_value = row[j].getContents();
                    if (slot_value.matches("\\[(.+)(,(.+))+\\]")) {
                        call = call.concat( " (" + slot_names[j] + " " + createJessList(slot_value) + ") ");
                    } else {
                        call = call.concat( " (" + slot_names[j] + " " + slot_value + ") ");
                    }
                }
                call = call.concat(") ");
            }
            call = call.concat(")");
            r.eval(call);
        } catch (Exception e) {
            System.out.println( "EXC in loadOrderedDeffacts " +e.getMessage() );
        }
     }
     
     private void loadInstrumentFacts(Rete r, File file, String name, String template){
         DocumentBuilder dBuilder;
         
         try {
             dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             Document doc = dBuilder.parse(file);
             doc.getDocumentElement().normalize();
             NodeList instrumentList = doc.getElementsByTagName("instrument");
             
             String call = "(deffacts " + name + " ";
             //loop through instruments
             for(int i=0;i<instrumentList.getLength();i++){
                 Element instrument = (Element)instrumentList.item(i);
                 NodeList instrumentFacts = instrumentList.item(i).getChildNodes();
                 call = call.concat(" (" + template + " ");  
                 //loop through facts of instrument i
                 for(int j=0; j<instrumentFacts.getLength();j++){
                    String slot_name = instrumentFacts.item(j).getNodeName();
                    if(slot_name.startsWith("#")) //carriage returns, tabs and comments in xml file start with #
                        continue;
                     if(slot_name.equalsIgnoreCase("measurements")) //measurement facts loaded in method loadCapabilityRules
                         continue;
                    String slot_value = instrumentFacts.item(j).getTextContent();
                    call = call.concat( " (" + slot_name + " " + slot_value + ") ");
                 }
                 
                call = call.concat(") ");
             }
             
            call = call.concat(")");
            r.eval(call);
        } catch (SAXException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }catch ( IOException  ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }catch ( ParserConfigurationException  ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }catch ( JessException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
     
     private void loadUnorderedDeffacts(Rete r, Workbook xls, String sheet, String name, String template) {
         try{
            Sheet meas = xls.getSheet(sheet);    
            String call = "(deffacts " + name + " ";
            int nfacts = meas.getRows();
            int nslots = meas.getColumns();
           
            for (int i = 1;i<nfacts;i++) {
                Cell[] row = meas.getRow(i);
                
                call = call.concat(" (" + template + " ");  
                for (int j = 0;j < nslots; j++) {
                    String cell_value = row[j].getContents();
                    String[] splitted = cell_value.split(" ");
                    int len = splitted.length;
                    String slot_name = "";
                    String slot_value = ""; 
                    if (len < 2) {
                        System.out.println("EXC in loadUnorderedDeffacts, expected format is slot_name slot_value. Space not found.");
                    }
                    if (len == 2) {
                       slot_name = splitted[0];
                       slot_value = splitted[1]; 
                    } else {
                       slot_name = splitted[0];
                       slot_value = splitted[1]; 
                       for (int kk = 2;kk < len;kk++) {
                           slot_value = slot_value + " " + splitted[kk]; 
                       }
                    }
                    
                    call = call.concat( " (" + slot_name + " " + slot_value + ") ");
                }
                call = call.concat(") ");
            }
            call = call.concat(")");
            r.eval(call);
        } catch (JessException  e) {
            System.out.println( "EXC in loadUnorderedDeffacts " +e.getMessage() );
        }catch (DOMException e) {
            System.out.println( "EXC in loadUnorderedDeffacts " +e.getMessage() );
        }
     }
     private void loadAttributeInheritanceRules(Rete r, Workbook xls, String sheet, String clp) {
         try {
             r.batch(clp);
             Sheet meas = xls.getSheet(sheet);
             
            int nrules = meas.getRows();
           
            for (int i = 1;i<nrules;i++) {
                Cell[] row = meas.getRow(i);
                String template1 = row[0].getContents();
                String copy_slot_type1 = row[1].getContents();
                String copy_slot_name1 = row[2].getContents();
                String matching_slot_type1 = row[3].getContents();
                String matching_slot_name1 = row[4].getContents();
                String template2 = row[5].getContents();
                String matching_slot_name2 = row[6].getContents();
                String copy_slot_name2 = row[7].getContents();
                String module = row[8].getContents();
                String call = "(defrule " + module + "::inherit-" + template1.split("::")[1] + "-" + copy_slot_name1 + "-TO-" + template2.split("::")[1] + " ";
                if (copy_slot_type1.equalsIgnoreCase("slot")) {
                    call = call + " (" + template1 + " (" + copy_slot_name1 + " ?x&~nil) ";
                } else {
                    call = call + " (" + template1 + " (" + copy_slot_name1 + " $?x&:(> (length$ $?x) 0)) ";
                }
                if (matching_slot_type1.equalsIgnoreCase("slot")) {
                    call = call + " (" + matching_slot_name1 + " ?id&~nil)) ";
                } else {
                    call = call + " (" + matching_slot_name1 + " $?id&:(> (length$ $?id) 0))) ";
                }
                call = call + " ?old <- (" + template2 + " ";
                if (matching_slot_type1.equalsIgnoreCase("slot")) {
                    call = call + " (" + matching_slot_name2 + " ?id) ";
                } else {
                    call = call + " (" + matching_slot_name2 + " $?id) ";
                }
                if (copy_slot_type1.equalsIgnoreCase("slot")) {
                    call = call + " (" + copy_slot_name2 + " nil) ";
                } else {
                    call = call + " (" + copy_slot_name2 + " $?x&:(eq (length$ $?x) 0)) ";
                }
                call = call + ") => (modify ?old (" + copy_slot_name2 + " ?x)))";
                r.eval(call);     
            }          
         }catch (Exception e) {
            System.out.println( "EXC in loadAttributeInheritanceRules " +e.getMessage() );
        }
     }
     private void loadFuzzyAttributeRules(Rete r, Workbook xls, String sheet, String template) {
         try {
             Sheet meas = xls.getSheet(sheet);
             
            int nrules = meas.getRows();
           
            for (int i = 1;i<nrules;i++) {
                Cell[] row = meas.getRow(i);
                String att = row[0].getContents();
                String param = row[1].getContents();
                //String unit = row[2].getContents();
                int num_values = Integer.parseInt(row[3].getContents());
                String[] fuzzy_values = new String[num_values];
                String[] mins = new String[num_values];
                String[] means = new String[num_values];
                String[] maxs = new String[num_values];
                String call_values = "(create$ ";
                String call_mins = "(create$ ";
                String call_maxs = "(create$ ";
                for (int j = 1;j<=num_values;j++) {
                    fuzzy_values[j-1] = row[4*j].getContents();
                    call_values = call_values + fuzzy_values[j-1] + " ";
                    mins[j-1] = row[1+4*j].getContents();
                    call_mins = call_mins + mins[j-1] +   " ";
                    means[j-1] = row[2+4*j].getContents();
                    maxs[j-1] = row[3+4*j].getContents();
                    call_maxs = call_maxs + maxs[j-1] + " ";
                }
                call_values = call_values + ")";
                call_mins = call_mins + ")";
                call_maxs = call_maxs + ")";
                

                String call = "(defrule FUZZY::numerical-to-fuzzy-" + att + " "; 
                if (param.equalsIgnoreCase("all")) {
                    call = call + "?m <- (" + template + " (" + att + "# ?num&~nil) (" + att + " nil)) => " ;
                    call = call + "(bind ?value (numerical-to-fuzzy ?num " + call_values + " " + call_mins + " " + call_maxs + " )) (modify ?m (" + att  + " ?value))) ";
                } else {
                    String att2 = att.substring(0, att.length()-1);
                    call = call + "?m <- (" + template + " (Parameter \"" + param + "\") (" + att2 + "# ?num&~nil) (" + att + " nil)) => " ;
                    call = call + "(bind ?value (numerical-to-fuzzy ?num " + call_values + " " + call_mins + " " + call_maxs + " )) (modify ?m (" + att  + " ?value))) ";
                }

                r.eval(call);     
            }          
         }catch (Exception e) {
            System.out.println( "EXC in loadAttributeInheritanceRules " +e.getMessage() );
        }
     }
     
     private void loadRequirementRules(Rete r, Workbook xls, String sheet) {
         try {
             Sheet meas = xls.getSheet(sheet);
             
            int nrules = meas.getRows();
            int nobj = 0;
            int nsubobj = 0;
            String current_obj = "";
            String current_subobj = "";
            String var_name = "";
            for (int i = 1;i<nrules;i++) {
                Cell[] row = meas.getRow(i);
                String obj = row[0].getContents();
                String explan = row[1].getContents();
                if(!obj.equalsIgnoreCase(current_obj)) {
                    nobj++;
                    nsubobj = 0;
                    var_name = "?*obj-" + obj + "*";
                    r.eval("(defglobal " + var_name + " = 0)");
                    current_obj = obj;
                }
                String subobj = row[2].getContents();
                if(!subobj.equalsIgnoreCase(current_subobj)) {
                    nsubobj++;
                    var_name = "?*subobj-" + subobj + "*";
                    r.eval("(defglobal " + var_name + " = 0)");
                    current_subobj = subobj;
                }
                String type = row[5].getContents();
                String value = row[6].getContents();
                String desc = row[7].getContents();
                String param = row[8].getContents();
                
                String tmp = "?*subobj-" + subobj + "*";
                
                if (Params.measurements_to_subobjectives.containsKey(param)) {
                    ArrayList list = (ArrayList) Params.measurements_to_subobjectives.get(param);
                    if(!list.contains(tmp)) {
                        list.add(tmp);
                        Params.measurements_to_subobjectives.put(param,list);
                    }   
                } else {
                    ArrayList list = new ArrayList();
                    list.add(tmp);
                    Params.measurements_to_subobjectives.put(param,list);
                }
                
                if (Params.measurements_to_objectives.containsKey(param)) {
                    ArrayList list = (ArrayList) Params.measurements_to_objectives.get(param);
                    if(!list.contains(obj)) {
                        list.add(obj);
                        Params.measurements_to_objectives.put(param,list);
                    }   
                } else {
                    ArrayList list = new ArrayList();
                    list.add(obj);
                    Params.measurements_to_objectives.put(param,list);
                }
                String pan = obj.substring(0,2);
                if (Params.measurements_to_panels.containsKey(param)) {
                    ArrayList list = (ArrayList) Params.measurements_to_panels.get(param);
                    if(!list.contains(pan)) {
                        list.add(pan);
                        Params.measurements_to_panels.put(param,list);
                    }   
                } else {
                    ArrayList list = new ArrayList();
                    list.add(pan);
                    Params.measurements_to_panels.put(param,list);
                }
                
                String call = "(defrule REQUIREMENTS::subobjective-" + subobj + "-" + type + " " + desc + " (REQUIREMENTS::Measurement (Parameter " + param + ") "; 
                //boolean more_attributes = true;
                int ntests = 0;
                String calls_for_later = "";
                for (int j = 9;j<row.length;j++) {
                    if (row[j].getType().toString().equalsIgnoreCase("Empty")) {
                        break;
                    }
                    String attrib = row[j].getContents();
                   
                    String[] tokens = attrib.split(" ",2);// limit = 2 so that remain contains RegionofInterest Global
                    String header = tokens[0];
                    String remain = tokens[1];
                    if (attrib.equalsIgnoreCase("")) {
                        call = call + " (taken-by ?who))";
                        //more_attributes = false;
                    } else if (header.startsWith("SameOrBetter")) {
                        ntests++;
                        String[] tokens2 = remain.split(" ");
                        String att = tokens2[0];
                        String val = tokens2[1];
                        String new_var_name = "?x" + ntests;
                        String match = att + " " +  new_var_name + "&~nil"; 
                        call = call + "(" + match + ")";
                        calls_for_later = calls_for_later + " (test (>= (SameOrBetter " + att + " " + new_var_name + " " + val + ") 0))";
                    } else if (header.startsWith("ContainsRegion")) {
                        ntests++;
                        String[] tokens2 = remain.split(" ");
                        String att = tokens2[0];
                        String val = tokens2[1];
                        String new_var_name = "?x" + ntests;
                        String match = att + " " +  new_var_name + "&~nil"; 
                        call = call + "(" + match + ")";
                        calls_for_later = calls_for_later + " (test (ContainsRegion " + new_var_name + " " + val + "))";
                    } else if (header.startsWith("ContainsBands")) {
                        ntests++;
                        String new_var_name = "?x" + ntests;
                        String match = " spectral-bands $" +  new_var_name; 
                        call = call + "(" + match + ")";
                        calls_for_later = calls_for_later + " (test (ContainsBands  (create$ " + remain + ") $" + new_var_name + "))";
                    } else {
                        call = call + "(" + attrib + ")";
                    }
                }
                call = call + "(taken-by ?who)) " + calls_for_later + " => ";
                var_name = "?*subobj-" + subobj + "*";
                
                if (type.startsWith("nominal")) {
                    call = call + "(assert (REASONING::fully-satisfied (subobjective " + subobj + ") (parameter " + param + ") (objective \" " + explan + "\") (taken-by ?who)))" ;
                } else {
                    call = call + "(assert (REASONING::partially-satisfied (subobjective " + subobj + ") (parameter " + param + ") (objective \" " + explan + "\") (attribute " + desc + ") (taken-by ?who)))" ;
                }
                call = call + "(bind " + var_name + " (max " + var_name + " " + value + " )))";
                r.eval(call);     
                r.eval("(defglobal ?*num-soundings-per-day* = 0)");
            }
            Params.subobjectives_to_measurements = getInverseHashMap(Params.measurements_to_subobjectives);
            Params.objectives_to_measurements = getInverseHashMap(Params.measurements_to_objectives);
            Params.panels_to_measurements = getInverseHashMap(Params.measurements_to_panels);
         }catch (Exception e) {
            System.out.println( "EXC in loadRequirementRules " +e.getMessage() );
        }
     }
     private void loadRequirementRulesAttribs(Rete r, Workbook xls, String sheet, MatlabFunctions m) {
         try {
             Sheet meas = xls.getSheet(sheet);
             int nlines = meas.getRows();            
             String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
             //String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ ))";
             String lhs = "";
             String rhs = "";
             String rhs2 = " (bind ?list (create$ ";
             String current_subobj = "";
             int nattrib = 0;
             String req_rule = "";
             String attribs = "";
             String param = "";
             String current_param = "";
             HashMap<String,ArrayList<String>> subobj_tests = null;
             Params.requirement_rules = new HashMap();
             for (int i =1;i<nlines;i++) {
                 Cell[] row = meas.getRow(i);
                 String subobj = row[0].getContents();
                 param = row[1].getContents();
                 
                 ArrayList<String> attrib_test = new ArrayList();
                if(!subobj.equalsIgnoreCase(current_subobj)) {
                    
                    if (nattrib > 0) {
                        //finish this requirement rule
                        String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
                        String parent = tokens[0];
                        String index = tokens[1];
                        call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
                        String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
                        req_rule = lhs + rhs0 + rhs + rhs2 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
                        //req_rule = lhs + rhs0 + rhs + " (assert (AGGREGATION::SUBOBJECTIVE (id " + subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
                        //req_rule = req_rule + " (bind ?*subobj-" + subobj + "* (max ?*subobj-" + subobj + "* (*$ ?list))))";
                        //req_rule = req_rule + " (printout t " + current_subobj + " ?list crlf) ";
                        req_rule = req_rule + ")";
                        Params.requirement_rules.put(current_subobj,subobj_tests);
                        Params.subobjectives_to_measurements.put(current_subobj, current_param);
                        r.eval(req_rule);
                        
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        attribs = "";
                        lhs = "(defrule REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        nattrib = 0;
                        subobj_tests = new HashMap();
                    } else {
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        attribs = "";
                        lhs = "(defrule REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        subobj_tests = new HashMap();
                        //nattrib = 0;
                    }
                }
                
                
                String attrib = row[2].getContents();
                attribs = attribs + " " + attrib;
                String type = row[3].getContents();
                String thresholds = row[4].getContents();
                String scores = row[5].getContents();
                String justif = row[6].getContents();
                attrib_test.add(type);
                attrib_test.add(thresholds);
                attrib_test.add(scores);
                subobj_tests.put(attrib, attrib_test);
                nattrib++;
                lhs = lhs + " (" + attrib + " ?val" + nattrib + "&~nil) ";
                rhs = rhs + "(bind ?x" + nattrib + " (nth$ (find-bin-num ?val" + nattrib + " " + m.toJessList(thresholds) + " ) " + m.toJessList(scores) + "))";
                rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                rhs2 = rhs2 + " ?x" + nattrib;
             }
             //Last rule has not been processed
            String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
            String parent = tokens[0];
            String index = tokens[1];
            call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
            String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
            req_rule = lhs + rhs0 + rhs + rhs2 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
            //req_rule = lhs + rhs0 + rhs + " (assert (AGGREGATION::SUBOBJECTIVE (id " + subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
            //req_rule = req_rule + " (bind ?*subobj-" + subobj + "* (max ?*subobj-" + subobj + "* (*$ ?list))))";
            //req_rule = req_rule + " (printout t " + current_subobj + " ?list crlf) ";
            req_rule = req_rule + ")";

            r.eval(req_rule);
            Params.requirement_rules.put(current_subobj,subobj_tests);
            Params.subobjectives_to_measurements.put(current_subobj, current_param);            
            call2= call2 + ")";
            r.eval(call2);
         }catch (Exception e) {
            System.out.println( "EXC in loadRequirementRulesAttribs " +e.getMessage() );
        }
     }
     
     private void loadRequirementRulesAttribsWithContinuousSatisfactionScore(Rete r, Workbook xls, String sheet, MatlabFunctions m) {
         try {
             Sheet meas = xls.getSheet(sheet);
             int nlines = meas.getRows();            
             String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
             //String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ ))";
             String lhs = "";
             String rhs = "";
             String rhs2 = " (bind ?list (create$ ";
             String rhs3 = " (bind ?vals (create$ ";
             String current_subobj = "";
             int nattrib = 0;
             String req_rule = "";
             String attribs = "";
             String param = "";
             String current_param = "";
             HashMap<String,ArrayList<String>> subobj_tests = null;
             Params.requirement_rules = new HashMap();
             
             String linefunc = "(deffunction lineFunc(?x1 ?y1 ?x2 ?y2 ?x3)" //x1 y1, x2 y2, are known points. x3 is what you want to test to get y3
                     + "(bind ?slope (/ (- ?y2 ?y1) (- ?x2 ?x1)))"
                     + "(return (+ (* ?slope (- ?x3 ?x1)) ?y1)))";
             r.eval(linefunc);
             String funcCallSIB = "(deffunction scoringFuncSIB (?val ?thresh ?scores)"
                     + "(bind ?n (find-bin-num ?val ?thresh))"
                     + "(bind ?uplimit (nth$ (length$ ?thresh) ?thresh))"
                     + "(if (eq ?n (length$ ?scores)) then (return (* (nth$ (- ?n 1) ?scores) (** 2.7183 (/ (- ?uplimit ?val) ?uplimit)))))" 
                     + "(if (eq ?n 1) then (return (nth$ 1 ?scores)))"
                     + "(return (lineFunc (nth$ (- ?n 1) ?thresh) (nth$ (- ?n 1) ?scores) (nth$ ?n ?thresh) (nth$ ?n ?scores) ?val)))";
             r.eval(funcCallSIB);
             
             for (int i =1;i<nlines;i++) {
                 Cell[] row = meas.getRow(i);
                 String subobj = row[0].getContents();
                 param = row[1].getContents();
                 
                 ArrayList<String> attrib_test = new ArrayList();
                if(!subobj.equalsIgnoreCase(current_subobj)) {
                    
                    if (nattrib > 0) {
                        //finish this requirement rule
                        String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
                        String parent = tokens[0];
                        String index = tokens[1];
                        call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
                        String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
                        req_rule = lhs + rhs0 + rhs + rhs2 + "))" + rhs3 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (attrib-vals ?vals) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
                        req_rule = req_rule + ")";
                        Params.requirement_rules.put(current_subobj,subobj_tests);
                        Params.subobjectives_to_measurements.put(current_subobj, current_param);
                        r.eval(req_rule);
                        
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        rhs3 = " (bind ?vals (create$ ";
                        attribs = "";
                        lhs = "(defrule REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        nattrib = 0;
                        subobj_tests = new HashMap();
                    } else {
                        //start first requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        rhs3 = " (bind ?vals (create$ ";
                        attribs = "";
                        lhs = "(defrule REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        subobj_tests = new HashMap();
                        //nattrib = 0;
                    }
                }
                
                String attrib = row[2].getContents();
                attribs = attribs + " " + attrib;
                String type = row[3].getContents();
                String thresholds = row[4].getContents();
                String scores = row[5].getContents();
                String justif = row[6].getContents();
                attrib_test.add(type);
                attrib_test.add(thresholds);
                attrib_test.add(scores);
                subobj_tests.put(attrib, attrib_test);
                nattrib++;
                lhs = lhs + " (" + attrib + " ?val" + nattrib + "&~nil) ";
                //Only apply some continuous scoring function to attributes that aren't associated with coverage
                if(type.equalsIgnoreCase("SIB")){
                    rhs = rhs + "(bind ?x" + nattrib + " (scoringFuncSIB "+"?val" + nattrib + " " + m.toJessList(thresholds) + " " + m.toJessList(scores) + "))";
                    rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                }else if(type.equalsIgnoreCase("LIB")){
                    rhs = rhs + "(bind ?x" + nattrib + " (scoringFuncLIB "+"?val" + nattrib + " " + m.toJessList(thresholds) + " " + m.toJessList(scores) + "))";
                    rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                }else{
                    rhs = rhs + "(bind ?x" + nattrib + " (nth$ (find-bin-num ?val" + nattrib + " " + m.toJessList(thresholds) + " ) " + m.toJessList(scores) + "))";
                    rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                }
                rhs2 = rhs2 + " ?x" + nattrib;
                rhs3 = rhs3 + " ?val" + nattrib;
             }
             //Last rule has not been processed
            String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
            String parent = tokens[0];
            String index = tokens[1];
            call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
            String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
            req_rule = lhs + rhs0 + rhs + rhs2 + "))" + rhs3 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (attrib-vals ?vals) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
            req_rule = req_rule + ")";

            r.eval(req_rule);
            Params.requirement_rules.put(current_subobj,subobj_tests);
            Params.subobjectives_to_measurements.put(current_subobj, current_param);            
            call2= call2 + ")";
            r.eval(call2);
         }catch (Exception e) {
            System.out.println( "EXC in loadRequirementRulesAttribs " +e.getMessage() );
        }
     }
     
     private void loadFuzzyRequirementRulesAttribs(Rete r, Workbook xls, String sheet, MatlabFunctions m) {
         try {
             Sheet meas = xls.getSheet(sheet);
             int nlines = meas.getRows();            
             String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
             //String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ ))";
             String lhs = "";
             String rhs = "";
             String rhs2 = " (bind ?list (create$ ";
             String current_subobj = "";
             int nattrib = 0;
             String req_rule = "";
             String attribs = "";
             String param = "";
             String current_param = "";
             HashMap<String,ArrayList<String>> subobj_tests = null;
             Params.requirement_rules = new HashMap();
             
             for (int i =1;i<nlines;i++) {
                 Cell[] row = meas.getRow(i);
                 String subobj = row[0].getContents();
                 param = row[1].getContents();
                 
                 ArrayList<String> attrib_test = new ArrayList();
                if(!subobj.equalsIgnoreCase(current_subobj)) {
                    
                    if (nattrib > 0) {
                        //finish this requirement rule
                        String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
                        String parent = tokens[0];
                        String index = tokens[1];
                        call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value (new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (MatlabFunctions getValue_inv_hashmap))) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
                        String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
                        req_rule = lhs + rhs0 + rhs + rhs2 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) "
                                + "(attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue \"Value\" (call "
                                + "(new FuzzyValue \"Value\" (new Interval \"interval\" (*$ ?list) (*$ ?list)) \"utils\" "
                                + "(MatlabFunctions getValue_hashmap)) getFuzzy_val) \"utils\" (MatlabFunctions getValue_inv_hashmap))) "
                                + " (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
                        req_rule = req_rule + ")";
                        Params.requirement_rules.put(current_subobj,subobj_tests);
                        Params.subobjectives_to_measurements.put(current_subobj, current_param);
                        r.eval(req_rule);
                        
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        attribs = "";
                        lhs = "(defrule FUZZY-REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        nattrib = 0;
                        subobj_tests = new HashMap();
                    } else {
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        attribs = "";
                        lhs = "(defrule FUZZY-REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        subobj_tests = new HashMap();
                        //nattrib = 0;
                    }
                }
                
                
                String attrib = row[2].getContents();
                attribs = attribs + " " + attrib;
                String type = row[3].getContents();
                String thresholds = row[4].getContents();
                String scores = row[5].getContents();
                String justif = row[6].getContents();
                attrib_test.add(type);
                attrib_test.add(thresholds);
                attrib_test.add(scores);
                subobj_tests.put(attrib, attrib_test);
                nattrib++;
                lhs = lhs + " (" + attrib + " ?val" + nattrib + "&~nil) ";
                rhs = rhs + "(bind ?x" + nattrib + " (nth$ (find-bin-num ?val" + nattrib + " " + m.toJessList(thresholds) + " ) " + m.toJessList(scores) + "))";
                rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                    + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                rhs2 = rhs2 + " ?x" + nattrib;
             }
             //Last rule has not been processed
            String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
            String parent = tokens[0];
            String index = tokens[1];
            call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value "
                    + "(new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (MatlabFunctions getValue_inv_hashmap)))"
                    + " (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") "
                    + "(reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
            String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
            req_rule = lhs + rhs0 + rhs + rhs2 + "))(assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) "
                                + "(attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue \"Value\" (call "
                                + "(new FuzzyValue \"Value\" (new Interval \"interval\" (*$ ?list) (*$ ?list)) \"utils\" "
                                + "(MatlabFunctions getValue_hashmap)) getFuzzy_val) \"utils\" (MatlabFunctions getValue_inv_hashmap))) "
                                + " (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
            req_rule = req_rule + ")";

            r.eval(req_rule);
            Params.requirement_rules.put(current_subobj,subobj_tests);
            Params.subobjectives_to_measurements.put(current_subobj, current_param);            
            call2= call2 + ")";
            r.eval(call2);
         }catch (Exception e) {
            System.out.println( "EXC in loadFuzzyRequirementRulesAttribs " +e.getMessage() );
            e.printStackTrace();
        }
     }
     
     private void loadFuzzyRequirementRulesAttribsWithContinousSatisfactionScore(Rete r, Workbook xls, String sheet, MatlabFunctions m) {
         try {
             Sheet meas = xls.getSheet(sheet);
             int nlines = meas.getRows();            
             String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
             //String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ ))";
             String lhs = "";
             String rhs = "";
             String rhs2 = " (bind ?list (create$ ";
             String rhs3 = " (bind ?vals (create$ ";
             String current_subobj = "";
             int nattrib = 0;
             String req_rule = "";
             String attribs = "";
             String param = "";
             String current_param = "";
             HashMap<String,ArrayList<String>> subobj_tests = null;
             Params.requirement_rules = new HashMap();
             
             String linefunc = "(deffunction lineFunc(?x1 ?y1 ?x2 ?y2 ?x3)" //x1 y1, x2 y2, are known points. x3 is what you want to test to get y3
                     + "(bind ?slope (/ (- ?y2 ?y1) (- ?x2 ?x1)))"
                     + "(return (+ (* ?slope (- ?x3 ?x1)) ?y1)))";
             r.eval(linefunc);
             String funcCallSIB = "(deffunction scoringFuncSIB (?val ?thresh ?scores)"
                     + "(bind ?n (find-bin-num ?val ?thresh))"
                     + "(bind ?uplimit (nth$ (length$ ?thresh) ?thresh))"
                     + "(if (eq ?n (length$ ?scores)) then (return (* (nth$ (- ?n 1) ?scores) (** 2.7183 (/ (- ?uplimit ?val) ?uplimit)))))" 
                     + "(if (eq ?n 1) then (return (nth$ 1 ?scores)))"
                     + "(return (lineFunc (nth$ (- ?n 1) ?thresh) (nth$ (- ?n 1) ?scores) (nth$ ?n ?thresh) (nth$ ?n ?scores) ?val)))";
             r.eval(funcCallSIB);
             
             for (int i =1;i<nlines;i++) {
                 Cell[] row = meas.getRow(i);
                 String subobj = row[0].getContents();
                 param = row[1].getContents();
                 
                 ArrayList<String> attrib_test = new ArrayList();
                if(!subobj.equalsIgnoreCase(current_subobj)) {
                    
                    if (nattrib > 0) {
                        //finish this requirement rule
                        String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
                        String parent = tokens[0];
                        String index = tokens[1];
                        call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value (new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (MatlabFunctions getValue_inv_hashmap))) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
                        String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
                        req_rule = lhs + rhs0 + rhs + rhs2 + "))" + rhs3 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) "
                                + "(attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue \"Value\" (call "
                                + "(new FuzzyValue \"Value\" (new Interval \"interval\" (*$ ?list) (*$ ?list)) \"utils\" "
                                + "(MatlabFunctions getValue_hashmap)) getFuzzy_val) \"utils\" (MatlabFunctions getValue_inv_hashmap))) "
                                + " (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )(attrib-vals ?vals))) ";
                        req_rule = req_rule + ")";
                        Params.requirement_rules.put(current_subobj,subobj_tests);
                        Params.subobjectives_to_measurements.put(current_subobj, current_param);
                        r.eval(req_rule);
                        
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        rhs3 = " (bind ?vals (create$ ";
                        attribs = "";
                        lhs = "(defrule FUZZY-REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        nattrib = 0;
                        subobj_tests = new HashMap();
                    } else {
                        //start next requirement rule
                        rhs = "";
                        rhs2 = " (bind ?list (create$ ";
                        rhs3 = " (bind ?vals (create$ ";
                        attribs = "";
                        lhs = "(defrule FUZZY-REQUIREMENTS::"  + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (Parameter " + param + ")";
                        current_subobj = subobj;
                        current_param = param;
                        subobj_tests = new HashMap();
                    }
                }
                
                String attrib = row[2].getContents();
                attribs = attribs + " " + attrib;
                String type = row[3].getContents();
                String thresholds = row[4].getContents();
                String scores = row[5].getContents();
                String justif = row[6].getContents();
                attrib_test.add(type);
                attrib_test.add(thresholds);
                attrib_test.add(scores);
                subobj_tests.put(attrib, attrib_test);
                nattrib++;
                lhs = lhs + " (" + attrib + " ?val" + nattrib + "&~nil) ";
                //Only apply some continuous scoring function to attributes that aren't associated with coverage
                if(type.equalsIgnoreCase("SIB")){
                    rhs = rhs + "(bind ?x" + nattrib + " (scoringFuncSIB "+"?val" + nattrib + " " + m.toJessList(thresholds) + " " + m.toJessList(scores) + "))";
                    rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                }else if(type.equalsIgnoreCase("LIB")){
                    rhs = rhs + "(bind ?x" + nattrib + " (scoringFuncLIB "+"?val" + nattrib + " " + m.toJessList(thresholds) + " " + m.toJessList(scores) + "))";
                    rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                }else{
                    rhs = rhs + "(bind ?x" + nattrib + " (nth$ (find-bin-num ?val" + nattrib + " " + m.toJessList(thresholds) + " ) " + m.toJessList(scores) + "))";
                    rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif 
                        + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
                }
                rhs2 = rhs2 + " ?x" + nattrib;
                rhs3 = rhs3 + " ?val" + nattrib;
             }
             //Last rule has not been processed
            String[] tokens = current_subobj.split("-",2);// limit = 2 so that remain contains RegionofInterest Global
            String parent = tokens[0];
            String index = tokens[1];
            call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value "
                    + "(new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (MatlabFunctions getValue_inv_hashmap)))"
                    + " (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") "
                    + "(reasons (create$ " + StringUtils.repeat("N-A ",nattrib) + " ))) ";
            String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ "  + StringUtils.repeat("N-A ",nattrib) + "))";
            req_rule = lhs + rhs0 + rhs + rhs2 + "))" + rhs3  + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) "
                                + "(attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue \"Value\" (call "
                                + "(new FuzzyValue \"Value\" (new Interval \"interval\" (*$ ?list) (*$ ?list)) \"utils\" "
                                + "(MatlabFunctions getValue_hashmap)) getFuzzy_val) \"utils\" (MatlabFunctions getValue_inv_hashmap))) "
                                + " (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )(attrib-vals ?vals)))";
            req_rule = req_rule + ")";

            r.eval(req_rule);
            Params.requirement_rules.put(current_subobj,subobj_tests);
            Params.subobjectives_to_measurements.put(current_subobj, current_param);            
            call2= call2 + ")";
            r.eval(call2);
         }catch (Exception e) {
            System.out.println( "EXC in loadFuzzyRequirementRulesAttribs " +e.getMessage() );
            e.printStackTrace();
        }
     }
     
     private void loadFuzzyRequirementRules(Rete r, Workbook xls, String sheet) {
         try {
             Sheet meas = xls.getSheet(sheet);
             
            int nrules = meas.getRows();
            int nobj = 0;
            int nsubobj = 0;
            String current_obj = "";
            String current_subobj = "";
            String var_name = "";
            String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
            
            for (int i = 1;i<nrules;i++) {
                Cell[] row = meas.getRow(i);
                String obj = row[0].getContents();
                String explan = row[1].getContents();
                if(!obj.equalsIgnoreCase(current_obj)) {
                    nobj++;
                    nsubobj = 0;
                    var_name = "?*obj-" + obj + "*";
                    r.eval("(defglobal " + var_name + " = 0)");
                    current_obj = obj;
                }
                String subobj = row[2].getContents();
                if(!subobj.equalsIgnoreCase(current_subobj)) {
                    nsubobj++;
                    var_name = "?*subobj-" + subobj + "*";
                    r.eval("(defglobal " + var_name + " = 0)");
                    current_subobj = subobj;
                }
                String type = row[5].getContents();
                String value = row[6].getContents();
                String desc = row[7].getContents();
                String param = row[8].getContents();
                
                String tmp = "?*subobj-" + subobj + "*";
                
                if (Params.measurements_to_subobjectives.containsKey(param)) {
                    ArrayList list = (ArrayList) Params.measurements_to_subobjectives.get(param);
                    if(!list.contains(tmp)) {
                        list.add(tmp);
                        Params.measurements_to_subobjectives.put(param,list);
                    }   
                } else {
                    ArrayList list = new ArrayList();
                    list.add(tmp);
                    Params.measurements_to_subobjectives.put(param,list);
                }
                
                if (Params.measurements_to_objectives.containsKey(param)) {
                    ArrayList list = (ArrayList) Params.measurements_to_objectives.get(param);
                    if(!list.contains(obj)) {
                        list.add(obj);
                        Params.measurements_to_objectives.put(param,list);
                    }   
                } else {
                    ArrayList list = new ArrayList();
                    list.add(obj);
                    Params.measurements_to_objectives.put(param,list);
                }
                String pan = obj.substring(0,2);
                if (Params.measurements_to_panels.containsKey(param)) {
                    ArrayList list = (ArrayList) Params.measurements_to_panels.get(param);
                    if(!list.contains(pan)) {
                        list.add(pan);
                        Params.measurements_to_panels.put(param,list);
                    }   
                } else {
                    ArrayList list = new ArrayList();
                    list.add(pan);
                    Params.measurements_to_panels.put(param,list);
                }
                
                String call = "(defrule FUZZY-REQUIREMENTS::subobjective-" + subobj + "-" + type + " " + desc + " (REQUIREMENTS::Measurement (Parameter " + param + ") "; 
                
                //boolean more_attributes = true;
                int ntests = 0;
                String calls_for_later = "";
                for (int j = 9;j<row.length;j++) {
                    if (row[j].getType().toString().equalsIgnoreCase("Empty")) {
                        break;
                    }
                    String attrib = row[j].getContents();
                   
                    String[] tokens = attrib.split(" ",2);// limit = 2 so that remain contains RegionofInterest Global
                    String header = tokens[0];
                    String remain = tokens[1];
                    if (attrib.equalsIgnoreCase("")) {
                        call = call + " (taken-by ?who))";
                        //more_attributes = false;
                    } else if (header.startsWith("SameOrBetter")) {
                        ntests++;
                        String[] tokens2 = remain.split(" ");
                        String att = tokens2[0];
                        String val = tokens2[1];
                        String new_var_name = "?x" + ntests;
                        String match = att + " " +  new_var_name + "&~nil"; 
                        call = call + "(" + match + ")";
                        calls_for_later = calls_for_later + " (test (>= (SameOrBetter " + att + " " + new_var_name + " " + val + ") 0))";
                    } else if (header.startsWith("ContainsRegion")) {
                        ntests++;
                        String[] tokens2 = remain.split(" ");
                        String att = tokens2[0];
                        String val = tokens2[1];
                        String new_var_name = "?x" + ntests;
                        String match = att + " " +  new_var_name + "&~nil"; 
                        call = call + "(" + match + ")";
                        calls_for_later = calls_for_later + " (test (ContainsRegion " + new_var_name + " " + val + "))";
                    } else if (header.startsWith("ContainsBands")) {
                        ntests++;
                        String new_var_name = "?x" + ntests;
                        String match = " spectral-bands $" +  new_var_name; 
                        call = call + "(" + match + ")";
                        calls_for_later = calls_for_later + " (test (ContainsBands  (create$ " + remain + ") $" + new_var_name + "))";
                    } else {
                        call = call + "(" + attrib + ")";
                    }
                }
                call = call + "(taken-by ?who)) " + calls_for_later + " => ";
                var_name = "?*subobj-" + subobj + "*";
                
                if (type.startsWith("nominal")) {
                    call = call + "(assert (REASONING::fully-satisfied (subobjective " + subobj + ") (parameter " + param + ") (objective \" " + explan + "\") (taken-by ?who)))" ;
                } else {
                    call = call + "(assert (REASONING::partially-satisfied (subobjective " + subobj + ") (parameter " + param + ") (objective \" " + explan + "\") (attribute " + desc + ") (taken-by ?who)))" ;
                }
                //Addition for fuzzy rules
                //tmpp = regexp(subobj,'(?<parent>.+)-(?<index>.+)','names');
                String the_index = "";
                String the_parent = "";
                
                call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value (new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (matlabf get_value_inv_hashmap))) (id " + subobj + ") (index " + 
                        the_index + ") parent " + the_parent + " )) ";
                call = call + "(assert (AGGREGATION::SUBOBJECTIVE (id " + subobj + ") (index " + the_index + " ) (parent " + the_parent + 
                        " ) (fuzzy-value (new FuzzyValue \"Value\" (call (new FuzzyValue \"Value\" (new Interval \"interval\" " + value + 
                        " " + value + ") \"utils\" (matlabf get_value_hashmap)) getFuzzy_val) \"utils\" (matlabf get_value_inv_hashmap))) (satisfaction " + value + ")  (satisfied-by ?who) ))";
                        
                //Back to normal rules
                call = call + " (bind " + var_name + " (max " + var_name + " " + value + " )))";
                r.eval(call);     
                r.eval("(defglobal ?*num-soundings-per-day* = 0)");
            }
            Params.subobjectives_to_measurements = getInverseHashMap(Params.measurements_to_subobjectives);
            Params.objectives_to_measurements = getInverseHashMap(Params.measurements_to_objectives);
            Params.panels_to_measurements = getInverseHashMap(Params.measurements_to_panels);
         }catch (Exception e) {
            System.out.println( "EXC in loadRequirementRules " +e.getMessage() );
        }
     }
     private void loadCapabilityRules(Rete r, Workbook xls) {
         DocumentBuilder dBuilder;
         try {
//             dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//             Document doc = dBuilder.parse(file);
//             doc.getDocumentElement().normalize();
//             NodeList instrumentList = doc.getElementsByTagName("instrument");
//             for(int i=0;i<instrumentList.getLength();i++){
//                 Element instrumentNode = (Element)instrumentList.item(i);
//                 String instrumentName = instrumentNode.getElementsByTagName("name").item(0).getTextContent();
//                 ArrayList meas = new ArrayList();
//                 ArrayList subobj = new ArrayList();
//                 ArrayList obj = new ArrayList();
//                 ArrayList pan = new ArrayList();
//                 String call = "(defrule MANIFEST::" + instrumentName + "-init-can-measure " + "(declare (salience -20)) ?this <- (CAPABILITIES::Manifested-instrument  (Name ?ins&" + instrumentName
//                         +  ") (Id ?id) (flies-in ?miss) (Intent ?int) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il)) " 
//                         + " (not (CAPABILITIES::can-measure (instrument ?ins) (can-take-measurements no))) => " 
//                         + "(assert (CAPABILITIES::can-measure (instrument ?ins) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (data-rate-duty-cycle# nil) (power-duty-cycle# nil)(orbit-RAAN ?raan)"
//                         + "(in-orbit (str-cat ?typ \"-\" ?h \"-\" ?inc \"-\" ?raan)) (can-take-measurements yes) (reason \"by default\"))))";
//                r.eval(call);
//                
//                String call2 = "(defrule CAPABILITIES::" + instrumentName + "-measurements " + "?this <- (CAPABILITIES::Manifested-instrument  (Name ?ins&" + instrumentName
//                         +  ") (Id ?id) (flies-in ?miss) (Intent ?int) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il)) " 
//                         + " (CAPABILITIES::can-measure (instrument ?ins) (can-take-measurements yes) (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p)) => " 
//                         + " (if (and (numberp ?dc-d) (numberp ?dc-d)) then (bind ?*science-multiplier* (min ?dc-d ?dc-p)) else (bind ?*science-multiplier* 1.0)) (assert (CAPABILITIES::resource-limitations (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p))) ";
//                String list_of_measurements = "";
//                
//                NodeList measurementList = ((Element)instrumentNode.getElementsByTagName("measurements").item(0)).getElementsByTagName("measurement");
//                //loop over the number of measuremetns that instrument can take
//                for (int j = 0;j<measurementList.getLength();j++) {
//                    String measurement = measurementList.item(j);
//                    call2 = call2 + "(assert (REQUIREMENTS::Measurement";
//                    
//                    String capability_type = row[0].getContents();//Measurement
//                    if (!capability_type.equalsIgnoreCase("Measurement")) {
//                        System.out.println("check line 1401 in JessInitializer. ATMS maybe causing some issues");
////                        throw new Exception("loadCapabilityRules: Type of capability not recognized (use Measurement)");
//                    }
//                    String att_value_pair = row[1].getContents();
//                    String[] tokens2 = att_value_pair.split(" ",2);
//                    String att = tokens2[0];//Parameter
//                    String val = tokens2[1];//"x.x.x Soil moisture"
//                    meas.add(val);
//                    ArrayList list_subobjs = (ArrayList) Params.measurements_to_subobjectives.get(val);
//                    if (list_subobjs != null) {
//                        Iterator list_subobjs2 = list_subobjs.iterator();
//                        while (list_subobjs2.hasNext()) {
//                            String tmp = (String) list_subobjs2.next();
//                            String subob = tmp.substring(9,tmp.length()-1);
//                            if (!subobj.contains(subob)) {
//                                subobj.add(subob);
//                            }
//                            String[] tokens3 = subob.split("-",2);
//                            String ob = tokens3[0];
//                            if (!obj.contains(ob)) {
//                                obj.add(ob);
//                            }
//                            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^[A-Z]+");
//                            Matcher m = p.matcher(ob);
//                            m.find();
//                            String pa = m.group();
//
//                            if (!pan.contains(pa)) {
//                                pan.add(pa);
//                            }
//                        }
//                    }
//                    for (int k = 1;k<row.length;k++) {
//                        String att_value_pair2 = row[k].getContents();
//                        call2 = call2 + " (" + att_value_pair2 + ") ";
//                    }
//                    call2 = call2 + "(taken-by " + instrumentName +  ") (flies-in ?miss) (orbit-altitude# ?h) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Id " + instrumentName + j + ") (Instrument " + instrumentName + "))) ";
//                    list_of_measurements = list_of_measurements + " " + instrumentName + j + " ";
//                }
//             }
//             
             for (String instrument:Params.instrument_list) {
                 Sheet sh = xls.getSheet(instrument);
                 int nmeasurements = sh.getRows();
                 ArrayList meas = new ArrayList();
                 ArrayList subobj = new ArrayList();
                 ArrayList obj = new ArrayList();
                 ArrayList pan = new ArrayList();
                 String call = "(defrule MANIFEST::" + instrument + "-init-can-measure " + "(declare (salience -20)) ?this <- (CAPABILITIES::Manifested-instrument  (Name ?ins&" + instrument
                         +  ") (Id ?id) (flies-in ?miss) (Intent ?int) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il)) " 
                         + " (not (CAPABILITIES::can-measure (instrument ?ins) (can-take-measurements no))) => " 
                         + "(assert (CAPABILITIES::can-measure (instrument ?ins) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (data-rate-duty-cycle# nil) (power-duty-cycle# nil)(orbit-RAAN ?raan)"
                         + "(in-orbit (str-cat ?typ \"-\" ?h \"-\" ?inc \"-\" ?raan)) (can-take-measurements yes) (reason \"by default\"))))";
                r.eval(call);
                
                String call2 = "(defrule CAPABILITIES::" + instrument + "-measurements " + "?this <- (CAPABILITIES::Manifested-instrument  (Name ?ins&" + instrument
                         +  ") (Id ?id) (flies-in ?miss) (Intent ?int) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il)) " 
                         + " (CAPABILITIES::can-measure (instrument ?ins) (can-take-measurements yes) (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p)) => " 
                         + " (if (and (numberp ?dc-d) (numberp ?dc-d)) then (bind ?*science-multiplier* (min ?dc-d ?dc-p)) else (bind ?*science-multiplier* 1.0)) (assert (CAPABILITIES::resource-limitations (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p))) ";
                String list_of_measurements = "";
                for (int i = 0;i<nmeasurements;i++) {
                    Cell[] row = sh.getRow(i);
                    call2 = call2 + "(assert (REQUIREMENTS::Measurement";
                    
                    String capability_type = row[0].getContents();//Measurement
                    if (!capability_type.equalsIgnoreCase("Measurement")) {
                        System.out.println("check line 1401 in JessInitializer. ATMS maybe causing some issues");
//                        throw new Exception("loadCapabilityRules: Type of capability not recognized (use Measurement)");
                    }
                    String att_value_pair = row[1].getContents();
                    String[] tokens2 = att_value_pair.split(" ",2);
                    String att = tokens2[0];//Parameter
                    String val = tokens2[1];//"x.x.x Soil moisture"
                    meas.add(val);
                    ArrayList list_subobjs = (ArrayList) Params.measurements_to_subobjectives.get(val);
                    if (list_subobjs != null) {
                        Iterator list_subobjs2 = list_subobjs.iterator();
                        while (list_subobjs2.hasNext()) {
                            String tmp = (String) list_subobjs2.next();
                            String subob = tmp.substring(9,tmp.length()-1);
                            if (!subobj.contains(subob)) {
                                subobj.add(subob);
                            }
                            String[] tokens3 = subob.split("-",2);
                            String ob = tokens3[0];
                            if (!obj.contains(ob)) {
                                obj.add(ob);
                            }
                            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^[A-Z]+");
                            Matcher m = p.matcher(ob);
                            m.find();
                            String pa = m.group();

                            if (!pan.contains(pa)) {
                                pan.add(pa);
                            }
                        }
                    }
                    for (int j = 1;j<row.length;j++) {
                        String att_value_pair2 = row[j].getContents();
                        call2 = call2 + " (" + att_value_pair2 + ") ";
                    }
                    call2 = call2 + "(taken-by " + instrument +  ") (flies-in ?miss) (orbit-altitude# ?h) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Id " + instrument + i + ") (Instrument " + instrument + "))) ";
                    list_of_measurements = list_of_measurements + " " + instrument + i + " ";
                }
                call2 = call2 + "(assert (SYNERGIES::cross-registered (measurements " + list_of_measurements + " ) (degree-of-cross-registration instrument) (platform ?id  )))";
                call2 = call2 + "(modify ?this (measurement-ids " + list_of_measurements + ")))";
                r.eval(call2);
                Params.instruments_to_measurements.put(instrument,meas);
                Params.instruments_to_subobjectives.put(instrument,subobj);
                Params.instruments_to_objectives.put(instrument,obj);
                Params.instruments_to_panels.put(instrument,pan);
             }       
             Params.measurements_to_instruments = getInverseHashMap(Params.instruments_to_measurements);
             Params.subobjectives_to_instruments = getInverseHashMap(Params.instruments_to_measurements);
             Params.objectives_to_instruments = getInverseHashMap(Params.instruments_to_objectives);
             Params.panels_to_instruments = getInverseHashMap(Params.instruments_to_panels);
         }catch (JessException e) {
            System.out.println( "EXC in loadRequirementRules " +e.getMessage() );
        }
     }
     private HashMap getInverseHashMap(HashMap hm) {
         HashMap inverse = new HashMap();
         Iterator es = hm.entrySet().iterator();
         while (es.hasNext()) {
             Map.Entry<String,ArrayList> entr = (Map.Entry<String,ArrayList>) es.next();
             String key = (String) entr.getKey();
             ArrayList vals = (ArrayList) entr.getValue();
             Iterator vals2 = vals.iterator();
             while (vals2.hasNext()) {
                 String val = (String) vals2.next();
                 if (inverse.containsKey(val)) {
                     ArrayList list = (ArrayList) inverse.get(val);
                     if (!list.contains(key)) {
                         list.add(key);
                         inverse.put(val,list);
                     } 
                 } else {
                     ArrayList list = new ArrayList();
                     list.add(key);
                     inverse.put(val,list);
                 }
             }
         }
         return inverse;
     }
     private void loadSynergyRules(Rete r, String clp) {
         try {
            r.batch(clp);
            Iterator it = Params.measurements_to_subobjectives.entrySet().iterator();
            while(it.hasNext())  {
                Map.Entry<String,ArrayList> es = (Map.Entry<String,ArrayList>) it.next();
                String meas = (String) es.getKey();
                ArrayList subobjs2 = (ArrayList) es.getValue(); 
                Iterator subobjs = subobjs2.iterator();
                String call = "(defrule SYNERGIES::stop-improving-" + meas.substring(1, meas.indexOf(" ")) + " ";
                while (subobjs.hasNext())  {
                    String var = (String) subobjs.next();
                    call = call + " (REASONING::fully-satisfied (subobjective " + var + "))";
                }
                call = call + " => (assert (REASONING::stop-improving (Measurement " + meas + "))))";
                r.eval(call);
            }
         } catch (Exception e) {
             System.out.println( "EXC in loadSynergyRules " +e.getMessage() );
         }     
     }
     private void loadAggregationRules (Rete r, File aggregationFile, String[] clps) throws JessException, IOException, SAXException, ParserConfigurationException {
         DocumentBuilder dBuilder;
             for (String clp:clps)
                 r.batch(clp);
             
             //Stakeholders or panels
             dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             Document doc = dBuilder.parse(aggregationFile);
             doc.getDocumentElement().normalize();
             NodeList panelNode = doc.getElementsByTagName("stakeholder");
             Params.npanels = panelNode.getLength();
             String call = "(deffacts AGGREGATION::init-aggregation-facts ";
             Params.panel_names = new ArrayList(Params.npanels);
             Params.panel_weights = new ArrayList(Params.npanels);
             Params.obj_weights = new ArrayList(Params.npanels);
             Params.subobj_weights = new ArrayList(Params.npanels);
             Params.num_objectives_per_panel = new ArrayList(Params.npanels);
             Params.subobj_weights_map = new HashMap();
             
             for(int i=0; i<panelNode.getLength();i++){
                 Element panel = (Element)panelNode.item(i);
                 Params.panel_names.add(panel.getElementsByTagName("id").item(0).getTextContent());
                 Params.panel_weights.add(Weight.parseWeight(panel.getElementsByTagName("weight").item(0).getTextContent()));
             }
             call = call  + " (AGGREGATION::VALUE (sh-scores (repeat$ -1.0 " + Params.npanels + ")) (sh-fuzzy-scores (repeat$ -1.0 " + Params.npanels + ")) (weights " + javaArrayList2JessList(Params.panel_weights) + "))";
             
             //load objectives and subobjective weights
             
             Params.subobjectives = new ArrayList();
             for(int i=0; i<panelNode.getLength();i++){
                 Element panel = (Element)panelNode.item(i);
                 NodeList objNode = panel.getElementsByTagName("objective");
                 ArrayList<Double> obj_weights = new ArrayList();
                 
                 ArrayList<ArrayList> subobj_weights_p = new ArrayList();
                 ArrayList subobj_p = new ArrayList();
                 for(int j=0;j<objNode.getLength();j++){ //cycle through objectives
                     Element obj = (Element)objNode.item(j);
                     obj_weights.add(Weight.parseWeight(obj.getElementsByTagName("weight").item(0).getTextContent()));
                     NodeList subobjNode = obj.getElementsByTagName("subobjective");
                     
                     ArrayList<Double> subobj_weights_o = new ArrayList<Double>();
                     ArrayList subobj_o = new ArrayList();
                     for (int k=0; k<subobjNode.getLength();k++){ //cycle through subobjectives
                         Element subobj = (Element)subobjNode.item(j);
                         String subobj_name = subobj.getElementsByTagName("id").item(0).getTextContent();
                         Double weight = Weight.parseWeight(subobj.getElementsByTagName("weight").item(0).getTextContent());
                         Params.subobj_weights_map.put(subobj_name,weight);
                         
                         subobj_o.add(subobj_name);
                         subobj_weights_o.add(weight);
                     }
                     
                     subobj_weights_p.add(subobj_weights_o);
                     subobj_p.add(subobj_o);
                     call = call + " (AGGREGATION::OBJECTIVE (id " + Params.panel_names.get(i) + (j + 1) + " ) (parent " + Params.panel_names.get(i) + ") (index " + (j + 1)+ " ) (subobj-fuzzy-scores (repeat$ -1.0 " +  subobj_weights_o.size() + ")) (subobj-scores (repeat$ -1.0 " + subobj_weights_o.size() + ")) (weights " +  javaArrayList2JessList(subobj_weights_o) + ")) ";
                 }
                 
                 Params.obj_weights.add(obj_weights);
                 Params.num_objectives_per_panel.add(obj_weights.size());
                 Params.subobj_weights.add(subobj_weights_p);
                 Params.subobjectives.add(subobj_p);
                 call = call + " (AGGREGATION::STAKEHOLDER (id " + Params.panel_names.get(i) + " ) (index " + (i + 1) + " ) (obj-fuzzy-scores (repeat$ -1.0 " +  obj_weights.size() + ")) (obj-scores (repeat$ -1.0 " + obj_weights.size() + ")) (weights " +  javaArrayList2JessList(obj_weights) + ")) ";
             }
             
             call = call + ")";//close deffacts
             r.eval(call);
     }
     private String javaArrayList2JessList(ArrayList list) {
         String call = "(create$";
         for (int i = 0;i<list.size();i++) {
             call = call + " " + list.get(i);
         }
         call = call + ")";
         return call;
     }
     private void loadAssimilationRules (Rete r, String clp) {
         try {
             r.batch(clp);
         } catch(Exception e) {
             System.out.println( "EXC in loadAssimilationRules " +e.getMessage() );
         }  
     }
     private void loadMassBudgetRules (Rete r, String clp) {
         try {
             r.batch(clp);
         } catch(Exception e) {
             System.out.println( "EXC in loadMassBudgetRules " +e.getMessage() );
         }  
     }
     private void loadSpacecraftDesignRules (Rete r, String clp) {
         try {
             r.batch(clp);
         } catch(Exception e) {
             System.out.println( "EXC in loadEpsDesignRules " +e.getMessage() );
         }  
     }
     private void loadCostEstimationRules (Rete r, String[] clps) {
         try {
             for (String clp:clps)
                r.batch(clp);
         } catch(Exception e) {
             System.out.println( "EXC in loadCostEstimationRules " +e.getMessage() );
         }  
     }
     private void loadLaunchVehicleSelectionRules (Rete r, String clp) {
         try {
             r.batch(clp);
         } catch(Exception e) {
             System.out.println( "EXC in loadLaunchVehicleSelectionRules " +e.getMessage() );
         }  
     }
     private void loadSearchRules (Rete r, String clp) {
         try {
             r.batch(clp);
             r.reset();
             r.setFocus("DATABASE");
             r.run();
         } catch(Exception e) {
             System.out.println( "EXC in loadSearchRules " +e.getMessage() );
         }
         
         //get rules in SEARCH
 /*        Iterator it = r.listDefrules();
         ArrayList<String> improve_rules = new ArrayList<String>();
         while(it.hasNext()) {
             HasLHS rule = (HasLHS) it.next();
             String name = rule.getName();
             if(!name.contains("improve"))
                 continue;
             jess.Pattern p = (jess.Pattern)rule.getConditionalElements();
             improve_rules.add(p.toString());
         }*/
     }
     
     private void loadDownSelectionRules (Rete r, String clp) {
         try {
             r.batch(clp);
         } catch(Exception e) {
             System.out.println( "EXC in loadDownSelectionRules " +e.getMessage() );
         }  
     }
     private void loadExplanationRules (Rete r, String clp) {
         try {
             r.batch(clp);
            String call = "(defquery REQUIREMENTS::search-all-measurements-by-parameter \"Finds all measurements of this parameter in the campaign\" " + 
            "(declare (variables ?param)) " +
            "(REQUIREMENTS::Measurement (Parameter ?param) (flies-in ?flies) (launch-date ?ld) (lifetime ?lt) (Instrument ?instr)" +
            " (Temporal-resolution ?tr) (All-weather ?aw) (Horizontal-Spatial-Resolution ?hsr) (Spectral-sampling ?ss)" +
            " (taken-by ?tk) (Vertical-Spatial-Resolution ?vsr) (sensitivity-in-low-troposphere-PBL ?tro) (sensitivity-in-upper-stratosphere ?str)))";
            r.eval(call);
         } catch(Exception e) {
             System.out.println( "EXC in loadExplanationRules " +e.getMessage() );
         }  
     }
     private String createJessList( String str )
    {
        String s = "(create$ ";
        
        str = str.substring(1, str.length()-1);
        String[] list = str.split(",");
        
        for( int i = 0; i < list.length; i++ )
            s = s + list[i] + " ";
        
        return ( s + ")" );
    }
}
