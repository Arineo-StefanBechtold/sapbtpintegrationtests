package me.cxdev.sapbtp.testing.comparison;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

public class XmlDocumentComparator implements DocumentComparator {
    @Override
    public ComparisonResult compare(String expected, String actual, List<String> ignoreFields) {
        try {
            String sanitizedExpected = sanitize(expected, ignoreFields);
            String sanitizedActual = sanitize(actual, ignoreFields);
            Diff diff = DiffBuilder.compare(sanitizedExpected)
                    .withTest(sanitizedActual)
                    .ignoreWhitespace()
                    .checkForSimilar()
                    .build();
            return new ComparisonResult(!diff.hasDifferences(), diff.hasDifferences() ? diff.toString() : "");
        } catch (Exception e) {
            return new ComparisonResult(false, "XML comparison failed: " + e.getMessage());
        }
    }

    private String sanitize(String xml, List<String> ignoreFields) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        if (ignoreFields != null) {
            for (String xpathExpression : ignoreFields) {
                removeMatchingNodes(document, xpathExpression);
            }
        }
        StringWriter writer = new StringWriter();
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private void removeMatchingNodes(Document document, String xpathExpression) throws Exception {
        NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath()
                .compile(xpathExpression)
                .evaluate(document, XPathConstants.NODESET);
        for (int i = nodeList.getLength() - 1; i >= 0; i--) {
            Node node = nodeList.item(i);
            if (node.getParentNode() != null) {
                node.getParentNode().removeChild(node);
            }
        }
    }
}
