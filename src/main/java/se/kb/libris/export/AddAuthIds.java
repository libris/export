package se.kb.libris.export;

import java.util.*;
import se.kb.libris.util.marc.*;
import se.kb.libris.util.marc.io.*;
import java.util.regex.*;

public class AddAuthIds {
    /**
     * Remove everything except 'a'-'z','A'-'z','-' and '$'. Convert 'a'-'z' -> 'A'-'Z'
     * 
     * @param sb 
     */
    public static void normalize(StringBuffer sb) {
        int n=0;
        boolean whitespace = false, first = true;
        
        for (int i=0;i<sb.length();i++) {
            char c = sb.charAt(i);
            
            if (Character.isLetterOrDigit(c) || c == '-' || c == '$') {
                if (whitespace) {
                    sb.setCharAt(n++, ' ');
                }
                
                whitespace = false;
                sb.setCharAt(n++, Character.toUpperCase(c));
                first = false;
            } else if (Character.isWhitespace(c) && !whitespace) {
                if (!first) whitespace = true;
            }
        }
        
        sb.setLength(n);
    }
    
    /**
     * Adds pointers from 9XX fields to 100, 110, 111, 130, 240, 440, 
     * 600, 610, 611, 630, 648, 650, 651, 654, 655, 700, 710, 711, 730,
     * 800, 810, 811 and 830 fields
     * @param args 
     * @throws java.lang.Exception 
     */
    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: bla bla <in_encoding> <out_encoding>");
            System.exit(1);
        }
        
        String inEncoding = args[0], outEncoding = args[1];
        StrictIso2709Reader reader = new StrictIso2709Reader(System.in);
        byte record[] = null;
        Pattern pattern1 = Pattern.compile("100|110|111|130|240|440|600|610|" +
                                           "611|630|648|650|651|654|655|700|" +
                                           "710|711|730|800|810|811|830");
        Pattern pattern2 = Pattern.compile("9..");
        
        while ((record = reader.readIso2709()) != null) {
            MarcRecord mr = Iso2709Deserializer.deserialize(record, inEncoding);
            
            // step 1: iterate over 9XX-fields that migh contain links
            // to 1XX,6XX,7XX,8XX-fields
            Iterator iter = mr.iterator(pattern2);
            while (iter.hasNext()) {
                StringBuffer sb = new StringBuffer();
                Datafield df = (Datafield)iter.next();
                Subfield sf6 = null;
                Iterator siter = df.iterator();
                Set codes = new HashSet();
                
                boolean uFound = false;
                while (siter.hasNext()) {
                    Subfield sf = (Subfield)siter.next();
                    char code = sf.getCode();
                    
                    if (code == 'u') {
                        uFound = true;
                        code = 'a';
                    } else if (code == '6') {
                        sf6 = sf;
                        sf.setData("");
                    }
                    
                    if (uFound) {
                        sb.append(" $" + code + " " + sf.getData());
                        codes.add(String.valueOf(code));
                    }
                }
                
                if (sb.length() == 0) continue;

                // iterate over 1XX,6XX,7XX,8XX-fields
                Iterator iter2 = mr.iterator(pattern1);
                while (iter2.hasNext()) {
                    StringBuffer sb2 = new StringBuffer();
                    Datafield df2 = (Datafield)iter2.next();
                    
                    boolean hold = false, first = true;
                    Iterator siter2 = df2.iterator();
                    while (siter2.hasNext()) {
                        Subfield sf = (Subfield)siter2.next();
                        
                        if (sf.getCode() == '5') {
                            break;
                        }
                        
                        if (codes.contains(String.valueOf(sf.getCode())) || first) {
                            first = false;
                            sb2.append(" $" + sf.getCode() + " " + sf.getData());
                        }
                    }
                    
                    normalize(sb);
                    normalize(sb2);
                    
                    if (sb.length() > 2 && sb2.length() > 2 && 
                        sb.toString().trim().substring(2).equalsIgnoreCase(
                            sb2.toString().trim().substring(2))) {
                        System.err.println("match: " + df2.getTag()  + " -> " + df.getTag() + " '" + sb + "'");

                        if (sf6 == null) {
                            sf6 = df.createSubfield('6', "");
                            df.listIterator().add((Subfield) sf6);
                        } 
                        
                        if (sf6.getData().equals("")) {
                            sf6.setData(df2.getTag());
                        } else {
                            sf6.setData(sf6.getData() + "," + df2.getTag());
                        }
                    }
                }
            }

            System.out.write(Iso2709Serializer.serialize(mr, outEncoding));
        }

        reader.close();
    }
}
