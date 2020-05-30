package com.pydio.android.client.data.listing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataSet<T extends Serializable> implements Serializable {

    final Object lock = new Object();

    protected ArrayList<T> mHistory;
    protected ArrayList<T> mList;
    protected ArrayList<T> mSelection;
    protected ArrayList<T> mElementInProcess;

    protected T mCurrentElement = null;
    protected DisplaySpecs mDisplaySpecs;
    protected Sorter mSorter;

    protected boolean mReverse = false;


    public synchronized void clearList() {
        list().clear();
    }

    public int length() {
        if (list() == null) return 0;
        return list().size();
    }

    public T get(int i) {
        if (i < 0 || i >= list().size()) {
            return null;
        }
        return list().get(i);
    }

    public int add(T t) {
        int i;
        Sorter sorter = sorter();
        if (sorter != null) {
            for (i = 0; i < length(); i++) {
                T tChild = get(i);
                if (sorter.isBefore(t, tChild)) {
                    break;
                }
            }
            list().add(i, t);
            return i;
        } else {
            if (mReverse) {
                list().add(0, t);
                return 0;
            } else {
                list().add(t);
                return length() - 1;
            }
        }
    }

    public void addAll(List<T> l) {
        for (T t : l) {
            int i;
            Sorter sorter = sorter();
            if (sorter != null) {
                for (i = 0; i < length(); i++) {
                    T tChild = get(i);
                    if (sorter.isBefore(t, tChild)) {
                        break;
                    }
                }
                list().add(i, t);
            } else {
                if (mReverse) {
                    list().add(0, t);
                } else {
                    list().add(t);
                }
            }
        }
    }

    public void removeAt(int i) {
        list().remove(i);
    }

    public void remove(T t) {
        list().remove(t);
    }

    public synchronized void historyPush(T t) {
        if (currentElement() != null) {
            history().add(currentElement());
        }
        setCurrentElement(t);
    }

    public synchronized T historyPop() {
        int size = history().size();
        if (size == 0) {
            setCurrentElement(null);
            return null;
        }

        T t = history().get(size - 1);
        history().remove(size - 1);
        setCurrentElement(t);
        return t;
    }

    public Sorter sorter() {
        return mSorter;
    }

    public void setSorter(Sorter sorter) {
        mSorter = sorter;
    }

    public ArrayList<T> selection() {
        if (mSelection == null) {
            mSelection = new ArrayList<T>();
        }
        return mSelection;
    }

    public ArrayList<T> list() {
        if (mList == null) {
            mList = new ArrayList<T>();
        }
        return mList;
    }

    public ArrayList<T> history() {
        if (mHistory == null) {
            mHistory = new ArrayList<T>();
        }
        return mHistory;
    }


    public void setInProcess(T t) {
        ArrayList<T> list = elementsInProcess();
        if (list().contains(t) && !list.contains(t)) {
            list.add(t);
        }
    }

    public void unSetInProcess(T t) {
        elementsInProcess().remove(t);
    }

    public boolean isInProcess(T t) {
        return elementsInProcess().contains(t);
    }

    protected ArrayList<T> elementsInProcess() {
        if (mElementInProcess == null) {
            mElementInProcess = new ArrayList<T>();
        }
        return mElementInProcess;
    }

    public void selectionAdd(T t) {
        selection().add(t);
    }

    public int selectionLength() {
        return selection().size();
    }

    public void selectionRemove(T t) {
        selection().remove(t);
    }

    public void selectionClear() {
        selection().clear();
    }

    public void setCurrentElement(T t) {
        mCurrentElement = t;
    }

    public T currentElement() {
        return mCurrentElement;
    }

    public boolean contains(T t) {
        return list().contains(t);
    }

    public int index(T t) {
        return list().indexOf(t);
    }

    public void setList(ArrayList<T> l) {
        mList = new ArrayList<T>(l);
    }

    public void setHistory(ArrayList<T> l) {
        mHistory = new ArrayList<T>(l);
    }

    public void clear() {
        list().clear();
        selection().clear();
        history().clear();
        setCurrentElement(null);
    }
}
