package sample.Models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class City {
    //static HashSet that fill by all the cities that searched for and not found
    private static HashSet<String> notFoundCity = new HashSet<String>();
    //static HashMap that fill by all the cities as key and their value is string of country*population+currency
    //that in the rest Countries API
    private static HashMap<String, String> restCountriesAPI = new HashMap<>();
    private String city;
    private String country;
    private String currency;
    private String population;
    private LinkedList<String> docsRepresent;

    /*
    static block that fill the restCountriesAPI HashMap by values from the city1.txt file
    "city1.txt" - is file we made from rest Countries API
    */
    static {
        String inputLine;
        String path = "src/main/resources/city1.txt";
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            StringBuilder x = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                x.append(inputLine);
            in.close();
            x.deleteCharAt(0);
            while (!x.toString().isEmpty() && x.toString().contains("@")) {
                restCountriesAPI.put(x.substring(0, x.toString().indexOf(":")).toUpperCase(), x.substring(x.toString().indexOf(":") + 1, x.toString().indexOf("@")));
                x.delete(0, x.indexOf("@") + 1);
            }
            restCountriesAPI.put(x.substring(0, x.toString().indexOf(":")), x.substring(x.toString().indexOf(":") + 1, x.toString().length()));
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public City(String city, String initDocId) {
        this.city = city;
        docsRepresent = new LinkedList<>();
        docsRepresent.addFirst(initDocId);
        infoByCity(city);
    }

    public City(String city, String country, String currency, String population, LinkedList<String> docsRepresent) {
        this.city = city;
        this.country = country;
        this.currency = currency;
        this.population = population;
        this.docsRepresent = docsRepresent;
    }

    /**
     * add a new DOC ID that represent by the city
     * @param id is the document id
     */
    void addDocId(String id) {
        this.docsRepresent.addLast(id);
    }

    public String getCity() {
        return city;
    }

    LinkedList<String> getDocsRepresent() {
        return docsRepresent;
    }

    String getCountry() {
        return country;
    }

    String getCurrency() {
        return currency;
    }

    String getPopulation() {
        return population;
    }

    /**
     * fill the current city info by search in restCountriesAPI HashMap and geobytes API
     *
     * @param city .
     */
    private void infoByCity(String city) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder country = new StringBuilder();
        StringBuilder currency = new StringBuilder();
        StringBuilder population = new StringBuilder();
        String inputLine;
        if (restCountriesAPI.containsKey(city.toUpperCase())) {
            String cityInfo = restCountriesAPI.get(city.toUpperCase());
            this.country = cityInfo.substring(cityInfo.indexOf(":") + 1, cityInfo.indexOf("*"));
            this.population = population(cityInfo.substring(cityInfo.indexOf("*") + 1, cityInfo.indexOf("+")));
            this.currency = cityInfo.substring(cityInfo.indexOf("+") + 1, cityInfo.length());
            //if the city is not in the notFoundCity HashSet search in the geobytes API
        } else if (!notFoundCity.contains(city.toUpperCase())) {
            try {
                URL oracle1 = new URL("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + city);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(oracle1.openStream()));
                while ((inputLine = in.readLine()) != null)
                    stringBuilder.append(inputLine);
                in.close();
                stringBuilder.delete(0, stringBuilder.toString().indexOf("country"));
                country.append(stringBuilder.toString().substring(10, stringBuilder.indexOf(",") - 1));
                if (country.toString().isEmpty()) {
                    stringBuilder.delete(0, stringBuilder.length());
                    notFoundCity.add(city.toUpperCase()); //idanW
                    this.country = "";
                } else {
                    stringBuilder.delete(0, stringBuilder.toString().indexOf("population"));
                    population.append(population(stringBuilder.toString().substring(13, stringBuilder.indexOf(",") - 1)));
                    stringBuilder.delete(0, stringBuilder.toString().indexOf("currencycode"));
                    currency.append(stringBuilder.toString().substring(15, stringBuilder.indexOf(",") - 1));
                }
                this.currency = currency.toString();
                this.country = country.toString();
                this.population = population.toString();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else
            this.country = "";
    }

    /**
     * get the population of country and return the number with 2 numbers after the decimal point and add K/M/B
     * to this number
     *
     * @param number .
     * @return .
     */
    private static String population(String number) {
        String x = number.replaceAll(",", "");
        double num = Double.parseDouble(x);
        if (num < 1000)
            return number;
        if (num < 1000000) {
            num /= 1000;
            num = Math.round(num * 100);
            return num / 100 + "K";
        }
        if (num < 1000000000) {
            num /= 1000000;
            num = Math.round(num * 100);
            return num / 100 + "M";
        } else {
            num /= 1000000000;
            num = Math.round(num * 100);
            return num / 100 + "B";
        }
    }

    @Override
    public String toString() {
        return  city + " : { " +
                "country=" + country +
                ", currency=" + currency +
                ", population=" + population +
                ", docsRepresent=" + docsRepresent.toString() +
                '}';
    }


}