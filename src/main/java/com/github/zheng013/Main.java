package com.github.zheng013;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static final DatabaseAccessObject dao = new DatabaseAccessObject();

    public static void main(String[] args) throws IOException, SQLException {
        //从数据库中加载即将处理的链接
        String link = null;
        while ((link = dao.getNextLinkAndDeleteFromDatabase()) != null) {
            if (link.startsWith("//")) {
                link = "https:" + link;
            }
            //判断链接是否被处理过  通过读取数据判断当前链接是否被处理过
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            //判断是否为我们想要处理的链接 暂时只处理 news.sina.cn 并且过滤掉一些不为新闻的链接
            if (isInterestingLink(link)) {
                //只有我们感兴趣的才处理他
                // 获取可用的a标签的href放入到链接池linkPool中
                Document doc = httpGetAndHtmlParse(link);
                parseUrlsFromPageStoreIntoDatabase(doc);
                //如果是一个新闻详情的界面 就存入数据库 否则什么都不做
                storeLinkIntoDatabaseIfIsNewsPage(doc, link);
                System.out.println(link);
                dao.updateDatabase(link, "insert into links_already_processed(link) values(?)");
            }
        }
    }

    private static void storeLinkIntoDatabaseIfIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        for (Element articleTag : articleTags) {
            //通过分析新闻的详情界面，分析接口并获取相关标题文本数据
            String title = articleTag.child(0).text();
            List<Element> paragraphs = articleTag.select("p");
            String content = paragraphs.stream().map(Element::text).collect(Collectors.joining("\n"));
            dao.insertNewsIntoDatabase(link, title, content);
        }
    }

    private static void parseUrlsFromPageStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            dao.updateDatabase(href, "insert into links_to_be_processed(link) values(?)");
        }
    }

    private static Document httpGetAndHtmlParse(String link) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        // user-agent 代理添加表明为浏览器访问
        httpGet.setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        System.out.println(link);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            System.out.println(response.getStatusLine());
            HttpEntity responseEntity = response.getEntity();
            String htmlString = EntityUtils.toString(responseEntity);
            return Jsoup.parse(htmlString);

        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) && isNotLoginPage(link)) || isIndexPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }
}
