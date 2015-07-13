/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

/**
 *
 * @author Marc
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import rbsa.eoss.local.Params;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.HashSet;

public class ArchitectureGenerator {

    private static ArchitectureGenerator instance = null;
    private ArrayList<Architecture> population;
    private Random rnd;
    
    private ArchitectureGenerator() {
        rnd = new Random();
    }

    public static ArchitectureGenerator getInstance() {
        if (instance == null) {
            instance = new ArchitectureGenerator();
        }
        return instance;
    }

    public ArrayList<Architecture> generatePrecomputedPopulation() {
        long NUM_ARCHS = Params.norb * Math.round(Math.pow(2, Params.ninstr) - 1);
        population = new ArrayList((int) NUM_ARCHS);
        try {
            for (int ns=0;ns<Params.nsats.length;ns++){
                for (int o = 0; o < Params.norb; o++) {
                    for (int ii = 1; ii < Math.round(Math.pow(2, Params.ninstr)); ii++) {
                        boolean[][] mat = new boolean[Params.norb][Params.ninstr];
                        boolean[] bin = de2bi(ii,Params.ninstr);
                        for (int i = 0; i < Params.norb; i++) {
                            if (o == i) {
                                for (int j = 0; j < Params.ninstr; j++) {
                                    if (bin[j]) {
                                        mat[i][j] = true;
                                    } else {
                                        mat[i][j] = false;
                                    }
                                }
                            } else {
                                for (int j = 0; j < Params.ninstr; j++) {
                                    mat[i][j] = false;
                                }
                            }   
                        }
                        population.add(new Architecture(mat,Params.nsats[ns]));
                    }
                }
        }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return (ArrayList<Architecture>) population;
    }
    public static boolean[] de2bi(int d, int N) {
        boolean[] b = new boolean[N];
        for (int i = 0;i<N;i++) {
            //double q = (double)d/2;
            int r = d%2;
            if (r == 1) {
                b[i] = true;
            } else {
                b[i] = false;
            }
            d = d/2;
        }
        return b;
    }
    public ArrayList<Architecture> getInitialPopulation (int NUM_ARCHS) {
        if (Params.initial_pop.isEmpty())
            return generateRandomPopulation(NUM_ARCHS);
        else
            return ResultManager.getInstance().loadResultCollectionFromFile(Params.initial_pop).getPopulation();
    }
    
    public ArrayList<Architecture> generateRandomPopulation(int NUM_ARCHS) {
        //int NUM_ARCHS = 100;
        int GENOME_LENGTH = Params.ninstr * Params.norb;
        population = new ArrayList(NUM_ARCHS);
        Random rnd = new Random();
        try {
            for (int i = 0; i < NUM_ARCHS; i++) {
                boolean[] x = new boolean[GENOME_LENGTH];
                Architecture new_one = null;
                do {
                    for (int j = 0; j < GENOME_LENGTH; j++) {
                        x[j] = rnd.nextBoolean();
                    }
                    new_one = new Architecture(x, Params.norb, Params.ninstr,1+rnd.nextInt(Params.nsats.length-1));
                    
                } while(!new_one.checkConstraints());
                population.add(new_one);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return (ArrayList<Architecture>) population;
    }

    public ArrayList<Architecture> localSearch(ArrayList<Architecture> pop0) {
        //Remove duplicates
        
        ArrayList<Architecture> pop1 = new ArrayList<Architecture>(); 
        for (Architecture arch1:pop0) {
            boolean UNIQUE = true;
            for (Architecture arch2:pop0) {
                if (arch1.getId() != arch2.getId() && arch1.compareTo(arch2) == 0) {
                    UNIQUE = false;
                    break;
                }
            }
            if (UNIQUE)
                pop1.add(arch1);
        }
        //ArrayList<Architecture> pop1  = new ArrayList<Architecture>(new HashSet<Architecture>(pop0));
        int n1 = pop1.size();
        ArrayList<Architecture> pop2  = new ArrayList<Architecture>();
        int nvars = pop1.get(0).getBitString().length;
        for (Architecture arch:pop1) {
            if (arch.getNsats()>1)
                pop2.add(new Architecture(arch.getBitString(),Params.norb,Params.ninstr,arch.getNsats()-1));
            if (arch.getNsats()<Params.nsats[Params.nsats.length-1])
                pop2.add(new Architecture(arch.getBitString(),Params.norb,Params.ninstr,arch.getNsats()+1));
        }
        for (int k = 0;k<n1;k++) {
            //System.out.println("Searching around arch " + k);
            for (int j = 0;j<nvars;j++) {
                boolean[] arch = pop1.get(k).getBitString().clone();
                if(arch[j]) {
                    arch[j] = false;
                } else {
                    arch[j] = true;
                }
                Architecture new_one = new Architecture(arch,Params.norb,Params.ninstr,pop1.get(k).getNsats());
                if (new_one.checkConstraints())
                    pop2.add(new_one);
            }
        }
        
        //Remove duplicates after local search
        ArrayList<Architecture> pop3 = new ArrayList<Architecture>(); 
        for (Architecture arch1:pop2) {
            boolean UNIQUE = true;
            for (Architecture arch2:pop2) {
                if (arch1.getId() != arch2.getId() && arch1.compareTo(arch2) == 0) {
                    UNIQUE = false;
                    break;
                }
            }
            if (UNIQUE)
                pop3.add(arch1);
        }
        return pop3;
    }
    
    public ArrayList<Architecture> getPopulation() {
        return population;
    }

    //Single architecture constructors
    public Architecture getRandomArch() {
        Architecture new_one = null;
        do {
            boolean[][] mat = new boolean[Params.norb][Params.ninstr];
        for (int i = 0; i < Params.norb; i++) 
            for (int j = 0;j < Params.ninstr;j++) 
                mat[i][j] = rnd.nextBoolean(); 
        new_one = new Architecture(mat,rnd.nextInt(Params.nsats.length));
        } while(!new_one.checkConstraints());
        
        return new_one;
    }
    public Architecture getTestArch() { // This architecture has a science score of 0.02
//        Architecture arch = new Architecture("0000000000000000000000010",5);
        Architecture arch = new Architecture("0000000010000000000000000",5);
        arch.setEval_mode("DEBUG");
        return arch;
    }
    public Architecture getTestArch2() { // This architecture has a science score of 0.02
        return new Architecture(new boolean[]{true,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true},Params.norb,Params.ninstr,5);
    }
    public Architecture getMaxArch() {
//        boolean[][] mat = new boolean[Params.norb][Params.ninstr];
//        for (int i = 0; i < Params.norb; i++) {
//            for (int j = 0;j < Params.ninstr;j++) {
//                mat[i][j] = true;
//            }
//
//        }
//        return new Architecture(mat,Params.nsats[Params.nsats.length-1]);
        return new Architecture("1000001111011110111101111",8);
    }
    public Architecture getMinArch() {
//        boolean[][] mat = new boolean[Params.norb][Params.ninstr];
//        for (int i = 0; i < Params.norb; i++) {
//            for (int j = 0;j < Params.ninstr;j++) {
//                mat[i][j] = false;
//            }
//
//        }
//        mat[1][1] = true;
//        return new Architecture(mat,1);
        return new Architecture("0000001000000000000000000",1);
    }
    public Architecture getUserEnteredArch() { // This architecture has a science score of 0.02
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        
        HashMap<String,String[]> mapping= new HashMap<String,String[]>();
        for (String orb:Params.orbit_list) {    
            try {
                boolean valid = false;
                String input = "";
                while(!valid) {
                    System.out.println("New payload in " + orb + "? ");
                    input = bufferedReader.readLine();
                    String[] instruments = input.split(" ");
                    ArrayList<String> validInstruments = new ArrayList<String>();
                    validInstruments.addAll(Arrays.asList(Params.instrument_list));
                    valid = true;
                    for (int i = 0;i<instruments.length;i++) {
                        String instr= instruments[i];
                        if(instr.equalsIgnoreCase("")) {
                            valid = true;
                            break;
                        }
                        if(!validInstruments.contains(instr)) {
                            valid = false;
                            break;
                        }
                    }
                }    
                mapping.put(orb,input.split(" "));
            } catch (Exception e) {
                System.out.println("EXC in getUserEnteredArch" + e.getMessage() + " " + e.getClass());
                e.printStackTrace();
                return null;
            }           
        }
        System.out.println("Num sats per orbit? " );
        try{
            String tmp = bufferedReader.readLine();
            return new Architecture(mapping,Integer.parseInt(tmp));
        }catch(Exception e){
            System.out.println("EXC in getUserEnteredArch" + e.getMessage() + " " + e.getClass());
            e.printStackTrace();
            return null;
        }
    }
    public void setPopulation(ArrayList<Architecture> population) {
        this.population = population;
    }
}
