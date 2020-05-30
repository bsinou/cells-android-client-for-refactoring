package com.pydio.android.client.data.listing;

import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.WorkspaceNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NodeDataSet implements Serializable {

    private final Object lock = new Object();
    private Sorter sorter;
    private DisplaySpecs displaySpecs;
    private ArrayList<Node> list;

    private static String[] EXCLUDED_ACCESS_TYPES = {
            Pydio.WORKSPACE_ACCESS_TYPE_CONF,
            Pydio.WORKSPACE_ACCESS_TYPE_SHARED,
            Pydio.WORKSPACE_ACCESS_TYPE_MYSQL,
            Pydio.WORKSPACE_ACCESS_TYPE_IMAP,
            Pydio.WORKSPACE_ACCESS_TYPE_JSAPI,
            Pydio.WORKSPACE_ACCESS_TYPE_USER,
            Pydio.WORKSPACE_ACCESS_TYPE_HOME,
            Pydio.WORKSPACE_ACCESS_TYPE_HOMEPAGE,
            Pydio.WORKSPACE_ACCESS_TYPE_SETTINGS,
            Pydio.WORKSPACE_ACCESS_TYPE_ADMIN
    };

    public NodeDataSet() {
        this.list = new ArrayList<>();
    }

    private Filter mFilter = node -> node.path().startsWith(".");

    private DisplaySpecs displaySpecs() {
        if (displaySpecs == null) displaySpecs = new DefaultDisplaySpecs();
        return displaySpecs;
    }

    private Sorter sorter() {
        if (sorter == null) sorter = new DefaultSorter();
        return sorter;
    }

    public void setSorter(Sorter sorter) {
        this.sorter = sorter;
    }

    public void removeSorter() {
        this.sorter = null;
    }

    public void setFilter(Filter filter) {
        mFilter = filter;
    }

    public void removeFilter() {
        mFilter = node -> false;
    }

    public Node get(int index) {
        synchronized (lock) {
            return list.get(index);
        }
    }

    public Node findByProperty(String name, String value) {
        synchronized (lock) {
            for (Node node: list) {
                String actualValue = node.getProperty(name);
                if (value.equals(actualValue)) {
                    return node;
                }
            }
            return null;
        }
    }

    public Node removeByProperty(String name, String value) {
        synchronized (lock) {
            Iterator<Node> it = this.list.iterator();
            while(it.hasNext()) {
                Node node = it.next();
                String actualValue = node.getProperty(name);
                if (value.equals(actualValue)) {
                    it.remove();
                    return node;
                }
            }
            return null;
        }
    }

    public int add(Node node) {
        synchronized (lock) {
            if (list.contains(node)) return -1;

            int node_type = node.type();
            if (node_type == Node.TYPE_WORKSPACE && Arrays.asList(EXCLUDED_ACCESS_TYPES).contains(((WorkspaceNode) node).getAccessType())) {
                return -1;
            }

            try {if (mFilter.match(node)) {return -1;}} catch (ClassCastException e) {}

            DisplaySpecs displaySpecs = displaySpecs();
            Node title = null;
            if (displaySpecs != null) {
                title = displaySpecs.update(node);
            }

            if (list.size() == 0) {
                int return_value = 0;
                if (title != null) {
                    list.add(title);
                    return_value = 1;
                }
                list.add(node);
                return return_value;
            }

            int i = 0;
            if (displaySpecs == null || displaySpecs.sectionsCount() == 0) {
                for (; i < list.size(); i++) {
                    Node n = list.get(i);
                    Sorter sorter = sorter();
                    if (sorter != null) {
                        if (sorter.isBefore(node, n)) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                list.add(i, node);
            } else {
                int section_count = 0, total_count = 0;
                while (section_count < displaySpecs.sectionsCount()) {
                    if (displaySpecs.sectionAt(section_count).match(node)) {
                        if (title != null) {
                            list.add(title);
                            list.add(node);
                            return total_count + 1;
                        } else {
                            i = total_count + 1;
                            int size = list.size();
                            while (i < size) {
                                Node n = list.get(i);
                                if (n.type() == Node.TYPE_LOCAL_NODE) break;
                                if (sorter().isBefore(node, n)) break;
                                i++;
                            }
                            list.add(i, node);
                            return i;
                        }
                    } else {
                        total_count += displaySpecs.sectionAt(section_count).items_count + 1;
                    }
                    section_count++;
                }
            }
            return i;
        }
    }

    public void addAll(List<Node> nodes) {
        for (Node node: nodes) {
            add(node);
        }
    }

    public int update(Node old, Node node) {
        synchronized (lock) {
            final int index = index(old.path());
            if (index == -1) {
                return -1;
            }

            list.set(index, node);
            return index;
        }
    }

    public boolean remove(Node node) {
        synchronized (lock) {
            int index = index(node.path());
            if (index == -1) {
                return false;
            }

            list.remove(index);
            return true;
        }
    }

    public void clear() {
        synchronized (lock) {
            list.clear();
        }
    }

    public int indexOf(Node node) {
        synchronized (lock) {
            return index(node.path());
        }
    }

    public boolean contains(Node node) {
        synchronized (lock) {
            int index = index(node.path());
            return index > 0;
        }
    }

    public ArrayList<Node> filter(Filter filter) {
        synchronized (lock) {
            ArrayList<Node> result = new ArrayList<>();
            for (Node node: list) {
                if (filter.match(node)) {
                    result.add(node);
                }
            }
            return result;
        }
    }

    public List<Node> copy() {
        synchronized (lock) {
            return new ArrayList<>(list);
        }
    }

    public int size() {
        synchronized (lock) {
            return list.size();
        }
    }

    private int index(String id) {
        for (int i = 0; i < list.size(); i++) {
            Node node = list.get(i);
            if (id.equals(node.path())) {
                return i;
            }
        }
        return -1;
    }

}
