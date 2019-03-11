package sample.Models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReadFile {
    private String path;
    //list of all the file path in the corpus
    private ArrayList<String> listOfFilePath;
    //list of all languages of documents in the corpus
    private HashSet<String> allDocsLanguage;
    //list of all cities of documents in the corpus
    private HashMap<String, City> allDocsCity;

    public ArrayList<String> getListOfFilePath() {
        return listOfFilePath;
    }

    public ReadFile(String path) {
        this.path = path;
        this.listOfFilePath = new ArrayList<>();
        this.allDocsLanguage = new HashSet<>();
        this.allDocsCity = new HashMap<>();
        filesForFolder(path);
    }

    public ArrayList<String> getAllDocsLanguage() {
        ArrayList<String> language = new ArrayList<>(allDocsLanguage);
        language.sort(String::compareTo);
        return language;
    }

    public HashMap<String, City> getAllDocsCity() {
        return allDocsCity;
    }

    /**
     * fill the listOfFilePath with all files in the corpus
     *
     * @param path .
     */
    private void filesForFolder(String path)
    {
        final File folder = new File(path);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filesForFolder(fileEntry.getPath());
            } else {
                if (!fileEntry.getAbsolutePath().contains("stop_words.txt"))
                    this.listOfFilePath.add(fileEntry.getAbsolutePath());
            }
        }
    }

    /**
     * read file and splitting it documents
     * also find their ID and the represented languages and cities for each document
     *
     * @param path .
     * @return .
     */
    public HashMap<String, String> read(String path)
    {
        HashMap<String, String> textById = new HashMap<>();
        StringBuilder text = new StringBuilder();
        StringBuilder id = new StringBuilder();
        StringBuilder city = new StringBuilder();
        StringBuilder language = new StringBuilder();
        File file = new File(path);
        try {
            Document document = Jsoup.parse(file, "UTF-8");
            Elements docs = document.getElementsByTag("DOC");
            Iterator<Element> iterator = docs.iterator();
            while (iterator.hasNext()) {
                Element element = iterator.next();

                //Document ID by <docno> tag
                id.append(element.getElementsByTag("DOCNO"));
                id.delete(0, 7);
                id.delete(id.length() - 8, id.length());
                id.replace(0, id.length(), id.toString().trim());

                //Text of the document by <text> tag
                text.append(element.getElementsByTag("TEXT"));
                if (text.toString().contains("<text>"))
                    text.delete(text.indexOf("<text>"), text.indexOf("<text>") + 6);
                if (text.toString().contains("</text>"))
                    text.delete(text.indexOf("</text>"), text.indexOf("</text>") + 7);
                if (text.toString().toLowerCase().contains("[text]"))
                    text.delete(0, text.toString().toLowerCase().indexOf("[text]") + 6);

                //City of the text by <f p=104> tag
                city.append(element.getElementsByTag("F").toString());
                if (city.toString().contains("<f p=\"104\">")) {
                    city.delete(0, city.indexOf("<f p=\"104\">") + 14);
                    if (city.toString().contains("</f>"))
                        city.delete(city.indexOf("</f>"), city.length());
                    while (city.toString().contains("\n"))
                        city.deleteCharAt(city.indexOf("\n"));
                    if (!city.toString().isEmpty() &&
                            (!Character.isUpperCase(city.toString().charAt(0)) || city.toString().charAt(0) == '>' || city.toString().charAt(0) == '<'
                                    || city.toString().charAt(0) == '-' || city.toString().charAt(0) == '['
                                    || city.toString().charAt(0) == ']' || city.toString().charAt(0) == '('
                                    || city.toString().charAt(0) == '\'' || Character.isDigit(city.toString().charAt(0))))
                        city.delete(0, city.length());
                    else {
                        city.delete(city.indexOf(" "), city.length());
                        city.replace(0, city.length(), city.toString().toUpperCase());
                        while (!city.toString().isEmpty() && (city.toString().contains("/") || city.length() < 3 || Character.isDigit(city.charAt(city.length() - 1)) || city.charAt(city.length() - 1) == ',' || city.charAt(city.length() - 1) == '-'))// || city.charAt(city.length()-1)==';' || || city.charAt(city.length()-1)=='3')
                            city.deleteCharAt(city.length() - 1);
                        if (!city.toString().isEmpty() && city.charAt(city.length() - 1) == ']')
                            city.delete(0, city.length());
                    }
                } else city.delete(0, city.length());

                //Language of the text by <f p=105> tag
                language.append(element.getElementsByTag("F").toString());
                if (language.toString().contains("<f p=\"105\">")) {
                    language.delete(0, language.indexOf("<f p=\"105\">") + 14);
                    if (language.toString().contains("</f>"))
                        language.delete(language.indexOf("</f>"), language.length());
                    while (language.toString().contains("\n"))
                        language.deleteCharAt(language.indexOf("\n"));
                    if (!language.toString().isEmpty() &&
                            (!Character.isUpperCase(language.toString().charAt(0)) || language.toString().charAt(0) == '>' || language.toString().charAt(0) == '<'
                                    || language.toString().charAt(0) == '-' || language.toString().charAt(0) == '['
                                    || language.toString().charAt(0) == ']' || language.toString().charAt(0) == '('
                                    || language.toString().charAt(0) == '\'' || Character.isDigit(language.toString().charAt(0))))
                        language.delete(0, language.length());
                    else {
                        language.delete(language.indexOf(" "), language.length());
                        if (language.charAt(language.length() - 1) == ',' || language.charAt(language.length() - 1) == ';' || language.charAt(language.length() - 1) == '-' || language.charAt(language.length() - 1) == '3')
                            language.deleteCharAt(language.length() - 1);
                    }
                } else language.delete(0, language.length());

                //add language to allDocsLanguage
                if (!language.toString().isEmpty())
                    allDocsLanguage.add(language.toString());
                //add city to allDocsCity and update for cities that founded the document ID they depand on
                if (!city.toString().isEmpty()) {
                    if (!allDocsCity.containsKey(city.toString())) {
                        City city1 = new City(city.toString(), id.toString());
                        if (!city1.getCountry().isEmpty())
                            allDocsCity.put(city.toString(), city1);
                    } else {
                        allDocsCity.get(city.toString()).addDocId(id.toString());
                    }
                }
                textById.put(id.toString(), text.toString());
                text.delete(0, text.length());
                id.delete(0, id.length());
                city.delete(0, city.length());
                language.delete(0, language.length());

            }
        } catch (IOException e) {
            e.getStackTrace();
        }
        return textById;
    }

    /**
     * Write all the cities objects to the disk
     * @param postingPath
     */
    public void writeCitiesToDisk(String postingPath)
    {
        try{
            File progData = new File(postingPath + "/ProgramData");
            if (!progData.exists()) progData.mkdirs();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(progData + "/Cities.txt"), StandardCharsets.UTF_8));
            for (Map.Entry<String, City> entry : allDocsCity.entrySet()) {
                bw.write(entry.getValue().toString());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Write all the cities objects to the disk
     * @param postingPath
     */
    public void writeLanguagesToDisk(String postingPath)
    {
        try{
            File progData = new File(postingPath + "/ProgramData");
            if (!progData.exists()) progData.mkdirs();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(progData + "/Languages.txt"), StandardCharsets.UTF_8));
            HashSet<String> hsLanguages = new HashSet<>(allDocsLanguage);
            for (String s : hsLanguages) {
                bw.write(s);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}