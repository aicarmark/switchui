package com.motorola.mmsp.rss.service.parse;

import java.io.InputStream;
import java.util.List;

import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.ItemInfo;

public interface XmlParserInterface {
	public int parseRss(InputStream in, FeedInfo info, List<ItemInfo> infos, boolean bCorrectedDate);
	public boolean preParseRss(InputStream in);
}
