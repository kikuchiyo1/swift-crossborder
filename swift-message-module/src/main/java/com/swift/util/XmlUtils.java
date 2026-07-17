package com.swift.util;

/** XML 轻量提取工具，只取报文头关键字段 */
public final class XmlUtils {
    private XmlUtils() {}

    public static String detectType(String xml) {
        if (xml.contains("FIToFICstmrCdtTrf")) return "pacs.008";
        if (xml.contains("FIToFIPmtStsRpt"))  return "pacs.002";
        if (xml.contains("<SwAck>")) {
            return xml.contains("<AckSts>NACK</AckSts>") ? "NACK" : "ACK";
        }
        return "UNKNOWN";
    }

    public static String extractTag(String xml, String tag) {
        int s = xml.indexOf("<" + tag + ">");
        int e = xml.indexOf("</" + tag + ">", s);
        return (s < 0 || e < 0) ? "" : xml.substring(s + tag.length() + 2, e);
    }

    public static String extractBic(String xml, String tag) {
        int s = xml.indexOf("<" + tag + ">");
        int e = xml.indexOf("</" + tag + ">", s);
        if (s < 0 || e < 0) return null;
        return extractTag(xml.substring(s, e), "BICFI");
    }
}
