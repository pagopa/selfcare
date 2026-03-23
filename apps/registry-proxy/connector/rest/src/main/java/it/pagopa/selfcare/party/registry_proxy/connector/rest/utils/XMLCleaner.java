package it.pagopa.selfcare.party.registry_proxy.connector.rest.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLCleaner {

    private XMLCleaner() {}

    public static byte[] cleanXml(byte[] rawXml, List<String> nodesToRemove) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(rawXml));

            for (String nodeToRemove : nodesToRemove) {
                NodeList elements = doc.getElementsByTagName(nodeToRemove);
                for (int i = elements.getLength() - 1; i >= 0; i--) {
                    Node node = elements.item(i);
                    Node parent = node.getParentNode();
                    if (parent != null) {
                        parent.removeChild(node);
                    }
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            enableProtectionXXE(transformerFactory);
            Transformer transformer = transformerFactory.newTransformer();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(output));
            return output.toByteArray();
        } catch (ParserConfigurationException
                 | SAXException
                 | IOException
                 | TransformerException
                 | IllegalArgumentException e) {
            throw new XmlProcessingException("Error during xml processing and cleaning", e);
        }
    }

    private static void enableProtectionXXE(TransformerFactory transformerFactory) throws TransformerConfigurationException {
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

}
