package sample.Models;

import java.io.*;
import java.util.*;

public class Parser {
    private HashSet<String> stopWords;
    private HashMap<String, Integer> months = new HashMap<>();
    private StemmerInterface stemmer = new Stemmer();
    private boolean stmr;

    public Parser(String path, boolean stmr) {
        this.stopWords = new HashSet<>();
        stopWordsFromFile(path);
        this.stmr = stmr;
        initMonths();
    }

    /**
     * Fill the stopWords HashSet in stop words for this corpus
     * @param path .
     */
    private void stopWordsFromFile(String path) {
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8"));
            String st;
            while ((st = br.readLine()) != null)
                this.stopWords.add(st.trim());
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    /**
     * Parse the text to terms by some the rules
     * @param text
     * @return dictionary fill with terms
     */
    public HashMap<String, TermData> Parsing(String text) {
        HashMap<String, TermData> dictionary = new HashMap<>();
        ArrayList<String> myList = new ArrayList<>();
        StringBuilder termBuild = new StringBuilder();
        homeMadeSplit(myList, text);
        int i = 0;
        while (!myList.isEmpty() && myList.size() - i > 0) {
            String current = myList.get(i);
            int index = -1;
            boolean toStem = false;

            if (stopWords.contains(current.toLowerCase()) && !current.trim().equals("between")) {
                i++;
                continue;
            }

            if (isNumberCase(current)) {
                /*
                 **********************************Our NEW rules!******************************************
                 */
                //AM PM rules
                if ((current.contains(":") && current.charAt(2) == ':' && isNumber(current.substring(0, 2) + current.substring(3, 5))) && (current.length() == 5 || current.toLowerCase().charAt(5) == 'p' || current.toLowerCase().charAt(5) == 'a')) {
                    if (myList.size() - (i + 1) > 0 && (myList.get(i + 1).toLowerCase().equals("a.m.") || myList.get(i + 1).toLowerCase().equals("am") || myList.get(i + 1).toLowerCase().equals("p.m.") || myList.get(i + 1).toLowerCase().equals("pm"))) {//HH:MM AM/PM
                        if (myList.get(i + 1).toLowerCase().equals("a.m.") || myList.get(i + 1).toLowerCase().equals("am"))
                            termBuild.append(current + " AM");
                        else
                            termBuild.append(current + " PM");
                        index = i;
                        i += 2;
                        //tokenized
                    } else if (isNumber(current.substring(0, 2) + current.substring(3, 5)) && (current.toLowerCase().contains("am") || current.toLowerCase().contains("pm"))) {//HH:MMAM/PM
                        if (current.toLowerCase().contains("am"))
                            termBuild.append(current.substring(0, current.toLowerCase().indexOf('a')) + " AM");
                        else
                            termBuild.append(current.substring(0, current.toLowerCase().indexOf('p')) + " PM");
                        index = i;
                        i += 1;
                        //tokenized
                    } else if (Integer.parseInt(current.substring(0, 2)) <= 24 && Integer.parseInt(current.substring(3, 5)) < 60) {//HH:MM
                        if (Integer.parseInt(current.substring(0, 2)) < 12)
                            termBuild.append(current + " AM");
                        else if (Integer.parseInt(current.substring(0, 2)) == 24)
                            termBuild.append("00:" + current.substring(3, 5) + " AM");
                        else
                            termBuild.append(current + " PM");
                        index = i;
                        i += 1;
                        //tokenized

                        //Percent range rule
                    } else if (Integer.parseInt(current.substring(0, 2)) + Integer.parseInt(current.substring(3, 5)) == 100) {//Our percent range term
                        termBuild.append(Integer.parseInt(current.substring(0, 2)) + "%-" + Integer.parseInt(current.substring(3, 5)) + "%");
                        index = i;
                        i += 1;
                        //tokenized
                    } else {
                        termBuild.append(current);
                        index = i;
                        i += 1;
                    }
                    /*
                     ***********************************Our NEW rules!*****************************************
                     */
                }
                //number-number
                else if ((current.contains("-") && !current.contains("--") && !current.equals("-")
                        && current.indexOf("-") != 0 && current.indexOf("-") + 1 != current.length()
                        && isRange(current))) {
                    termBuild.append(current.toLowerCase());
                    while (termBuild.toString().charAt(termBuild.toString().length() - 1) == '-')
                        termBuild.deleteCharAt(termBuild.toString().length() - 1);
                    index = i;
                    i += 1;
                    toStem = true;
                    //tokenized
                }
                //Percent Terms
                else if ((current.contains("%") && isNumber(current.substring(0, current.indexOf("%")))) || (isNumber(current) && myList.size() - (i + 1) > 0 && (myList.get(i + 1).toLowerCase().equals("percent")
                        || myList.get(i + 1).toLowerCase().equals("percentage")))) {//term format "number%" OR "number percernt/percentage"
                    if (current.contains("%")) {//term format "number%"
                        if (!current.substring(current.indexOf("%") + 1, current.length()).isEmpty())
                            termBuild.append(current.substring(0, current.indexOf("%") + 1));
                        else termBuild.append(current);
                        index = i;
                        i += 1;
                        //tokenized
                    } else {//term format "number percent/percentage"
                        termBuild.append(current + "%");
                        index = i;
                        i += 2;
                        //tokenized
                    }
                }
                //Date Terms
                else if (isNumber(current) && myList.size() - (i + 1) > 0 && (months.containsKey(myList.get(i + 1)))) {//term format "DD-MM"
                    int monthNum = months.get(myList.get(i + 1));
                    termBuild.append(monthNum);
                    if (monthNum < 10)
                        termBuild.insert(0, 0);
                    int day = (int) (parseNumber(current));
                    if (day < 10) {
                        termBuild.append("-0" + day);
                    } else termBuild.append("-" + day);
                    index = i;
                    i += 2;
                    //tokenized
                }
                //Prices that lower Then Million Terms
                else if (lowerThenMillion(current) && (current.contains("$") && isNumber(current.replace("$", ""))
                        || (((current.contains("/") && isNumber(current.replaceAll("/", ""))) || isNumber(current)) && myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("dollars"))
                        || (isNumber(current) && myList.size() - (i + 2) > 0 && (myList.get(i + 1).contains("/") && isNumber(myList.get(i + 1).replaceAll("/", ""))) && myList.get(i + 2).toLowerCase().equals("dollars")))) {
                    if (current.contains("$")) {//term format "$price"
                        termBuild.append(current);
                        while (termBuild.toString().contains("$"))
                            termBuild.deleteCharAt(termBuild.indexOf("$"));
                        termBuild.append(" Dollars");
                        index = i;
                        i += 1;
                        //tokenized
                    } else if (myList.get(i + 1).toLowerCase().equals("dollars")) {//term format "price dollars"
                        termBuild.append(current + " Dollars");
                        index = i;
                        i += 2;
                        //tokenized
                    } else {//term format "price fraction dollars"
                        termBuild.append(current + " " + myList.get(i + 1) + " Dollars");
                        index = i;
                        i += 3;
                        //tokenized
                    }
                }

                //Prices that higher Then Million Terms
                else if ((current.contains("$") && isNumber(current.replace("$", "")))
                        || (isNumber(current) && (
                        (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("dollars"))
                                || (myList.size() - (i + 2) > 0 && (myList.get(i + 1).toLowerCase().equals("m") || myList.get(i + 1).toLowerCase().equals("bn")) && (myList.get(i + 2).toLowerCase().equals("dollars")))
                                || (myList.size() - (i + 3) > 0 && (myList.get(i + 1).toLowerCase().equals("million") || myList.get(i + 1).toLowerCase().equals("billion") || myList.get(i + 1).toLowerCase().equals("trillion")) &&
                                myList.get(i + 2).equals("U.S.") && myList.get(i + 3).toLowerCase().equals("dollars"))))) {
                    int x = 0;
                    if (current.contains("$")) {//term format "$price" OR "$price million/billion/trillion"
                        if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("million")) {//term format "$price million"
                            termBuild.append(current + " M Dollars");
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            index = i;
                            i += 2;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("billion")) {//term format "$price billion"
                            termBuild.append(current);
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            double num = 1000 * parseNumber(termBuild.toString());
                            termBuild.delete(0, termBuild.length());
                            termBuild.append(num);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append(" M Dollars");
                            index = i;
                            i += 2;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("trillion")) {//term format "$price trillion"
                            termBuild.append(current);
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            double num = 1000000 * parseNumber(termBuild.toString());
                            termBuild.delete(0, termBuild.length());
                            termBuild.append(num);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append(" M Dollars");
                            index = i;
                            i += 2;
                            //tokenized
                        } else {//term format "$price"
                            termBuild.append(current);
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            double num = parseNumber(termBuild.toString()) / 1000000;
                            termBuild.delete(0, termBuild.length());
                            termBuild.append(num);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append(" M Dollars");
                            index = i;
                            i += 1;
                            //tokenized
                        }
                    } else {//term format "price dollars" OR "price m/bn dollars"
                        if (myList.size() - (i + 3) > 0 && myList.get(i + 2).equals("U.S.") /*&& myList.get(i + 3).equals("S") */ && myList.get(i + 3).toLowerCase().contains("dollars")) {
                            x += 1;
                        }
                        if (myList.size() - (i + 1) > 0 && (myList.get(i + 1).toLowerCase().equals("m") || (myList.get(i + 1).equals("million")))) {//term format "price m dollars" or "price million U.S. dollars"
                            termBuild.append(current + " M Dollars");
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            index = i;
                            i = i + 2 + x;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && (myList.get(i + 1).toLowerCase().equals("bn") || (myList.get(i + 1).toLowerCase().equals("billion")))) {//term format "price bn dollars" or "price billion U.S. dollars"
                            termBuild.append(current);
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            double num = 1000 * parseNumber(termBuild.toString());
                            termBuild.delete(0, termBuild.length());
                            termBuild.append(num);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append(" M Dollars");
                            index = i;
                            i = i + 2 + x;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && (myList.get(i + 1).toLowerCase().equals("trillion"))) {//term format "price trillion U.S. dollars"
                            termBuild.append(current);
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            double num = 1000000 * parseNumber(termBuild.toString());
                            termBuild.delete(0, termBuild.length());
                            termBuild.append(num);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append(" M Dollars");
                            index = i;
                            i = i + 2 + x;
                            //tokenized
                        } else {//term format "price dollars"
                            termBuild.append(current);
                            while (termBuild.toString().contains("$"))
                                termBuild.deleteCharAt(termBuild.indexOf("$"));
                            double num = parseNumber(termBuild.toString()) / 1000000;
                            termBuild.delete(0, termBuild.length());
                            termBuild.append(num);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append(" M Dollars");
                            index = i;
                            i = i + 2 + x;
                            //tokenized
                        }
                    }
                }
                //Numbers Terms
                else if (isNumber(current) || (current.contains("/") && isNumber(current.replaceAll("/", "")))) {//term format "number" OR "number thousand/million/billion/trillion"
                    if (!current.contains("/") && (Math.abs(parseNumber(current)) >= 1000 || (myList.size() - (i + 1) > 0 && (myList.get(i + 1).toLowerCase().equals("thousand") || myList.get(i + 1).toLowerCase().equals("million") || myList.get(i + 1).toLowerCase().equals("billion") || myList.get(i + 1).toLowerCase().equals("trillion"))))) {
                        if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("thousand")) {
                            termBuild.append(current + "K");
                            index = i;
                            i += 2;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("million")) {
                            termBuild.append(current + "M");
                            index = i;
                            i += 2;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("billion")) {
                            termBuild.append(current + "B");
                            index = i;
                            i += 2;
                            //tokenized
                        } else if (myList.size() - (i + 1) > 0 && myList.get(i + 1).toLowerCase().equals("trillion")) {
                            double numberBefore = parseNumber(current) * 1000;
                            termBuild.append(numberBefore);
                            if (termBuild.toString().substring(termBuild.indexOf("."), termBuild.length()).equals(".0"))
                                termBuild.delete(termBuild.indexOf("."), termBuild.length());
                            termBuild.append("B");
                            index = i;
                            i += 2;
                            //tokenized
                        } else {
                            termBuild.append(isKorMorB(current));
                            index = i;
                            i += 1;
                        }
                    } else {
                        if (current.contains("/") && current.charAt(0) != '0' && current.charAt(current.indexOf('/') + 1) != '0') {
                            termBuild.append(current);
                            index = i;
                            i += 1;
                            //tokenized
                        } else {
                            int y = 0;
                            if (current.charAt(0) != '0') {
                                termBuild.append(isKorMorB(current));
                                if (!(termBuild.toString().toLowerCase().contains("k") || termBuild.toString().toLowerCase().contains("m") || termBuild.toString().toLowerCase().contains("b")) && myList.size() - (i + 1) > 0 && (myList.get(i + 1).contains("/")
                                        && myList.get(i + 1).charAt(0) != '0' && myList.get(i + 1).charAt(current.indexOf('/') + 1) != '0' && isNumber(myList.get(i + 1).replaceAll("/", "")))) {
                                    termBuild.append(" " + myList.get(i + 1));
                                    y++;
                                }
                            } else
                                termBuild.append(current);
                            index = i;
                            i = i + 1 + y;
                            //tokenized
                        }
                    }
                }
            } else if (current.contains("-") || current.toLowerCase().equals("between") || months.containsKey(current)) {
                //term with "-"
                if ((current.contains("-") && !current.contains("--") && !current.equals("-")
                        && current.indexOf("-") != 0 && current.indexOf("-") + 1 != current.length()
                        && isRange(current))) {
                    if (Character.isUpperCase(current.charAt(0)))
                        termBuild.append(current.toUpperCase());
                    else termBuild.append(current.toLowerCase());
                    while (termBuild.toString().charAt(termBuild.toString().length() - 1) == '-')
                        termBuild.deleteCharAt(termBuild.toString().length() - 1);
                    index = i;
                    i += 1;
                    toStem = true;
                    //tokenized
                }
                //term format "Between num and num"
                else if (myList.size() - (i + 3) > 0 && current.toLowerCase().equals("between")
                        && isNumber(myList.get(i + 1)) && isNumber(myList.get(i + 3))
                        && myList.get(i + 2).toLowerCase().equals("and")) {//term format "Between num and num"
                    termBuild.append("Between " + myList.get(i + 1) + " " + myList.get(i + 2) + " " + myList.get(i + 3));
                    index = i;
                    i += 4;
                    //tokenized
                }
                //term format "MM-YYYY" OR "MM-DD"
                else if (months.containsKey(current) && myList.size() - (i + 1) > 0 && isNumber(myList.get(i + 1))) {
                    int monthNum = months.get(current);
                    termBuild.append(monthNum);
                    if (monthNum < 10)
                        termBuild.insert(0, 0);
                    int yearOrDay = (int) (parseNumber(myList.get(i + 1)));
                    if (yearOrDay > 0 && yearOrDay <= 31) {//term format "MM-DD"
                        if (yearOrDay < 10) {
                            termBuild.append("-0" + yearOrDay);
                        } else termBuild.append("-" + yearOrDay);
                        index = i;
                        i += 2;
                        //tokenized
                    } else {//term format "MM-YYYY"
                        termBuild.insert(0, yearOrDay + "-");
                        index = i;
                        i += 2;
                        //tokenized
                    }
                }
            }

            if (termBuild.toString().isEmpty() && current.trim().equals("between")) {
                i++;
                continue;
            }

            //if the current word is not any rule then fill it in as a word and check uppercase and lowercase rules
            String termToDic = "";
            if (termBuild.toString().isEmpty() || toStem == true) {
                if (this.stmr) {
                    if (termBuild.length() == 0)
                        termToDic = stemmer.stemTerm(this.stmr, current);
                    else
                        termToDic = stemmer.stemTerm(this.stmr, termBuild.toString());
                } else if (termBuild.toString().isEmpty()) termToDic = current;
                else termToDic = termBuild.toString();
                termBuild.delete(0, termBuild.length());
                if (dictionary.containsKey(termToDic.toLowerCase())) {//The term already in the dictionary in lowercase
                    termBuild.append(termToDic.toLowerCase());
                    index = i;
                    i += 1;
                } else if (dictionary.containsKey(termToDic.toUpperCase())) {//The term already in the dictionary in uppercase
                    if (Character.isUpperCase(termToDic.charAt(0))) {
                        termBuild.append(termToDic.toUpperCase());
                        index = i;
                        i += 1;
                    } else {
                        int tf = dictionary.get(termToDic.toUpperCase()).gettF() + 1;
                        boolean isImportant = dictionary.get(termToDic.toUpperCase()).getImportant();
                        String place = dictionary.get(termToDic.toUpperCase()).getPlaces();
                        dictionary.remove(termToDic.toUpperCase());
                        dictionary.put(termToDic.toLowerCase(), new TermData(isImportant, tf, place, i));
                        i++;
                        //tokenized
                        continue;
                    }
                } else {//The term is not exist in the dictionary
                    if (Character.isUpperCase(termToDic.charAt(0))) {//add the term to the dictionary in uppercase
                        termBuild.append(termToDic.toUpperCase());
                        index = i;
                        i += 1;
                        //tokenized
                    } else {//add the term to the dictionary in lowercase
                        termBuild.append(termToDic.toLowerCase());
                        index = i;
                        i += 1;
                        //tokenized
                    }
                }
            }

            //add term to the dictionary
            addToDictonary(dictionary, termBuild.toString(), index);
            termBuild.delete(0, termBuild.length());

        }
        return dictionary;
    }

    /**
     * spliting the text by delimiter char by char
     * @param myList - the list to fill
     * @param text   - the text i got to split
     */
    private void homeMadeSplit(ArrayList<String> myList, String text) {
        StringBuilder str = new StringBuilder();
        int i = 0;
        char s;
        while (text.length() > i) {
            s = text.charAt(i);
            if ((s == ' ' || s == '\\' || s == '[' || s == ']' || s == '!' || s == '@' || s == '#' || s == '^' || s == '&' || s == '*' || s == '(' || s == ')' || s == '+' || s == '='
                    || s == '`' || s == '~' || s == '?' || s == '"' || s == ';' || s == '{' || s == '}' || s == '|' || s == '<' || s == '>' || s == '\n' || s == '_')//
                    || (s == ':' && text.length() > i + 2 && !(Character.isDigit(text.charAt(i - 1)) && Character.isDigit(text.charAt(i - 2)) && Character.isDigit(text.charAt(i + 1)) && Character.isDigit(text.charAt(i + 2))))
                    || (s == '%' && !isNumber(str.toString()))
                    || (s == '/' && text.length() > i + 1 && !slash(str.toString(), text.charAt(i + 1)))
                    || (s == '\'' && text.length() > i + 1 && !apostrophe(str.toString(), text.charAt(i + 1)))
                    || (s == '$' && text.length() > i + 1 && !((text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '-') && Character.isDigit(text.charAt(i + 1))))
                    || (s == '-' && text.length() > i + 1 && !dash(str.toString(), text.charAt(i + 1)))
                    || (s == '.' && text.length() > i + 1 && !dot(str.toString(), text.charAt(i + 1)))
                    || (s == ',' && (str.length() == 0 || text.length() > i + 3 && !(Character.isDigit(text.charAt(i - 1)) && Character.isDigit(text.charAt(i + 1)) && Character.isDigit(text.charAt(i + 2)) && Character.isDigit(text.charAt(i + 3)))))) {
                if (str.length() > 0) {
                    myList.add(str.toString());
                    str.delete(0, str.length());
                }
            } else
                str.append(s);
            i++;
        }
        if (!str.toString().isEmpty()){
            myList.add(str.toString());
            str.delete(0,str.length());
        }
    }

    /**
     * func that add to the Dictionary terms by their values
     * @param dic   - the hash map to add to it the terms
     * @param token - string of the token will be the Key
     * @param index - for creating the term objact and check the place of the token if he is un the first 30 indexs so he is important
     * @return
     */
    private void addToDictonary(HashMap<String, TermData> dic, String token, int index) {
        boolean isImportant = false;
        if (index < 30)
            isImportant = true;

        if (dic.containsKey(token)) {
            dic.get(token).settF(dic.get(token).gettF() + 1);
            dic.get(token).setPlace(index);
        } else
            dic.put(token, new TermData(isImportant, 1, index));
    }

    /**
     * check if token is phrase
     * @return true if 'str' is a phrase
     */
    private static boolean isRange(String str) {
        String[] s = str.split("-");
        if (s.length > 3) return false;
        if (s.length < 3) return true;
        if (isNumber(s[0]) || isNumber(s[1]) || isNumber(s[2])) return false;
        return true;
    }

    /**
     * check if string is number
     * @return true if the argument is a number.
     */
    private static boolean isNumber(String strNum) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strNum);
        while (stringBuilder.toString().contains(","))
            stringBuilder.deleteCharAt(stringBuilder.indexOf(","));
        try {
            double d = Double.parseDouble(stringBuilder.toString());
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    /**
     * check if the string is a number case for the rules that given
     * number case is price term ,am/pm term ,number term and percent term
     * @param strNum
     * @return
     */
    private static boolean isNumberCase(String strNum) {
        if (strNum.contains("%") || strNum.contains("$") || strNum.contains(":")) return true;
        for (int i = 0; strNum.length() > i; i++) {
            char c = strNum.charAt(i);
            if (Character.isLetter(c))
                return false;
        }
        return true;
    }

    /**
     * parsing string to number
     * @param strNum
     * @return the number as double
     */
    private static double parseNumber(String strNum) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strNum);
        while (stringBuilder.toString().contains(","))
            stringBuilder.deleteCharAt(stringBuilder.indexOf(","));
        double d = Double.parseDouble(stringBuilder.toString());
        return d;
    }

    /**
     * categorized the number to K/M/B
     * @param number
     * @return the number with K/M/B
     */
    private static String isKorMorB(String number) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(number);
        while (stringBuilder.toString().contains(","))
            stringBuilder.deleteCharAt(stringBuilder.indexOf(","));
        double num = Double.parseDouble(stringBuilder.toString());
        stringBuilder.delete(0, stringBuilder.length());
        if (num < 1000) {
            return number;
        } else if (num < 1000000) {
            num /= 1000;
            stringBuilder.append(num + "K");
        } else if (num < 1000000000) {
            num /= 1000000;
            stringBuilder.append(num + "M");
        } else {
            num /= 1000000000;
            stringBuilder.append(num + "B");
        }
        if (stringBuilder.toString().substring(stringBuilder.toString().indexOf("."), stringBuilder.toString().length() - 1).equals(".0")) {
            stringBuilder.delete(stringBuilder.toString().length() - 3, stringBuilder.toString().length() - 1);
        }
        return stringBuilder.toString();
    }

    /**
     * check if the string is homeMadeSplit number that lower then million
     * @param number
     * @return
     */
    private static boolean lowerThenMillion(String number) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(number);
        while (stringBuilder.toString().contains(","))
            stringBuilder.deleteCharAt(stringBuilder.indexOf(","));
        if (stringBuilder.toString().contains("$"))
            stringBuilder.deleteCharAt(stringBuilder.indexOf("$"));
        double num;
        try {
            num = Double.parseDouble(stringBuilder.toString());
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        if (num < 1000000) {
            return true;
        }
        return false;
    }

    /**
     * get string before the slash and the char after and return true or false by some rules
     * @param str
     * @param c
     * @return if its a legal slash
     */
    private static boolean slash(String str, Character c) {
        if (str.equals("0")) return false;
        if (c == '0') return false;
        if (str.equals("")) return false;
        if (!Character.isDigit(c))
            return false;
        for (int i = 0; str.length() > i; i++) {
            char ch = str.charAt(i);
            if (i == 0 && (ch == '-' || ch == '$'))
                continue;
            if (ch == ',')
                continue;
            if (!Character.isDigit(ch))
                return false;
        }
        return true;
    }

    /**
     * get string before the apostrophe and the char after and return true or false by some rules
     * @param str
     * @param c
     * @return if its a legal apostrophe
     */
    private static boolean apostrophe(String str, Character c) {
        if (str.isEmpty()) return false;
        if (!Character.isLetter(c)) return false;
        return true;
    }

    /**
     * get string before the dot and the char after and return true or false by some rules
     * @param str
     * @param c
     * @return if its a legal dot
     */
    private static boolean dot(String str, Character c) {
        if (str.equals("U") && c == 'S') return true;
        if (str.equals("U.S") && c == ' ') return true;
        if ((str.toUpperCase().equals("A") || str.toUpperCase().equals("P")) && c.toString().toUpperCase().equals("M"))
            return true;
        if ((str.toUpperCase().equals("A.M") || str.toUpperCase().equals("P.M")) && c == ' ') return true;
        if (str.toUpperCase().equals("ST") && (c == ' ' || c.toString().toUpperCase().contains("P"))) return true;

        if (!Character.isDigit(c))
            return false;
        for (int i = 0; str.length() > i; i++) {
            char ch = str.charAt(i);
            if (i == 0 && (ch == '-' || ch == '$'))
                continue;
            if (ch == ',')
                continue;
            if (!Character.isDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    /**
     * get string before the dash and the char after and return true or false by some rules
     * @param str
     * @param c
     * @return if its a legal dash
     */
    private static boolean dash(String str, Character c) {
        if (c == ' ') return false;
        if (str.isEmpty() && c == '-') return false;
        if (str.isEmpty() && Character.isDigit(c)) return true;
        if (str.isEmpty() && c == '$') return true;
        if (!str.isEmpty() && (Character.isDigit(c) || Character.isLetter(c) || c == '$')) return true;
        return false;
    }

    /**
     * initialize the months
     */
    private void initMonths() {
        months.put("JAN", 1);
        months.put("Jan", 1);
        months.put("JANUARY", 1);
        months.put("January", 1);
        months.put("FEB", 2);
        months.put("Feb", 2);
        months.put("February", 2);
        months.put("FEBRUARY", 2);
        months.put("Mar", 3);
        months.put("MAR", 3);
        months.put("March", 3);
        months.put("MARCH", 3);
        months.put("Apr", 4);
        months.put("APR", 4);
        months.put("April", 4);
        months.put("APRIL", 4);
        months.put("May", 5);
        months.put("MAY", 5);
        months.put("June", 6);
        months.put("JUNE", 6);
        months.put("July", 7);
        months.put("JULY", 7);
        months.put("Aug", 8);
        months.put("AUG", 8);
        months.put("August", 8);
        months.put("AUGUST", 8);
        months.put("Sept", 9);
        months.put("SEPT", 9);
        months.put("September", 9);
        months.put("SEPTEMBER", 9);
        months.put("Oct", 10);
        months.put("OCT", 10);
        months.put("October", 10);
        months.put("OCTOBER", 10);
        months.put("Nov", 11);
        months.put("NOV", 11);
        months.put("November", 11);
        months.put("NOVEMBER", 11);
        months.put("Dec", 12);
        months.put("DEC", 12);
        months.put("December", 12);
        months.put("DECEMBER", 12);
    }
}