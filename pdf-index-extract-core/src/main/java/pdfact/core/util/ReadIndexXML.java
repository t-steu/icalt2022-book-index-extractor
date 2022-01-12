package pdfact.core.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ReadIndexXML {

    public Collection<IndexEntry> readPdf(String path) {
        ArrayList<IndexEntry> entries = new ArrayList<>();
        DocumentBuilderFactory docBuildFactory = DocumentBuilderFactory.newInstance();
        try {
            docBuildFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder docBuilder = docBuildFactory.newDocumentBuilder();
            File f = new File(path);
            Document doc = docBuilder.parse(f);

            // recommended but not necessary
            doc.getDocumentElement().normalize();

            NodeList nodeEntries = doc.getElementsByTagName("entry");

            ArrayList<Node> nodes = new ArrayList<>();
            // Filter all nodes with tag "entry" that belong to subentries as parent
            for (int i = 0; i < nodeEntries.getLength(); i++) {
                if (!nodeEntries.item(i).getParentNode().getNodeName().equals("subentries"))
                    nodes.add(nodeEntries.item(i));
            }

            entries.addAll(castEntriesTag(nodes));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            // e.printStackTrace();
            System.err.println("Goldstandard is not found under " + path);
        }
        return entries;
    }

    private Collection<IndexEntry> castEntriesTag(ArrayList<Node> nodeEntries) {
        ArrayList<IndexEntry> entries = new ArrayList<>();
        for (Node n : nodeEntries) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                entries.add(castEntryTag(e));
            }
        }
        return entries;
    }

    private IndexEntry castEntryTag(Element nodeElement) {
        IndexEntry entry = new IndexEntry();
        entry.setPhrase(castPhraseTag(nodeElement));

        NodeList pagenumbers = ((Element) nodeElement.getElementsByTagName("pagenumbers").item(0))
                .getElementsByTagName("number");

        entry.addNumbers(castPagenumbersTag(pagenumbers));

        Node subentriesNode = nodeElement.getElementsByTagName("subentries").item(0);

        if (subentriesNode != null && subentriesNode.getNodeType() == Node.ELEMENT_NODE) {

            NodeList subentries = ((Element) subentriesNode).getElementsByTagName("entry");
            for (int i = 0; i < subentries.getLength(); i++) {
                if (subentries.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    entry.addSubEntry(castEntryTag((Element) subentries.item(i)));
                }
            }
            NodeList refNodes = nodeElement.getElementsByTagName("reference");
            if (refNodes.getLength() != 0)
                entry.addReferences(castReferencesTag(refNodes));
        }
        return entry;
    }

    private Collection<String> castReferencesTag(NodeList nodes) {
        ArrayList<String> references = new ArrayList<>();
        for (int j = 0; j < nodes.getLength(); j++) {
            references.add(nodes.item(j).getTextContent());
        }
        return references;
    }

    private Collection<String> castPagenumbersTag(NodeList pagenumberNodes) {
        ArrayList<String> pagenumbers = new ArrayList<>();
        for (int j = 0; j < pagenumberNodes.getLength(); j++) {
            pagenumbers.add(pagenumberNodes.item(j).getTextContent());
        }
        return pagenumbers;
    }

    private String castPhraseTag(Element nodeElement) {
        return nodeElement.getElementsByTagName("phrase").item(0).getTextContent();
    }

}
