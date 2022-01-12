package pdfact.core.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class WriteToXML {

    private Document dom;

    public void saveToXML(Path xml, String file, List<IndexEntry> entries) {
        Element e = null;

        // instance of a DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use factory to get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // create instance of DOM
            dom = db.newDocument();

            // create the root index element
            Element indexEle = dom.createElement("index");

            // create data elements and place them under root
            e = dom.createElement("file");
            e.appendChild(dom.createTextNode(file));
            indexEle.appendChild(e);

            // create entries element
            Element entriesEle = dom.createElement("entries");
            indexEle.appendChild(entriesEle);

            for (IndexEntry entry : entries) {
                entriesEle.appendChild(createEntry(entry));
            }

            dom.appendChild(indexEle);

            try {
                Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                // send DOM to file
                tr.transform(new DOMSource(dom), new StreamResult(new FileOutputStream(xml.toString())));

            } catch (TransformerException te) {
                System.out.println(te.getMessage());
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        } catch (ParserConfigurationException pce) {
            System.out.println("UsersXML: Error trying to instantiate DocumentBuilder " + pce);
        }
    }

    public Element createEntry(IndexEntry entry) {
        Element e;
        Element entryEle = dom.createElement("entry");

        e = dom.createElement("phrase");
        e.appendChild(dom.createTextNode(entry.getPhrase()));
        entryEle.appendChild(e);

        Element pagenumbersEle = dom.createElement("pagenumbers");
        entryEle.appendChild(pagenumbersEle);

        for (int i = 0; i < entry.getNumbers().size(); i++) {
            String number = entry.getNumbers().get(i);
            int offset = entry.getOffsets().get(i);
            e = dom.createElement("number");
            e.setAttribute("pageOffset", String.valueOf(offset));
            e.appendChild(dom.createTextNode(number.replace(" ", "")));
            pagenumbersEle.appendChild(e);
        }

        if (entry.isHasSubEntries()) {
            Element subEntries = dom.createElement("subentries");
            entryEle.appendChild(subEntries);
            for (IndexEntry subEntry : entry.getSubentries()) {
                subEntries.appendChild(createEntry(subEntry));
            }
        }
        return entryEle;
    }

}
