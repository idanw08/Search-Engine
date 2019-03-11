package sample.Models;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

public class DictionaryRecord {
    private int df;
    private int ptr;
    private double idf;
    private boolean isCapital;
    private StringProperty term;
    private IntegerProperty totalFreq;

    DictionaryRecord(String term ,int initFreq, boolean isCapital) {
        this.term = new SimpleStringProperty(term);
        this.totalFreq = new SimpleIntegerProperty(initFreq);
        this.df = 1;
        this.isCapital = isCapital;
        ptr = -1;
    }

    public DictionaryRecord(DictionaryRecord other)
    {
        this.term = new SimpleStringProperty(other.term.getValue());
        this.totalFreq = new SimpleIntegerProperty(other.getTotalFreq());
        this.df = other.df;
        this.idf = other.idf;
        this.isCapital = !other.isCapital();
        ptr = -1;
    }

    public DictionaryRecord(String term, int df, int freq)
    {
        this.term = new SimpleStringProperty(term);
        this.df = df;
        this.totalFreq = new SimpleIntegerProperty(freq);
        isCapital = Character.isUpperCase(term.charAt(0));
    }

    DictionaryRecord(String term, int df, int totalFreq, int ptr, double idf)
    {
        this.term = new SimpleStringProperty(term);
        this.df = df;
        this.totalFreq = new SimpleIntegerProperty(totalFreq);
        this.ptr = ptr;
        this.idf = idf;
        isCapital = Character.isUpperCase(term.charAt(0));
    }

    void updateTotalFreq(int val)
    {
        totalFreq.setValue(totalFreq.getValue() + val);
    }

    int getTotalFreq()
    {
        return totalFreq.get();
    }

    void updateDF()
    {
        df++;
    }

    public int getDF()
    {
        return df;
    }

    public boolean isCapital() {
        return isCapital;
    }

    public double getIdf() {
        return idf;
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }

    void setPtr(int _ptr) {
        this.ptr = _ptr;
    }

    int getPtr() {
        return ptr;
    }
    // get Property items for the dictionary table
    public StringProperty getTermProperty()
    {
        return term;
    }

    public ObservableValue<Integer> getTotalFreqProperty()
    {
        return new ReadOnlyObjectWrapper<>(getTotalFreq());
    }
}
