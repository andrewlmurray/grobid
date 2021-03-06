package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * SAX parser for author sequences encoded in the TEI format data.
 * Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 * @author Patrice Lopez
 */
public class TEIAuthorSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private String title = null;
    private String affiliation = null;
    private String address = null;
    private String note = null;
    private String keywords = null;

    public int n = 0;

    public TEIAuthorSaxParser() {
        labeled = new ArrayList<String>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        if ((
                (qName.equals("forename")) | (qName.equals("middlename")) | (qName.equals("title")) |
                        (qName.equals("suffix")) |
                        (qName.equals("surname")) | (qName.equals("lastname")) | (qName.equals("marker")) |
                        (qName.equals("roleName"))
        ) & (currentTag != null)) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("author")) {
            String text = getText();
            if (text.length() > 0) {
                currentTag = "<other>";
                writeField(text);
            }
            labeled.add("\n \n");
            n++;
        }
        accumulator.setLength(0);
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {

        String text = getText();
        if (text.length() > 0) {
            currentTag = "<other>";
            writeField(text);
        }
        accumulator.setLength(0);

        if (qName.equals("title") | qName.equals("roleName")) {
            currentTag = "<title>";
        } else if (qName.equals("marker")) {
            currentTag = "<marker>";
        } else if ((qName.equals("surname")) | (qName.equals("lastname"))) {
            currentTag = "<surname>";
        } else if (qName.equals("middlename")) {
            currentTag = "<middlename>";
        } else if (qName.equals("forename")) {
            currentTag = "<forename>";
        } else if (qName.equals("suffix")) {
            currentTag = "<suffix>";
        }
        /*else if (qName.equals("author")) {
              int length = atts.getLength();

              // Process each attribute
              for (int i=0; i<length; i++) {
                  // Get names and values for each attribute
                  String name = atts.getQName(i);
                  String value = atts.getValue(i);

                  if (name != null) {
                      if (name.equals("file")) {
                             System.out.println(value);
                         }
                  }
              }

          }*/
    }

    private void writeField(String text) {
        // we segment the text
        //StringTokenizer st = new StringTokenizer(text, " \n\t");
        List<String> tokens = TextUtilities.segment(text, TextUtilities.punctuations);

        boolean begin = true;
        //while(st.hasMoreTokens()) {
        for (String tok : tokens) {
            //String tok = st.nextToken().trim();
            tok = tok.trim();
            if (tok.length() == 0) continue;
            boolean punct1 = false;

            if (tok.equals("+L+")) {
                labeled.add("@newline\n");
            } else if (tok.equals("+PAGE+")) {
                // page break not relevant for authors
                labeled.add("@newline\n");
            } else {
                String content = tok;
                int i = 0;
                for (; i < TextUtilities.punctuations.length(); i++) {
                    if (tok.length() > 0) {
                        if (tok.charAt(tok.length() - 1) == TextUtilities.punctuations.charAt(i)) {
                            punct1 = true;
                            content = tok.substring(0, tok.length() - 1);
                            break;
                        }
                    }
                }
                if (tok.length() > 0) {
                    if ((tok.startsWith("(")) & (tok.length() > 1)) {
                        if (punct1)
                            content = tok.substring(1, tok.length() - 1);
                        else
                            content = tok.substring(1, tok.length());
                        if (begin) {
                            labeled.add("(" + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add("(" + " " + currentTag + "\n");
                        }
                    } else if ((tok.startsWith("[")) & (tok.length() > 1)) {
                        if (punct1)
                            content = tok.substring(1, tok.length() - 1);
                        else
                            content = tok.substring(1, tok.length());
                        if (begin) {
                            labeled.add("[" + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add("[" + " " + currentTag + "\n");
                        }
                    } else if ((tok.startsWith("\"")) & (tok.length() > 1)) {
                        if (punct1)
                            content = tok.substring(1, tok.length() - 1);
                        else
                            content = tok.substring(1, tok.length());
                        if (begin) {
                            labeled.add("\"" + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add("\"" + " " + currentTag + "\n");
                        }
                    }
                }

                if (content.length() > 0) {
                    if (begin) {
                        labeled.add(content + " I-" + currentTag + "\n");
                        begin = false;
                    } else {
                        labeled.add(content + " " + currentTag + "\n");
                    }
                }

                if (punct1) {
                    if (begin) {
                        labeled.add(tok.charAt(tok.length() - 1) + " I-" + currentTag + "\n");
                        begin = false;
                    } else {
                        labeled.add(tok.charAt(tok.length() - 1) + " " + currentTag + "\n");
                    }
                }
            }

            begin = false;
        }
    }

}
