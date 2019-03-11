package sample.Models;

class CityDocument extends Document {
    private City city;

    CityDocument (String docId, int max_tf, int unique_words, City city)
    {
        super(docId, max_tf, unique_words);
        this.city = city;
    }

    public City getCity() {
        return city;
    }
}
