package sample.Models;

public class PostingRecord {
    private String docId;
    private int tf;
    private String places;

    PostingRecord(String docId, int tf, String places) {
        this.docId = docId;
        this.tf = tf;
        this.places = places;
    }

    public String getDocId() {
        return docId;
    }

    public int getTf() {
        return tf;
    }

    public void setPlaces(String places)
    {
        this.places = places;
    }

    @Override
    public String toString()
    {
        return "docId=" + docId + ", tf=" + tf + ", positions=" + places;
    }
}
