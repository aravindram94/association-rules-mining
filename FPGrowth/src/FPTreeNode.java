import java.util.HashMap;

public class FPTreeNode {
    String label;
    int frequency;
    FPTreeNode sibling_link;
    FPTreeNode parent_node;
    HashMap<String,FPTreeNode> children;
    FPTreeNode aux_node;

    FPTreeNode(String label) {
        this.label = label;
        this.frequency = 1;
        children = new HashMap<>();
        sibling_link = null;
        parent_node = null;
        aux_node = null;
    }

    FPTreeNode(FPTreeNode node, int freq) {
        this.label = node.label;
        this.frequency = freq;
        children = new HashMap<>();
        sibling_link = null;
        parent_node = null;
        aux_node = null;
    }

    FPTreeNode getChildNode(String label) {
        return this.children.get(label);
    }

    void incrementFrequency(String label) {
        FPTreeNode node = children.get(label);
        node.frequency++;
        children.put(label,node);
    }

    FPTreeNode insertChild(String child_label) {
        if(children.containsKey((child_label))) {
            FPTreeNode node = children.get(child_label);
            node.frequency++;
            children.put(child_label,node);
            return node;
        } else {
            FPTreeNode node = new FPTreeNode(child_label);
            node.parent_node = this;
            children.put(child_label,node);
            return node;
        }
    }

    @Override
    public String toString() {
        return "[label : "+this.label+" frequency : "+this.frequency+"]";
    }
}
