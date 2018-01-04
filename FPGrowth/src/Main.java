import java.io.*;
import java.util.*;

class Item {
    String item_id;
    int count;

    Item(String id, int c) {
        item_id = id;
        count = c;
    }

    @Override
    public String toString() {
        return "(id : " + item_id + ", count : " + count + ")";
    }
}

class Transaction {
    int txn_id;
    ArrayList<Item> items;

    Transaction(int id, ArrayList<Item> items) {
        this.txn_id = id;
        this.items = items;
    }

    @Override
    public String toString() {
        String str = "txn_id : " + txn_id + " items : ";
        for (Item i : items) {
            str += i.toString() + " ";
        }
        return str;
    }
}

class FPTree {
    FPTreeNode root;
    LinkedHashMap<String, FPTreeNode> similar_item_link;
    LinkedHashMap<String, Integer> similar_item_count;


    FPTree() {
        root = new FPTreeNode("-1");
        similar_item_link = new LinkedHashMap<>();
        similar_item_count = new LinkedHashMap<>();
    }

    void insertNodeIntoSimilarLink(String label, FPTreeNode node) {

        if (this.similar_item_link.containsKey(label)) {
            FPTreeNode current_node = this.similar_item_link.get(label);
            while (current_node.sibling_link != null) {
                current_node = current_node.sibling_link;
            }
            current_node.sibling_link = node;

            this.similar_item_count.put(label, this.similar_item_count.get(label) + node.frequency);
        } else {
            this.similar_item_link.put(label, node);
            this.similar_item_count.put(label, node.frequency);
        }
    }

    void incrementFrequencyInSimilarItemCount(String label) {
        if (this.similar_item_count.containsKey(label)) {
            this.similar_item_count.put(label, this.similar_item_count.get(label) + 1);
        }
    }
}


public class Main {

    static Comparator<Item> ItemCmp = new Comparator<Item>() {
        public int compare(Item o1, Item o2) {
            if (o1.count < o2.count) {
                return 1;
            } else if (o1.count > o2.count) {
                return -1;
            }
            return 0;
        }
    };

    static HashMap<Integer, ArrayList<String>> txn_list = new HashMap<>();
    static ArrayList<Transaction> arranged_txn_list = new ArrayList<>();

    static HashMap<String, Integer> item_support_count = new HashMap<>();

    static FPTree tree = new FPTree();

    static Set<String> frequent_itemsets = new HashSet<>();

    static Set<String> association_rules = new HashSet<>();

    public static void main(String[] args) throws IOException {

        int minSup = 50;

        double minConf = 0.8;

        String input_file_name = "inter";

        String output_file_name = "out.txt";

        if(args.length == 4) {
            minSup = Integer.parseInt(args[0]);

            minConf = Double.parseDouble(args[1]);

            input_file_name = args[2];

            output_file_name = args[3];
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output_file_name));

        long start = System.currentTimeMillis();

        makeTxnList(input_file_name);

        arrangeTxnList(minSup);

        create_fp_tree();

        create_conditional_fp_tree(tree, minSup, "",bufferedWriter);

        long mid = System.currentTimeMillis();

        if(minSup > 20) {
            generateRules(minSup, minConf, bufferedWriter);
        }


        // Always close files.
        bufferedWriter.close();

        long end = System.currentTimeMillis();

        System.out.println("Frequent itemsets count : "+frequent_itemsets.size());

        System.out.println("Association rules count : "+association_rules.size());

        System.out.println("Time for frequent itemsets generation :" + (mid - start) + " millis");

        System.out.println("Time for rules generation :" + (end - start) + " millis");



    }

    public static void makeTxnList(String filename) {
        File file = new File(filename);

        Scanner inputStream;

        try {
            inputStream = new Scanner(file);

            while (inputStream.hasNext()) {
                String line = inputStream.nextLine();
                //System.out.println("line : " + line);
                String[] line_split = line.split(" ");
                int txn_id = Integer.parseInt(line_split[0]);
                String item_id = line_split[1];

                if (txn_list.containsKey(txn_id)) {
                    ArrayList<String> item_list = txn_list.get(txn_id);
                    item_list.add(item_id);
                    txn_list.put(txn_id, item_list);
                } else {
                    ArrayList<String> item_list = new ArrayList<>();
                    item_list.add(item_id);
                    txn_list.put(txn_id, item_list);
                }

                if (item_support_count.containsKey(item_id)) {
                    item_support_count.put(item_id, item_support_count.get(item_id) + 1);
                } else {
                    item_support_count.put(item_id, 1);
                }

            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void arrangeTxnList(int minSup) {
        for (Integer txn_id : txn_list.keySet()) {
            ArrayList<Item> arranged_items_list = new ArrayList<>();
            for (String item_id : txn_list.get(txn_id)) {
                if (item_support_count.get(item_id) > minSup) {
                    arranged_items_list.add(new Item(item_id, item_support_count.get(item_id)));
                }
            }

            Collections.sort(arranged_items_list, ItemCmp);
            arranged_txn_list.add(new Transaction(txn_id, arranged_items_list));
        }
    }

    public static void create_fp_tree() {

        for (Transaction txn : arranged_txn_list) {
            FPTreeNode temp_root = tree.root;
            for (Item item : txn.items) {
                FPTreeNode child_node = temp_root.getChildNode(item.item_id);
                if (child_node != null) {
                    temp_root.incrementFrequency(child_node.label);
                    tree.incrementFrequencyInSimilarItemCount(child_node.label);
                    temp_root = child_node;
                } else {
                    FPTreeNode inserted_node = temp_root.insertChild(item.item_id);
                    temp_root = inserted_node;
                    tree.insertNodeIntoSimilarLink(item.item_id, inserted_node);
                }
            }
        }

        ArrayList<Map.Entry<String, Integer>> list = new ArrayList(tree.similar_item_count.entrySet());
        Collections.reverse(list);
        tree.similar_item_count.clear();
        for (Map.Entry<String, Integer> entry : list) {
            tree.similar_item_count.put(entry.getKey(), entry.getValue());

            //releasing the children pointers
            FPTreeNode current_node = tree.similar_item_link.get(entry.getKey());
            while (current_node != null) {
                current_node.children = null;
                current_node = current_node.sibling_link;
            }
        }
    }

    public static void create_conditional_fp_tree(FPTree tree, int minSup, String prefix, BufferedWriter bufferedWriter) throws IOException {

        for (String current_label : tree.similar_item_count.keySet()) {


            if (tree.similar_item_count.get(current_label) >= minSup) {
                if (prefix.equals("")) {
                    System.out.println("Frequent item set Added : " + current_label);
                    frequent_itemsets.add(current_label);
                    if(minSup <= 20) {
                        bufferedWriter.write("{"+current_label+"} | {} | "+minSup+" | -1");
                        bufferedWriter.newLine();
                    }
                } else {
                    System.out.println("Frequent item set Added : " + current_label + "," + prefix );
                    frequent_itemsets.add(current_label + "," + prefix);
                    if(minSup <= 20) {
                        bufferedWriter.write("{"+current_label + "," + prefix+"} | {} | "+minSup+" | -1");
                        bufferedWriter.newLine();
                    }
                    item_support_count.put(current_label + "," + prefix, tree.similar_item_count.get(current_label));
                }


                //System.out.println("Parsing for prefix : " + prefix);

                //1st parse
                FPTree conditinal_fp_tree = new FPTree();
                FPTreeNode current_leaf_node = tree.similar_item_link.get(current_label);
                while (current_leaf_node != null) {

                    FPTreeNode prev_node = null;
                    FPTreeNode current_parent_node = current_leaf_node.parent_node;
                    while (current_parent_node != null && !current_parent_node.label.equals("-1")) {

                        if (current_parent_node.aux_node != null) {
                            current_parent_node.aux_node.frequency = current_parent_node.aux_node.frequency + current_leaf_node.frequency;
                            conditinal_fp_tree.similar_item_count.put(current_parent_node.label, conditinal_fp_tree.similar_item_count.get(current_parent_node.label) + current_leaf_node.frequency);

                        } else {
                            current_parent_node.aux_node = new FPTreeNode(current_parent_node, current_leaf_node.frequency);
                            current_parent_node.aux_node.children = null;
                            conditinal_fp_tree.insertNodeIntoSimilarLink(current_parent_node.aux_node.label, current_parent_node.aux_node);
                        }

                        if (prev_node != null) {
                            prev_node.aux_node.parent_node = current_parent_node.aux_node;
                        }

                        prev_node = current_parent_node;
                        current_parent_node = current_parent_node.parent_node;
                    }
                    current_leaf_node = current_leaf_node.sibling_link;
                }

                //2nd parse

                current_leaf_node = tree.similar_item_link.get(current_label);
                while (current_leaf_node != null) {

                    FPTreeNode current_parent_node = current_leaf_node.parent_node;
                    while (current_parent_node != null && !current_parent_node.label.equals("-1")) {
                        current_parent_node.aux_node = null;
                        current_parent_node = current_parent_node.parent_node;
                    }
                    current_leaf_node = current_leaf_node.sibling_link;
                }



                //pruning
                ArrayList<Map.Entry<String, Integer>> similar_item_list = new ArrayList<>(conditinal_fp_tree.similar_item_count.entrySet());

                for (int i = 0; i < similar_item_list.size(); i++) {
                    if (similar_item_list.get(i).getValue() < minSup) {
                        conditinal_fp_tree.similar_item_count.remove(similar_item_list.get(i).getKey());
                        if (i + 1 < similar_item_list.size()) {
                            FPTreeNode node = conditinal_fp_tree.similar_item_link.get(similar_item_list.get(i + 1).getKey());
                            while (node != null) {
                                if (node.parent_node != null && node.parent_node.label.equals(similar_item_list.get(i).getKey())) {
                                    node.parent_node = node.parent_node.parent_node;
                                }
                                node = node.sibling_link;
                            }
                            conditinal_fp_tree.similar_item_link.remove(similar_item_list.get(i).getKey());
                        } else {
                            conditinal_fp_tree.similar_item_link.remove(similar_item_list.get(i).getKey());
                        }
                    }
                }

                if (prefix.equals("")) {
                    create_conditional_fp_tree(conditinal_fp_tree, minSup, current_label,bufferedWriter);
                } else {
                    create_conditional_fp_tree(conditinal_fp_tree, minSup, current_label + "," + prefix,bufferedWriter);
                }

                conditinal_fp_tree.similar_item_count = null;
                conditinal_fp_tree.similar_item_link = null;
                conditinal_fp_tree = null;
            }
        }
    }

    public static void generateRules(double minSup, double minConf,BufferedWriter bufferedWriter) throws IOException {
        for (String item : frequent_itemsets) {

            rulesPermutator(item_support_count.get(item), item,"",minSup,minConf,0,0, bufferedWriter);
        }
    }

    public static void rulesPermutator(double total_sup,String prefix,String suffix,double minSup, double minConf, int level, int index, BufferedWriter bufferedWriter) throws IOException {

        String[] prefix_split = prefix.split(",");

        if(prefix_split.length == 1){
            return;
        }

        for(int i = index; i < prefix_split.length;i++) {
            String consequent = suffix+","+prefix_split[i];

            if(suffix.equals("")){
                consequent = consequent.substring(1);
            }

            String antecedant = "";

            for(int j = 0; j < i;j++) {
                antecedant += ","+prefix_split[j];
            }

            for(int j = i+1; j < prefix_split.length;j++) {
                antecedant += ","+prefix_split[j];
            }
            if(!antecedant.equals("")) {
                antecedant = antecedant.substring(1);
            }


            double conf = (double)total_sup / (double)item_support_count.get(antecedant);

            if(conf > minConf) {
                association_rules.add(antecedant+" => "+consequent);
                System.out.println(antecedant+" => "+consequent);
                if(minSup > 20) {
                    bufferedWriter.write("{"+antecedant+"} | {"+consequent+"} | "+minSup+" | "+minConf );
                    bufferedWriter.newLine();
                }
                rulesPermutator(total_sup,antecedant,consequent,minSup,minConf, level + 1, i, bufferedWriter);
            }

        }
    }

    public static void printTxnList() {
        for (Transaction txn : arranged_txn_list) {
            System.out.println(txn.toString());
        }
    }

    public static void printSiblings(FPTree tree) {
        for (String label : tree.similar_item_count.keySet()) {
            FPTreeNode current_node = tree.similar_item_link.get(label);
            System.out.print("current label : " + label + "->" + tree.similar_item_count.get(label) + " ");
            while (current_node != null) {
                System.out.print(current_node.toString());
                current_node = current_node.sibling_link;
            }
            System.out.println();
        }
    }

    public static void printFreqItemset() {
        for (String s : frequent_itemsets) {
            System.out.println(s);
        }
        System.out.println("total freq items : "+frequent_itemsets.size());
    }


    public static void printPaths(String label) {
        System.out.println("paths for the label : " + label);
        FPTreeNode current_node = tree.similar_item_link.get(label);
        //System.out.print("current label : "+label+"->"+tree.similar_item_count.get(label)+" ");
        while (current_node != null) {
            System.out.print(current_node.toString());
            FPTreeNode node = current_node.parent_node;
            while (node != null) {
                System.out.print(node.toString());
                node = node.parent_node;
            }
            current_node = current_node.sibling_link;
            System.out.println();
        }
        System.out.println();
    }
}


