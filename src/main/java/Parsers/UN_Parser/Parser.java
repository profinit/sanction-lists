package Parsers.UN_Parser;

import Helpers.Defines;
import Parsers.IParser;
import Parsers.SanctionListEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Stack;

import static Helpers.Defines.replaceCountryAbbreviation;
import static Helpers.Defines.replaceNationalityAdjective;

/**
 * @author Peter Babics &lt;babicpe1@fit.cvut.cz&gt;
 */
public class Parser implements IParser
{

    private final Stack<SanctionListEntry> list = new Stack<SanctionListEntry>();

    private HashSet<String> parseAliasNode(Node node)
    {
        NodeList childs = node.getChildNodes();
        String aliasses = null;
        for (int i = 0; i < childs.getLength(); ++i)
        {
            Node child = childs.item(i);
            String childName = child.getNodeName().toLowerCase().trim();
            if (childName.compareTo("alias_name") == 0)
            {
                aliasses = child.getNodeValue();
                break;
            }
        }

        if (aliasses != null)
        {
            HashSet<String> out = new HashSet<String>();
            String[] t = aliasses.split(";");
            for (String s : t)
                out.add(Defines.sanitizeString(s));
            return out;
        }

        return null;
    }

    private String parseAddressNode(Node node)
    {
        NodeList childs = node.getChildNodes();
        String street = null;
        String city = null;
        String country = null;
        for (int i = 0; i < childs.getLength(); ++i)
        {
            Node child = childs.item(i);
            String childName = child.getNodeName().toLowerCase().trim();
            if (childName.compareTo("street") == 0)
                street = child.getNodeValue();
            else if (childName.compareTo("city") == 0)
                city = child.getNodeValue();
            else if (childName.compareTo("country") == 0)
                country = child.getNodeValue();

        }
        String place = street;
        if (city != null)
        {
            if (place == null)
                place = city;
            else
                place = place + " " + city;
            place = place.trim();
        }
        if (country != null)
        {
            if (place == null)
                place = country;
            else
                place = place + " " + city;
            place = place.trim();
        }

        return place;
    }

    private String parseBirthDateNode(Node node)
    {
        NodeList childs = node.getChildNodes();
        String date = null;
        for (int i = 0; i < childs.getLength(); ++i)
        {
            Node child = childs.item(i);
            String childName = child.getNodeName().toLowerCase().trim();
            if (childName.compareTo("YEAR") == 0)
                date = child.getNodeValue();
        }
        return date;
    }

    private String parseBirthPlaceNode(Node node)
    {
        NodeList childs = node.getChildNodes();
        String city = null;
        String country = null;
        for (int i = 0; i < childs.getLength(); ++i)
        {
            Node child = childs.item(i);
            String childName = child.getNodeName().toLowerCase().trim();
            if (childName.compareTo("city") == 0)
                city = child.getNodeValue();
            else if (childName.compareTo("country") == 0)
                country = child.getNodeValue();
        }

        String place = city;
        if (country != null)
        {
            if (place == null)
                place = country;
            else
                place = place + " " + city;
            place = place.trim();
        }

        return place;
    }

    private String parseNationalityNode(Node node)
    {
        NodeList childs = node.getChildNodes();
        for (int i = 0; i < childs.getLength(); ++i)
        {
            Node child = childs.item(i);
            String childName = child.getNodeName().toLowerCase().trim();
            if (childName.compareTo("value") == 0)
                return child.getNodeValue();
        }
        return null;
    }

    private void parseEntityNodeList(NodeList entities, String prefix)
    {
        for (int i = 0; i < entities.getLength(); ++i)
        {
            Node node = entities.item(i);

            SanctionListEntry entry = new SanctionListEntry("UN", prefix.compareTo("individual_") == 0 ? SanctionListEntry.EntryType.PERSON : SanctionListEntry.EntryType.COMPANY);

            NodeList childs = node.getChildNodes();
            String[] names = new String[5];

            for (int j = 0; j < childs.getLength(); ++j)
            {
                Node child = childs.item(j);
                String nodeName = child.getNodeName().toLowerCase();
                if (nodeName.compareTo("first_name") == 0)
                    names[0] = child.getNodeValue();
                else if (nodeName.compareTo("second_name") == 0)
                    names[1] = child.getNodeValue();
                else if (nodeName.compareTo("third_name") == 0)
                    names[2] = child.getNodeValue();
                else if (nodeName.compareTo("fourth_name") == 0)
                    names[3] = child.getNodeValue();
                else if (nodeName.compareTo("fifth)name") == 0)
                    names[4] = child.getNodeValue();
                else if (nodeName.compareTo("first_name") == 0)
                {
                    HashSet<String> aliases = parseAliasNode(child);
                    if (aliases != null && aliases.size() > 0)
                        entry.names.addAll(aliases);
                } else if (nodeName.compareTo(prefix + "date_of_brith") == 0)
                {
                    String birth = parseBirthDateNode(child);
                    if (birth != null && birth.trim().length() > 0)
                        entry.datesOfBirth.add(Defines.sanitizeString(birth));
                } else if (nodeName.compareTo(prefix + "place_of_brith") == 0)
                {
                    String birth = parseBirthPlaceNode(child);
                    if (birth != null && birth.trim().length() > 0)
                        entry.placesOfBirth.add(replaceCountryAbbreviation(Defines.sanitizeString(birth)));
                } else if (nodeName.compareTo("nationality") == 0)
                {
                    String nationality = parseNationalityNode(child);
                    if (nationality != null && nationality.trim().length() > 0)
                        entry.nationalities.add(replaceNationalityAdjective(replaceCountryAbbreviation(Defines.sanitizeString(nationality))));
                } else if (nodeName.compareTo(prefix + "address") == 0)
                {
                    String address = parseAddressNode(child);
                    if (address != null && address.trim().length() > 0)
                        entry.addresses.add(replaceCountryAbbreviation(Defines.sanitizeString(address)));
                }
            }

            String name = names[0];
            for (int n = 1; n < 5; ++n)
                if (names[n] != null)
                {
                    if (name == null)
                        name = names[n];
                    else
                        name = name + " " + names[n];
                }
            if (name != null)
                entry.names.add(Defines.sanitizeString(name));

            list.push(entry);
        }
    }

    public void initialize(InputStream stream)
    {
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(stream);
            XPath selector = XPathFactory.newInstance().newXPath();
            NodeList entities = (NodeList) selector.compile("//INDIVIDUALS/INDIVIDUAL").evaluate(document, XPathConstants.NODESET);
            parseEntityNodeList(entities, "individual_");
            entities = (NodeList) selector.compile("//ENTITIES/ENTITY").evaluate(document, XPathConstants.NODESET);
            parseEntityNodeList(entities, "entity_");

        } catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }  catch (SAXException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (XPathExpressionException e)
        {
            e.printStackTrace();
        }
    }

    public SanctionListEntry getNextEntry()
    {
        if (list.size() == 0)
            return null;
        return list.pop();
    }
}