/*
 * Copyright 2014 CEL UK
 */
package svgfxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.Located;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *
 * @author tony
 */
class Parser
{

    private static final Set<String> styleClasses = new HashSet<>();

    public void parse(String fileName) throws JDOMException, IOException
    {
        System.out.println("parsing " + fileName);

        File directory = new File(fileName).getParentFile();
        String fileNameStr = new File(fileName).getName();
        List<String> nameParts = Arrays.asList(fileNameStr.split("\\."));
        fileNameStr = nameParts.get(0);
        String destinationFile = new File(directory, fileNameStr + ".fxml").getAbsolutePath();
        System.out.println("saving to " + destinationFile);

        SAXBuilder jdomBuilder = new SAXBuilder();
        jdomBuilder.setJDOMFactory(new LocatedJDOMFactory());
        jdomBuilder.setExpandEntities(false);

        // jdomDocument is the JDOM2 Object
        Document jdomDocument = jdomBuilder.build(fileName);

        Element svg = jdomDocument.getRootElement();

        Element convertedElement = processElement(svg);
        Document convertedDocument = new Document(convertedElement);

        // get object to see output of prepared document  
        XMLOutputter xmlOutput = new XMLOutputter();

        // passed fileWriter to write content in specified file  
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(convertedDocument, new FileWriter(destinationFile));

        String content = new String(Files.readAllBytes(Paths.get(destinationFile)));

        content = content.replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "");
        content = content.replaceFirst("Pane",
                                       "Pane xmlns=\"http://javafx.com/javafx/8\" xmlns:fx=\"http://javafx.com/fxml/1\"");
        content = content.replaceAll("FX", "fx:");
        content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "\n"
            + "<?import javafx.scene.control.*?>\n" + "<?import javafx.scene.*?>\n"
            + "<?import javafx.scene.shape.*?>\n" + "<?import java.lang.*?>\n"
            + "<?import javafx.scene.layout.*?>\n" + content;

        Files.write(Paths.get(destinationFile), content.getBytes());

        System.out.println("\nThe following style classes were used:");
        System.out.println("--------------------------------------");
        for (String styleClass : styleClasses)
        {
            System.out.println(styleClass);
        }
        System.out.println("\n");
        System.out.println("(Please add these classes to the css file.)\n");

    }

    private Element processElement(Element originalElement) throws DataConversionException
    {
        try {
        String newElementName = "";
        Element extraChildElement = null;
        Set<Attribute> attributes = null;
        boolean skip = false;
        switch (originalElement.getName().toLowerCase())
        {
            case "svg":
                newElementName = "Group";
                break;
            case "g":
                newElementName = "Group";
                break;
            case "circle":    
                newElementName = "Circle";
                attributes = processCircleAttributes(originalElement);
                break;
            case "ellipse":    
                newElementName = "Ellipse";
                attributes = processEllipseAttributes(originalElement);
                break;                
            case "rect":
                newElementName = "Rectangle";
                attributes = processRectangleAttributes(originalElement);
                break;
            case "polygon":
                newElementName = "Polygon";
                attributes = processPolygonAttributes(originalElement);
                extraChildElement = createPointsElement(originalElement);
                break;
            case "text":
                skip = true;
                break;
            case "line":
                newElementName = "Line";
                attributes = processLineAttributes(originalElement);
                break;
            case "polyline":
                newElementName = "Polyline";
                attributes = processPolylineAttributes(originalElement);
                extraChildElement = createPointsElement(originalElement);
                break;
            case "path":
                newElementName = "SVGPath";
                attributes = processSVGPathAttributes(originalElement);
                break;
            default:
                throw new RuntimeException("Did not recognise " + originalElement.getName());
        }
        if (skip)
        {
            return null;
        }
        Element newElement = new Element(newElementName);
        if (extraChildElement != null)
        {
            newElement.addContent(extraChildElement);
        }
        if (attributes != null)
        {
            newElement.setAttributes(attributes);
        }
        List<Element> children = new ArrayList<>();
        for (Element element : originalElement.getChildren())
        {
            Element childElement = processElement(element);
            if (childElement != null)
            {
                children.add(childElement);
            }
        }
        if (children.size() == 1)
        {
            // for groups with only one child, don't return the group, just the child
            return children.get(0);
        } else
        {
            newElement.addContent(children);
            return newElement;
        }
        }
        catch (Exception ex) {
            Located located = (Located) originalElement;
            System.out.println("Error processing " + originalElement + " at line " + located.getLine() + " column " + located.getColumn());
            throw ex;
        }
    }

    private Set<Attribute> processLineAttributes(Element element) throws DataConversionException
    {
        Set<Attribute> attributes = new HashSet<>();
        String x1 = element.getAttribute("x1").getValue();
        String y1 = element.getAttribute("y1").getValue();
        String x2 = element.getAttribute("x2").getValue();
        String y2 = element.getAttribute("y2").getValue();
        attributes.add(new Attribute("startX", x1));
        attributes.add(new Attribute("endX", x2));
        attributes.add(new Attribute("startY", y1));
        attributes.add(new Attribute("endY", y2));

        String stroke = element.getAttributeValue("stroke");
        attributes.add(new Attribute("stroke", stroke));

        String dashArray = element.getAttributeValue("stroke-dasharray");
        if (dashArray != null && !dashArray.equals("none") && !dashArray.equals(""))
        {
            String className = "strokeDashArray" + dashArray.replaceAll("\\.", "_");
            className = className.replaceAll(",", "-");
            attributes.add(new Attribute("styleClass", className));
            styleClasses.add(className);
        }

        return attributes;
    }

    private Set<Attribute> processSVGPathAttributes(Element element)
    {
        Set<Attribute> attributes = new HashSet<>();
        String stroke = element.getAttributeValue("stroke");
        if (stroke != null)
        {
            attributes.add(new Attribute("stroke", stroke));
        }
        String d = element.getAttribute("d").getValue();
        attributes.add(new Attribute("content", d));

        String fill = element.getAttributeValue("fill");
        if (fill != null && !fill.equals("none") && !fill.equals(""))
        {
            attributes.add(new Attribute("fill", fill));
        }

        return attributes;
    }

    private Set<Attribute> processPolylineAttributes(Element element)
    {
        Set<Attribute> attributes = new HashSet<>();
        String stroke = element.getAttributeValue("stroke");
        if (stroke != null)
        {
            attributes.add(new Attribute("stroke", stroke));
        }

        return attributes;
    }

    private Set<Attribute> processPolygonAttributes(Element element)
    {
        Set<Attribute> attributes = new HashSet<>();
        String fill = element.getAttributeValue("fill");
        if (fill != null && !fill.equals(""))
        {
            attributes.add(new Attribute("fill", fill));
        }
        return attributes;
    }
    
    private Set<Attribute> processEllipseAttributes(Element element)
    {
        Set<Attribute> attributes = new HashSet<>();
        String fill = element.getAttributeValue("fill");
        if (fill != null && !fill.equals(""))
        {
            attributes.add(new Attribute("fill", fill));
        }
        String cx = element.getAttributeValue("cx");
        String cy = element.getAttributeValue("cy");
        String rx = element.getAttributeValue("rx");
        String ry = element.getAttributeValue("ry");
        if (cy == null || cy.equals("")) {
            cy = "0";
        }
        if (cx == null || cx.equals("")) {
            cx = "0";
        }        
        attributes.add(new Attribute("centerX", cx));
        attributes.add(new Attribute("centerY", cy));
        attributes.add(new Attribute("radiusX", rx));
        attributes.add(new Attribute("radiusY", ry));
        return attributes;
    }    
    
    private Set<Attribute> processCircleAttributes(Element element)
    {
        Set<Attribute> attributes = new HashSet<>();
        String fill = element.getAttributeValue("fill");
        if (fill != null && !fill.equals(""))
        {
            attributes.add(new Attribute("fill", fill));
        }
        String cx = element.getAttributeValue("cx");
        String cy = element.getAttributeValue("cy");
        String r = element.getAttributeValue("r");
        if (cy == null || cy.equals("")) {
            cy = "0";
        }
        if (cx == null || cx.equals("")) {
            cx = "0";
        }        
        attributes.add(new Attribute("centerX", cx));
        attributes.add(new Attribute("centerY", cy));
        attributes.add(new Attribute("radius", r));
        return attributes;
    }      

    private Set<Attribute> processRectangleAttributes(Element element)
    {
        Set<Attribute> attributes = new HashSet<>();

        String x = element.getAttributeValue("x");
        String y = element.getAttributeValue("y");
        if (y == null || y.equals("")) {
            y = "0";
        }
        if (x == null || x.equals("")) {
            x = "0";
        }        
        String height = element.getAttributeValue("height");
        if (height == null || height.equals("")) {
            height = "0";
        }
        String width = element.getAttributeValue("width");
        if (width == null || width.equals("")) {
            width = "0";
        }        
        attributes.add(new Attribute("x", x));
        attributes.add(new Attribute("y", y));
        attributes.add(new Attribute("height", height));
        attributes.add(new Attribute("width", width));

        String fill = element.getAttributeValue("fill");
        if (fill != null && !fill.equals(""))
        {
            attributes.add(new Attribute("fill", fill));
        }
        return attributes;
    }

    private Element createPointsElement(Element element)
    {
        Element newElement = new Element("points");
        String pointsStr = element.getAttribute("points").getValue();
        List<String> points = Arrays.asList(pointsStr.split(" "));
        for (String point : points)
        {
            if (point.contains(","))
            {
                // <Double fx:value="-50.0" />
                List<String> xyValues = Arrays.asList(point.split(","));
                String x = xyValues.get(0);
                String y = xyValues.get(1);
                Element pointElementX = new Element("Double");
                pointElementX.setAttribute("FXvalue", x);
                newElement.addContent(pointElementX);
                Element pointElementY = new Element("Double");
                pointElementY.setAttribute("FXvalue", y);
                newElement.addContent(pointElementY);
            }
        }
        return newElement;
    }
}
