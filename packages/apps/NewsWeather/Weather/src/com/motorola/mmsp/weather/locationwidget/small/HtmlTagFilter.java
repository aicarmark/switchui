package com.motorola.mmsp.weather.locationwidget.small;

public class HtmlTagFilter {
	public static String removeHtmlTag(String htmlString) {
		StringBuilder sBuilder = new StringBuilder(htmlString);
		for (int startIndex = sBuilder.indexOf("<"), startIndex2 = sBuilder.indexOf("&"); startIndex >= 0
				|| startIndex2 >= 0; startIndex = sBuilder.indexOf("<"), startIndex2 = sBuilder.indexOf("&")) {
			if (startIndex >= 0 && sBuilder.indexOf(">", startIndex) >= 0) {
				if (startIndex > 0 && sBuilder.charAt(startIndex - 1) != ' ') {
					sBuilder.insert(startIndex, ' ');
					startIndex++;
				}
				sBuilder.delete(startIndex, sBuilder.indexOf(">", startIndex) + 1);
				continue;
			}
			if (startIndex2 >= 0 && sBuilder.indexOf(";", startIndex2) >= 0) {
				if (startIndex2 > 0 && sBuilder.charAt(startIndex2 - 1) != ' ') {
					sBuilder.insert(startIndex2, ' ');
					startIndex2++;
				}
				sBuilder.delete(startIndex2, sBuilder.indexOf(";", startIndex2) + 1);
				continue;
			}
		}
		return sBuilder.toString();
	}
}
