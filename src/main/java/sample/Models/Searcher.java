package sample.Models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Searcher {
    private static int queryID = 0;
    private static HashSet<String> queryIDs = new HashSet<>();
    private Parser parser;
    private Ranker ranker;
    private String pathOfPostingFolder;
    private String resPath;
    private TreeMap<String, DictionaryRecord> dictionary;
    private HashMap<Character, String> postingFiles = new HashMap<>();

    public Searcher(Parser parser, TreeMap<String, DictionaryRecord> dictionary, Ranker ranker, String pathOfPostingFolder, String resPath) {
        this.parser = parser;
        this.ranker = ranker;
        this.dictionary = dictionary;
        this.pathOfPostingFolder = pathOfPostingFolder;
        this.resPath = resPath;
        initPostingFiles(postingFiles);
    }

    /*public void setResPath(String resPath) {
        this.resPath = resPath;
    }*/

    /**
     * Finds and return the relevant documents for query and write the result to the disk
     * @param query .
     * @param cities .
     * @param toSemanticTreatment .
     * @return an arraylist of the relevant documents
     */
    public ArrayList<sample.Models.Document> relevantDocsFromQuery(String query, HashSet<City> cities, boolean toSemanticTreatment) {
        ArrayList<sample.Models.Document> relevantDocs = parseFromQuery(query, cities, toSemanticTreatment);
        //send to write to file function format:"queryNumber + " 0 " + DocNum + " 1 42.38 mt"
        if (!relevantDocs.isEmpty()) {
            String queryNumber = queryNum();
            writeQueryResultToFile(relevantDocs, queryNumber);
        }
        return relevantDocs;
    }

    /**
     * Finds the relevant documents for each query in the query file and write the result to the disk
     * @param file .
     * @param cities .
     * @param toSemanticTreatment .
     */
    public void relevantDocsFromQueryFile(File file, HashSet<City> cities, boolean toSemanticTreatment) {
        TreeMap<String, String> readQueryFile = readQueryFile(file);
        for (String queryNumber : readQueryFile.keySet()) {
            ArrayList<sample.Models.Document> relevantDocs = parseFromQuery(readQueryFile.get(queryNumber), cities, toSemanticTreatment);
            //send to write to file function format:" query + " 0 " + DocNum +" 1 42.38 mt"
            if (!relevantDocs.isEmpty()) {
                writeQueryResultToFile(relevantDocs, queryNumber);
                queryIDs.add(queryNumber);
            }
        }
    }

    /**
     *Finds the relevant documents for the given query
     * @param query .
     * @param cities .
     * @param toSemanticTreatment .
     * @return a sort arraylist containing the top 50 relevant documentsS
     */
    private ArrayList<sample.Models.Document> parseFromQuery(String query, HashSet<City> cities, boolean toSemanticTreatment) {
        HashSet<String> docsByCities = docsByCities(cities);
        // HashMap for all the terms by docs (K- docId, V- term name ,TermData)
        HashMap<String, HashMap<String, TermData>> docsAndTerms = new HashMap<>();
        String querynew = "";
        String desc = "";
        if (query.contains("@")) {
            desc = query.substring(query.indexOf("@") + 1).toLowerCase();
            querynew = query.substring(0, query.indexOf("@")).toLowerCase();
        }else
            querynew = query;

        // return all the Terms of the query, after parse
        HashSet<String> queryTerms = new HashSet<>(parser.Parsing(querynew).keySet());

        String semanticList;
        if (toSemanticTreatment)
            semanticList = semanticTreatment(queryTerms);
        else
            semanticList = "";

        // return all the rest Terms of the query (from the description and semntic treatment), after parse
        HashSet<String> restTerms = new HashSet<>(parser.Parsing(desc + " " + semanticList).keySet());

        // add words with a similar meaning to the terms in the given query
        HashSet<String> allTerms = new HashSet<>(queryTerms);
        if (!restTerms.isEmpty()){
            allTerms.addAll(restTerms);
        }

        for (String term : allTerms) {
            //check if the term is in the dictionary
            if (dictionary.containsKey(term) || dictionary.containsKey(term.toUpperCase()) ||
                    dictionary.containsKey(term.toLowerCase())) {
                String termToAdd = term;
                if (dictionary.containsKey(term.toUpperCase()))
                    termToAdd = term.toUpperCase();
                else if (dictionary.containsKey(term.toLowerCase()))
                    termToAdd = term.toLowerCase();
                //find the pointer for the line in the posting line
                int pointer = dictionary.ceilingEntry(termToAdd).getValue().getPtr();
                //how much lines to read from the posting line
                int df = dictionary.ceilingEntry(termToAdd).getValue().getDF();

                //all the lines of the term from the posting file
                HashSet<String> allLineFromPostingFiles = postingLines(pointer, df, termToAdd);

                if (!allLineFromPostingFiles.isEmpty()) {
                    for (String line : allLineFromPostingFiles) {
                        String docId = line.substring(line.indexOf("| docId=") + 8, line.indexOf(", tf="));

                        //check if the docId is in the Hash of the docsByCity
                        if (docsByCities != null && !docId.isEmpty() && !docsByCities.contains(docId))
                            continue;

                        int tF = Integer.parseInt(line.substring(line.indexOf(", tf=") + 5, line.indexOf(", positions=")));
                        String positions = line.substring(line.indexOf(", positions=") + 12).trim();
                        //add to docsAndTerms
                        if (docsAndTerms.containsKey(docId)) {
                            docsAndTerms.get(docId).put(termToAdd, new TermData(tF, positions));
                        } else {
                            HashMap<String, TermData> hashMap = new HashMap<>();
                            hashMap.put(termToAdd, new TermData(tF, positions));
                            docsAndTerms.put(docId, hashMap);
                        }
                    }
                }
            }
        }
        // return an arraylist containing the ranked documents by descending order of relevancy
        return ranker.rank(queryTerms, restTerms, docsAndTerms);
    }

    /**
     * @return a random query number (id)
     */
    private String queryNum() {
        String queryNum = "" + (Searcher.queryID++);
        while (queryIDs.contains(queryNum))
            queryNum = "" + (Searcher.queryID++);
        queryIDs.add(queryNum);
        return queryNum;
    }

    /**
     * Write the query result file to disk
     * @param toWrite .
     * @param queryNumber .
     */
    private void writeQueryResultToFile(ArrayList<sample.Models.Document> toWrite, String queryNumber) {
        try {
            File query_file = new File(resPath + "\\results.txt");
            if (!query_file.exists()) {
                BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(query_file), StandardCharsets.UTF_8));
                toWrite.forEach((doc) -> {
                    try {
                        br.write(queryNumber + " 0 " + doc.getDoc_id() + " 1 42.38 mt");
                        br.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                br.close();
            } else {
                try (FileWriter fw = new FileWriter(query_file, true);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter out = new PrintWriter(bw)) {
                    for (sample.Models.Document doc : toWrite) {
                        out.println(queryNumber + " 0 " + doc.getDoc_id() + " 1 42.38 mt");
                    }
                    out.close();
                    bw.close();
                    fw.close();
                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the query file and split it to query number and the query
     * @param file .
     * @return .
     */
    private TreeMap<String, String> readQueryFile(File file) {
        TreeMap<String, String> QueryById = new TreeMap<>();
        try {
            Document document = Jsoup.parse(file, "UTF-8");
            Elements docs = document.getElementsByTag("top");
            Iterator<Element> yotam = docs.iterator();
            while (yotam.hasNext()) {
                Element element = yotam.next();

                //Query ID by <num> tag
                String qId = element.getElementsByTag("num").toString();
                qId = qId.substring(qId.indexOf("Number:") + 7, qId.indexOf("<title>"));
                qId = qId.trim();

                //Query by <title> tag
                String queryText = element.getElementsByTag("title").toString();
                queryText = queryText.substring(7, queryText.indexOf("</title>"));
                queryText = queryText.trim();

                //Query description by <desc> tag
                String e = "";
                if (!element.getElementsByTag("desc").toString().isEmpty()) {
                    e = element.getElementsByTag("desc").toString().toLowerCase();
                    if (e.contains("<narr>")) {
                        e = e.substring(e.indexOf("description:") + 12, e.indexOf("<narr>"));
                    } else
                        e = e.substring(e.indexOf("description:") + 12, e.indexOf("</desc>"));

                    if (e.contains("discuss")){
                        e = e.substring(e.indexOf("discuss")+7,e.indexOf("."));
                    }else
                        e = "";
                    e = e.trim();
                }/*
                //Query Narrative by <narr> tag
                String narr = "";
                if (!element.getElementsByTag("narr").toString().isEmpty()) {
                    narr = element.getElementsByTag("narr").toString();
                    if (narr.contains("<narr>")) {
                        narr = narr.substring(e.indexOf("Narrative:") + 10, narr.indexOf("</narr>")).toLowerCase();
                    }
                    if(narr.contains("relevant document")){
                        narr = narr.substring(narr.indexOf("relevant document")+ 17);
                    }else if (narr.contains("discussing")){
                        String[] narrArr = narr.split("discussing");
                        StringBuilder newNarr = new StringBuilder();
                        for (int i = 1 ; i<narrArr.length; i++){
                            if (narrArr[i].contains("not relevant") || narrArr[i].contains("non-relevant") || narrArr[i].contains("not relevant:"))
                                continue;
                            else if (narrArr[i].contains("relevant.")){
                                newNarr.append(narrArr[i].substring(0,narrArr[i].indexOf("relevant.")) + " ");
                            }else if (narrArr[i].contains("relevant:")){
                                newNarr.append(narrArr[i].substring(narrArr[i].indexOf("relevant:")+9) + " ");
                            }
                        }
                        narr = newNarr.toString();
                    }else narr ="";
                    narr.trim();
                }*/
                QueryById.put(qId, queryText + "@" + e /*+ " " + narr*/);
            }
        } catch (IOException e) {
            e.getStackTrace();
        }
        return QueryById;
    }

    /**
     * Return specific lines from the posting file
     * @param firstLine .
     * @param numOfLines .
     * @param wordToSearch .
     * @return the relevant line from the posting file
     */
    private HashSet<String> postingLines(int firstLine, int numOfLines, String wordToSearch) {
        String filePath;
        HashSet<String> allHisLines = new HashSet<>();
        File noStemmerDir = new File(pathOfPostingFolder + "/Posting Files");
        File stemmerDir = new File(pathOfPostingFolder + "/Posting Files_stemmer");

        if (noStemmerDir.exists()) {
            if (Character.isLetter(wordToSearch.charAt(0)))
                filePath = pathOfPostingFolder + "/Posting Files/" + postingFiles.get(wordToSearch.toUpperCase().charAt(0));
            else filePath = pathOfPostingFolder + "/Posting Files/$-9.txt";
        }else if (stemmerDir.exists()){
            if (Character.isLetter(wordToSearch.charAt(0)))
                filePath = pathOfPostingFolder + "/Posting Files_stemmer/" + postingFiles.get(wordToSearch.toUpperCase().charAt(0));
            else filePath = pathOfPostingFolder + "/Posting Files_stemmer/$-9.txt";
        }else return allHisLines;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), "UTF-8"))) {
            for (int i = 0; i < firstLine; i++)
                br.readLine();
            for (int i = 0; i < numOfLines; i++)
                allHisLines.add(br.readLine());
        } catch (IOException e) {
            e.getStackTrace();
        }
        return allHisLines;
    }

    /**
     * Return all the docs that the cities the func get are in their "104" tag or in the text
     * @param cities .
     * @return hashset of all the names of the cities
     */
    private HashSet<String> docsByCities(HashSet<City> cities) {
        if (cities==null || cities.isEmpty()) {
            return null;
        }
        HashSet<String> docs = new HashSet<>();
        for (City c : cities) {
            docs.addAll(c.getDocsRepresent());
            if (dictionary.containsKey(c.getCity())) {
                int pointer = dictionary.ceilingEntry(c.getCity()).getValue().getPtr();
                int df = dictionary.ceilingEntry(c.getCity()).getValue().getDF();
                HashSet<String> allLineFromPostingFiles = postingLines(pointer, df, c.getCity());
                for (String line : allLineFromPostingFiles) {
                    docs.add(line.substring(line.indexOf("| docId=") + 8, line.indexOf(", tf=")));
                }
            }
        }
        return docs;
    }

    /**
     * Find with 'Datamuse' API words with a meaning similar for the given query
     * @param queryWords .
     * @return list of similar words which the API found as relevant
     */
    private String semanticTreatment(HashSet<String> queryWords) {
        if (queryWords.isEmpty()) {
            return null;
        }
        StringBuilder similarWord = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String s : queryWords) {
            query.append(s + "+");
        }
        query.deleteCharAt(query.length() - 1);
        String word;
        int numOfWordToImport = 2 * queryWords.size();
        if (numOfWordToImport > 100)
            numOfWordToImport = 100;
        else if (numOfWordToImport == 0)
            return null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL oracle1 = new URL("https://api.datamuse.com/words?ml=" + query);
            String inputLine;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(oracle1.openStream(), "UTF-8"));
            while ((inputLine = in.readLine()) != null)
                stringBuilder.append(inputLine);
            in.close();
            if (stringBuilder.length() > 10) {
                for (int i = 0; i < numOfWordToImport; i++) {
                    word = stringBuilder.substring(stringBuilder.toString().indexOf("\"word\":\"") + 8, stringBuilder.toString().indexOf("\",\""));
                    similarWord.append(word + " ");
                    stringBuilder.delete(0, stringBuilder.indexOf("},{\"word\":") + 3);
                }
                stringBuilder.delete(0, stringBuilder.length());
            } else
                return null;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return similarWord.toString();
    }

    /**
     * initialize the postingFiles
     */
    private void initPostingFiles(HashMap<Character, String> postingFiles) {
        postingFiles.put('A', "A-B.txt");
        postingFiles.put('B', "A-B.txt");
        postingFiles.put('C', "C-D.txt");
        postingFiles.put('D', "C-D.txt");
        postingFiles.put('E', "E-F.txt");
        postingFiles.put('F', "E-F.txt");
        postingFiles.put('G', "G-H.txt");
        postingFiles.put('H', "G-H.txt");
        postingFiles.put('I', "I-J.txt");
        postingFiles.put('J', "I-J.txt");
        postingFiles.put('K', "K-L.txt");
        postingFiles.put('L', "K-L.txt");
        postingFiles.put('M', "M-N.txt");
        postingFiles.put('N', "M-N.txt");
        postingFiles.put('O', "O-P.txt");
        postingFiles.put('P', "O-P.txt");
        postingFiles.put('Q', "Q-R.txt");
        postingFiles.put('R', "Q-R.txt");
        postingFiles.put('S', "S-T.txt");
        postingFiles.put('T', "S-T.txt");
        postingFiles.put('U', "U-V.txt");
        postingFiles.put('V', "U-V.txt");
        postingFiles.put('W', "W-X.txt");
        postingFiles.put('X', "W-X.txt");
        postingFiles.put('Y', "Y-Z.txt");
        postingFiles.put('Z', "Y-Z.txt");
    }
}