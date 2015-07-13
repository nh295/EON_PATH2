/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rbsa.eoss;

/**
 * Class for parsing fractional weights used in aggregation rules
 * @author Nozomi
 */
public class Weight {
    /**
     * This methods parses a string into a double. It can handle fractions as well 
     * @param str
     * @return 
     */
    public static double parseWeight(String str){
        double out;
        try{
           out = Double.parseDouble(str);
        }catch(NumberFormatException e){
            String[] parts = str.split("/");
            out = Double.parseDouble(parts[0])/Double.parseDouble(parts[1]);
        }
        return out;
    }
}
