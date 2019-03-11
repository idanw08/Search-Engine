package sample.Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.CheckListView;
import sample.Models.*;

import java.io.*;
import java.net.URL;
import java.util.*;

public class MainWindowController implements Initializable
{
    private boolean toStem, toSemantic;
    private long startTime, endTime;
    private String corpus_path, postings_path, lbl_posting_path, resQueries_path;
    private HashSet<City> hs_citiesSelected;
    private TreeMap<String, DictionaryRecord> loadedDictionary;
    private HashMap<String, City> loadedCities;

    private ReadFile readFile;
    private Indexer indexer;
    private Searcher searcher;
    private Ranker ranker;

    private File queryFile;

    private CheckListView<String> checkListView;

    public MainWindowController() {
        toStem = false; toSemantic = false;

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Image image = new Image(getClass().getResource("/black_white.jpg").toURI().toString());
            imageView_logo.setImage(image);
            imageView_logo.setCache(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert() {
        Alert alert;
        if (corpus_path == null && postings_path==null)
        {
            alert = new Alert(Alert.AlertType.ERROR, "Please Enter Corpus and Posting Paths");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        } else if (corpus_path == null)
        {
            alert = new Alert(Alert.AlertType.ERROR, "Missing Corpus Path");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        } else {
            alert = new Alert(Alert.AlertType.ERROR, "Missing Posting Path");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    private ArrayList<String> restoreLanguages() throws IOException {
        ArrayList<String> language = new ArrayList<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(postings_path + "/ProgramData/Languages.txt"), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty())
                language.add(line);
        }
        br.close();
        language.sort(String::compareTo);
        return language;
    }

    private HashMap<String, Document> restoreDocuments() throws IOException {
        HashMap<String, Document> docSet = new HashMap<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(postings_path + "/ProgramData/Documents.txt"), "UTF-8"));
        String line, docId;
        int max_tf, unique_words, length;
        while ((line = br.readLine()) != null) {
            docId = line.substring(16, line.indexOf(", max_tf="));
            max_tf = Integer.parseInt(line.substring(line.indexOf(", max_tf=")+9, line.indexOf(", unique_words=")));
            unique_words = Integer.parseInt(line.substring(line.indexOf(", unique_words=")+15, line.indexOf(", length=")));
            length = Integer.parseInt(line.substring(line.indexOf(", length=")+9, line.indexOf(", entities=")));

            docSet.put(docId, new Document(docId, max_tf, unique_words, length));
            docSet.get(docId).setEntities( line.substring(line.indexOf(", entities=") + 11, line.lastIndexOf('}')));
        }

        return docSet;
    }

    private void restoreCities() throws IOException {
        loadedCities = new HashMap<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(postings_path + "/ProgramData/Cities.txt"), "UTF-8"));
        String line, city, country, currency, population, docs[];
        LinkedList<String> list;

        while ((line = br.readLine()) != null) {
            city = line.substring(0, line.indexOf(":")-1);
            country = line.substring(line.indexOf(" country=")+9, line.indexOf(", currency="));
            currency = line.substring(line.indexOf(", currency=")+11, line.indexOf(", population="));
            population = line.substring(line.indexOf(", population=")+13, line.indexOf(", docsRepresent="));

            docs = line.substring(line.indexOf(", docsRepresent=")+17, line.length()-2).split(", ");
            list = new LinkedList<>(Arrays.asList(docs));

            loadedCities.put(city, new City(city, country, currency, population, list));
        }
    }

    @FXML
    public VBox vBox_mainWindows;
    public Button btn_corpus_browse;
    public Button btn_postings_browse;
    public Button btn_generate;
    public Button btn_browseQuery;
    public Button btn_cities;
    public Button btn_run;
    public Button btn_resPath;
    public TextArea txt_queryEntry;
    public TextField txt_corpus_path;
    public TextField txt_postings_path;
    public TextField txt_queryPath;
    public CheckBox chbx_stemming;
    public CheckBox chbx_semantic;
    public ImageView imageView_logo;
    public MenuButton m_languages;
    public Label lbl_resPath;
    public Label lbl_status;
    public Label lbl_city;

    /**
     * this method generates the dictionary and posting files.
     */
    public void generateDictionaryAndPosting() {
        if (corpus_path==null || lbl_posting_path==null) {
            showAlert();
            return;
        }

        lbl_status.setText("Parsing and Indexing...");

        Task<Void> tGenerate = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                startTime = System.currentTimeMillis();
                readFile = new ReadFile(corpus_path);
                Parser parser = new Parser(corpus_path+"/stop_words.txt", toStem);
                File postingDir;
                // create the directory
                if (toStem) {
                    postings_path = lbl_posting_path + "/Posting With Stemmer";
                    postingDir = new File(postings_path);
                }
                else {
                    postings_path = lbl_posting_path + "/Posting Without Stemmer";
                    postingDir = new File(postings_path);
                }
                if (!postingDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    postingDir.mkdirs();
                }

                indexer = new Indexer(postings_path);
                indexer.indexCities(readFile.getAllDocsCity());
                HashMap<String,String> files;//will be fill by docid and text of the files
                for (int i = 0 ; i<readFile.getListOfFilePath().size() ; i++){
                    files = readFile.read(readFile.getListOfFilePath().get(i));//filling the docs file by file
                    System.out.println(i);
                    for (String id:files.keySet()) {
                        indexer.processTerms(parser.Parsing(files.get(id)), id);
                    }
                }
                indexer.flushTmpPosting(); //clear all the remaining terms which haven't written to disk
                indexer.mergeTmpPostingFiles(); // build final posting files
                indexer.writeDictionaryToDisk(); // store all the dictionary in disk
                indexer.writeDocumentsToDisk(); // store the documents in disk
                readFile.writeCitiesToDisk(postings_path); // store the cities in disk
                readFile.writeLanguagesToDisk(postings_path); // store the languages in disk
                try { FileUtils.deleteDirectory(new File(postings_path+"/Temporary Postings")); } // delete all temporary files
                catch (IOException e) { e.printStackTrace(); }
                endTime = System.currentTimeMillis();

                System.out.println("Done!");
                return null;
            }
        };

        tGenerate.setOnSucceeded(event -> {
            lbl_status.setText("");
            displaySummary();
        });

        Thread t = new Thread(tGenerate);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Open the file explorer window, Enable the user to choose a directory.
     * @param actionEvent when one of the 'Browse' button is selected.
     */
    public void openDirectoryFileExplorer(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();

        File selectedDir = dc.showDialog(vBox_mainWindows.getScene().getWindow());
        if (actionEvent.getSource().equals(btn_corpus_browse) && selectedDir!=null)
        {
            corpus_path = selectedDir.getPath();
            txt_corpus_path.setText(selectedDir.getAbsolutePath());
            txt_corpus_path.setDisable(true);
        }
        else if (actionEvent.getSource().equals(btn_postings_browse) && selectedDir!=null)
        {
            lbl_posting_path = selectedDir.getPath();
            txt_postings_path.setText(selectedDir.getAbsolutePath());
            txt_postings_path.setDisable(true);
        }
        else if (actionEvent.getSource().equals(btn_resPath) && selectedDir!=null) {
            lbl_resPath.setText("  Results File Path: " + selectedDir.getAbsolutePath() + "\\results.txt");
            resQueries_path = selectedDir.getAbsolutePath();
        }
    }

    /**
     * when the user check the stemming box, toStem value will be true.
     * when the user uncheck the stemming box, toStem value will be false.
     */
    public void setToStem() {
        toStem = chbx_stemming.isSelected();
    }

    /**
     * when the user check the semantic box, toSemantic value will be true.
     * when the user uncheck the semantic box, toSemantic value will be false.
     */
    public void setToSemantic() {
        toSemantic = chbx_semantic.isSelected();
    }

    /**
     * reset action deletes all the files created by the application and restore it to
     * the initial state, meaning all text and check boxes are cleared.
     * SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    */
    public void reset() {
        try {
            File Posting_dir = new File(txt_postings_path.getText());
            if (Posting_dir.exists())
                FileUtils.cleanDirectory(Posting_dir);
        }
        catch (Exception e) { /* there are no files to delete --> do nothing*/ }
        txt_corpus_path.clear();
        txt_postings_path.clear();
        corpus_path = null;
        postings_path = null;
        lbl_posting_path = null;
        chbx_stemming.setSelected(false);
        chbx_stemming.setIndeterminate(false);
        toStem = false;
        m_languages.getItems().clear();
        loadedDictionary = null;
        loadedCities = null;
        new Alert(Alert.AlertType.INFORMATION, "Reset Completed").showAndWait();
    }

    /**
     * display the dictionary to the user in a table.
     */
    public void displayDictionary() {
        if (indexer == null && loadedDictionary==null) {
            new Alert(Alert.AlertType.ERROR, "Dictionary N/A").showAndWait();
            return;
        }
        try {
            Stage d_window = new Stage();
            d_window.setTitle("Dictionary");

            Collection<DictionaryRecord> list;
            if (indexer != null)
                list = indexer.getDictionary().values();
            else
                list = loadedDictionary.values();
            ObservableList<DictionaryRecord> details = FXCollections.observableArrayList(list);

            TableView<DictionaryRecord> tbl_dictionary = new TableView<>();
            TableColumn<DictionaryRecord, String> clmn_terms = new TableColumn<>();
            TableColumn<DictionaryRecord, Integer> clmn_totalFreq = new TableColumn<>();
            tbl_dictionary.setMinWidth(500.0);
            tbl_dictionary.setMinHeight(802.0);
            clmn_terms.setText("Terms");
            clmn_terms.setMinWidth(300.0);
            clmn_totalFreq.setText("Total Frequency");
            clmn_totalFreq.setMinWidth(200.0);

            //noinspection unchecked
            tbl_dictionary.getColumns().addAll(clmn_terms, clmn_totalFreq);

            clmn_terms.setCellValueFactory(data -> data.getValue().getTermProperty());
            clmn_totalFreq.setCellValueFactory(data -> data.getValue().getTotalFreqProperty());
            tbl_dictionary.setItems(details);

            ScrollPane scrollPane = new ScrollPane(tbl_dictionary);
            scrollPane.setFitToWidth(true);
            Scene scene = new Scene(scrollPane, 515, 705);
            d_window.setScene(scene);
            //d_window.setResizable(false);
            d_window.show();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Dictionary is unavailable");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    /**
     * loads the dictionary form the directory which its path is in the posting files text box
     */
    public void loadDictionary() {
        if (txt_postings_path.getText().isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "No posting path").showAndWait();
            return;
        }
        lbl_status.setText("loading dictionary...");
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String str = txt_postings_path.getText();
                    loadedDictionary = Indexer.readDictionaryFromFile(str);
                    postings_path = lbl_posting_path;

                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Missing Posting Path");
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    alert.showAndWait();
                }
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Dictionary Loaded");
            alert.show();
            lbl_status.setText("");
            addLanguages();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * add languages to the list will be presented to the user.
     * the languages values are read from a file in the program directory in the postings file directory
     */
    public void addLanguages() {
        File language = new File(postings_path + "/ProgramData/Languages.txt");
        if (readFile == null && !language.exists())
            return;
        m_languages.maxHeight(100.0);
        m_languages.getItems().clear();
        ArrayList<String> languages = new ArrayList<>();
        if (language.exists()){
            try {
                languages = restoreLanguages();
            }catch (IOException e){
                e.getStackTrace();
            }
        }
        else
            languages = readFile.getAllDocsLanguage();

        languages.forEach((lang) -> {
            MenuItem menuItem = new MenuItem(lang);
            m_languages.getItems().add(menuItem);
        });
    }

    /**
     * display the user a summary of the 'generate dictionary and posting files' process.
     * including #terms, #doc, time(min), time(sec)
     */
    private void displaySummary() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Indexing Summary");
        alert.setHeaderText(null);
        alert.setContentText("#Docs Indexed: " + this.indexer.getNumDocsIndexed() + '\n' +
                            "#Unique Terms: " + this.indexer.getNumUniqueTerms() + '\n' +
                            "Time(min): " + ((double)(endTime - startTime)/60000) + " min." +
                            "Time(sec): " + ((double)(endTime - startTime)/1000) + " sec."
        );
        alert.showAndWait();
    }

    /**
     * display the user a list of cities from the documents in the corpus.
     */
    public void displayCities() {
        try { restoreCities(); }
        catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "No Cities to display").showAndWait();
            return;
        }

        ObservableList<String> list = FXCollections.observableArrayList();
        list.addAll(loadedCities.keySet());
        list = list.sorted();

        if (checkListView == null)
            checkListView = new CheckListView<>(list);

        checkListView.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
            hs_citiesSelected = new HashSet<>();
            ObservableList<String> selected = checkListView.getCheckModel().getCheckedItems();
            if (readFile!=null) {
                for (String city : selected) {
                    hs_citiesSelected.add(readFile.getAllDocsCity().get(city));
                }
            } else {
                for (String city : selected)
                    hs_citiesSelected.add(loadedCities.get(city));
            }
            lbl_city.setText(selected.toString().substring(1, selected.toString().length()-1));
        });

        Stage window = new Stage();
        ScrollPane scrollPane = new ScrollPane(checkListView);
        Scene layout = new Scene(scrollPane);
        window.setScene(layout);
        window.showAndWait();
    }

    /**
     * run the query from the text area which the user had typed in.
     */
    public void runQuery() {
        if (indexer!=null) { // after generate dictionary and posting
            loadedDictionary = indexer.getDictionary();
        }

        if (loadedDictionary == null) {
            new Alert(Alert.AlertType.ERROR, "Load Dictionary First!").showAndWait();
            return;
        }
        if (lbl_posting_path == null || lbl_posting_path.length()==0) {
            new Alert(Alert.AlertType.ERROR, "Enter Postings Directory Path!").showAndWait();
            return;
        }
        if (corpus_path == null || corpus_path.length() == 0){
            new Alert(Alert.AlertType.ERROR, "Enter Corpus Directory Path!").showAndWait();
            return;
        }
        if (txt_queryEntry.getText().length()==0 && txt_queryPath.getText().length()==0) {
            new Alert(Alert.AlertType.ERROR, "Please Enter a Query").showAndWait();
            return;
        }
        if (txt_queryEntry.getText().length()>0 && txt_queryPath.getText().length()>0) {
            new Alert(Alert.AlertType.ERROR, "Unable to run two queries. Please Choose only one").showAndWait();
            return;
        }

        lbl_status.setText("searching...");
        // get the ranked documents
        if (searcher == null) {
            try { ranker = new Ranker(loadedDictionary, restoreDocuments()); }
            catch (IOException e) { e.printStackTrace(); }
        }
        boolean stem = false;
        if (postings_path.contains("Posting With Stemmer"))
            stem = true;

        if (resQueries_path == null || resQueries_path.length() == 0) {
            lbl_resPath.setText(postings_path + "\\results.txt");
            searcher = new Searcher(new Parser(corpus_path + "\\stop_words.txt", stem), loadedDictionary, ranker, postings_path, postings_path);
        }
        else
            searcher = new Searcher(new Parser(corpus_path + "\\stop_words.txt", stem), loadedDictionary, ranker, postings_path, resQueries_path);

        ObservableList<Document> retrievedDocumentsList;

        if (txt_queryEntry.getText().length() > 0) {
            if (hs_citiesSelected==null) hs_citiesSelected=new HashSet<>();
            retrievedDocumentsList = FXCollections.observableArrayList(searcher.relevantDocsFromQuery(txt_queryEntry.getText(), hs_citiesSelected, toSemantic));
            lbl_status.setText("");
        } else {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    searcher.relevantDocsFromQueryFile(queryFile, hs_citiesSelected, toSemantic);
                    return null;
                }
            };
            task.setOnSucceeded(event -> {
                lbl_status.setText("");
                new Alert(Alert.AlertType.INFORMATION, "IR  completed. Queries result stored in file!").showAndWait();
            });
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
            return;
        }

        // display the retrieved documents in new stage
        Stage window = new Stage();
        window.setTitle("Retrieved Documents");
        TableView<Document> tbl = new TableView<>();
        TableColumn<Document, String> col_ids = new TableColumn<>("Document ID");
        tbl.getColumns().add(col_ids);
        col_ids.setCellValueFactory(data -> data.getValue().getPropertyDoc_id());

        tbl.setItems(retrievedDocumentsList);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addButtonToTable(tbl);
        ScrollPane scrollPane = new ScrollPane(tbl);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        Scene scene = new Scene(scrollPane, 400, 500);
        window.setScene(scene);
        window.show();
    }

    /**
     * add a new column to the table that contains a button foreach row.
     * the button will open a new stage containing the top 5 most frequent entities in the document.
     * @param table .
     */
    private void addButtonToTable(TableView<Document> table ) {
        @SuppressWarnings("unchecked") TableColumn<Document, Void> colBtn = new TableColumn("Entities");
        Callback<TableColumn<Document, Void>, TableCell<Document, Void>> cellFactory = new Callback<TableColumn<Document, Void>, TableCell<Document, Void>>() {
            @Override
            public TableCell<Document, Void> call(final TableColumn<Document, Void> param) {
                @SuppressWarnings("UnnecessaryLocalVariable") final TableCell<Document, Void> cell = new TableCell<Document, Void>() {

                    private final Button btn = new Button("Show Entities");
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            Document doc = getTableView().getItems().get(getIndex());
                            List<String> entities = strongestEntities(doc.getEntities());
                            Stage window = new Stage();
                            window.setTitle("The Entities of " + doc.getDoc_id());
                            Label entity = new Label();

                            if (entities.isEmpty()){
                                entity.setText("There are no entities in this Document");
                            }else {
                                StringBuilder show = new StringBuilder();
                                for (int i = 0; i <entities.size() && i <5 ; i++){
                                    show.append(entities.get(i)+"\n");
                                }
                                if (show.toString().isEmpty()){
                                    show.append("There are no entities in this Document");
                                }
                                entity.setText(show.toString());
                            }
                            window.setScene(new Scene(entity, 300, 100));
                            window.show();
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        colBtn.setCellFactory(cellFactory);
        table.getColumns().add(colBtn);
    }

    /**
     * return the top 5 most frequent entities in the document (if exist)
     * @param entities .
     * @return .
     */
    private List<String> strongestEntities(String entities) {
        List<String> list = new ArrayList<>();
        if (entities.isEmpty())
            return list;
        String[] entitiesArr = entities.split("@");
        for (String s : entitiesArr) {
            if (loadedDictionary.containsKey(s.substring(0, s.indexOf("_")))) {
                int df = loadedDictionary.get(s.substring(0, s.indexOf("_"))).getDF();
                int newTf = Integer.parseInt(s.substring(s.indexOf("_") + 1));
                double rank = (double) (newTf) / (double) (df);
                rank = round(rank, 3);
                list.add("Entity: " + s.substring(0, s.indexOf("_")) + " ,Score: " + rank);
            }
        }

        list.sort((o1, o2) -> {
            double oo1 = Double.parseDouble(o1.substring(o1.indexOf("Score:") + 7));
            double oo2 = Double.parseDouble(o2.substring(o2.indexOf("Score:") + 7));
            if (oo1 < oo2) return 1;
            else if (oo1 > oo2) return -1;
            else
                return o1.substring(o1.indexOf("Entity: ") + 8, o1.indexOf(" ,Score:")).compareTo(o2.substring(o2.indexOf("Entity: ") + 8, o2.indexOf(" ,Score:")));
        });
        return list;
    }

    /**
     * rounds the value to upper bound with 2 digits after the decimal point
     * @param value
     * @param places
     * @return
     */
    private double round(double value, @SuppressWarnings("SameParameterValue") int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * run the query from the file in the path the user had entered.
     */
    public void browseQueryFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("All files (*)", "*")
        );
        queryFile = fileChooser.showOpenDialog(vBox_mainWindows.getScene().getWindow());
        if (queryFile !=null) {
            txt_queryPath.setText(queryFile.getAbsolutePath());
        }
    }

    public void clearQueryPath() {
        // clear text area/field
        txt_queryEntry.clear();
        txt_queryPath.clear();

        // clear cities
        checkListView = null;
        hs_citiesSelected = null;

        resQueries_path = "";
        lbl_resPath.setText("");
        queryFile = null;
        lbl_city.setText("");
        chbx_semantic.setSelected(false);
        chbx_semantic.setIndeterminate(false);
        toSemantic = false;

        searcher = null;
    }
}