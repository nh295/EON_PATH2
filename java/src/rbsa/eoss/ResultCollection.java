/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

/**
 *
 * @author Marc
 */
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import rbsa.eoss.local.Params;
import java.util.ArrayList;

public class ResultCollection implements java.io.Serializable {
    
    private static final long serialVersionUID =  -7849211725194194520L;
    private String stamp;
    private String filePath;
    private String name;
    private Stack<Result> results;
    private String conf;
    private ArrayList<Result> front;
    
    public ResultCollection()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss" );
        stamp = dateFormat.format( new Date() );
        name = Params.getName();
        conf = Params.getConfiguration();
        //int ind1 = inputFile.indexOf("\\");
        //int ind2 = inputFile.indexOf(".");
        //String tmp = inputFile.substring(ind1+1, ind2);
        
        filePath = Params.path_save_results + File.separator + stamp + "_" + name + ".rs";
        results = new Stack<Result>();
        front = new ArrayList<Result>();
    }
     public ResultCollection(Stack<Result> results)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss" );
        stamp = dateFormat.format( new Date() );
        name = Params.getName();
        conf = Params.getConfiguration();
        //int ind1 = inputFile.indexOf("\\");
        //int ind2 = inputFile.indexOf(".");
        //String tmp = inputFile.substring(ind1+1, ind2);
        
        filePath = Params.path_save_results + File.separator + stamp + "_" + name + ".rs";
        this.results = results;
        front = compute_pareto_front(results);
    }
    private ArrayList<Result> compute_pareto_front(Stack<Result> stack) {
        ArrayList<Result> thefront = new ArrayList<Result>();
        for (int i = 0;i<stack.size();i++) {
            Result r1 = stack.get(i);
            boolean dominated = false;
            for (int j = 0;j<stack.size();j++) {
                if(r1.dominates(stack.get(j))==-1) {
                    dominated = true;
                    break;//dominated
                }
            }
            if(!dominated) {
                thefront.add(r1);
            }
        }
        return thefront;
    } 
    public ResultCollection( String stamp, Stack<Result> results )
    {
        this.stamp = stamp;
        this.results = results;
        front = compute_pareto_front(results);
    }

    public String getStamp() {
        return stamp;
    }

    public void setStamp(String stamp) {
        this.stamp = stamp;
    }

    public Stack<Result> getResults() {
        return results;
    }

    public void setResults(Stack<Result> results) {
        this.results = results;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String inputFile) {
        this.name = inputFile;
    }

    public ArrayList<Result> getFront() {
        return front;
    }

    public ArrayList<Architecture> getPopulation() {
        ArrayList<Architecture> pop = new ArrayList<Architecture>();
        for (Result res:front)
            pop.add(res.getArch());
        return pop;
    }
    public void setFront(ArrayList<Result> front) {
        this.front = front;
    }

   
    
    public void pushResult( Result result ) {
        results.push( result );
    }
    
    public Result popResult() {
        return results.pop();
    }
    
    public Result peekResult() {
        return results.peek();
    }
    
    public void clearResults()
    {
        results.clear();
    }
    
    public boolean isEmpty()
    {
        return results.isEmpty();
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }
    
}

