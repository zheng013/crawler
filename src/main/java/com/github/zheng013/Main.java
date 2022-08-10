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

public class Main {
    private static String getNextLink(Connection connection, String sql) throws SQLException {
        String link = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                link = resultSet.getString(1);
            }
        }
        return link;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C:\\Users\\gyenno\\Desktop\\crawler\\news", "root", "root");
        //从数据库中加载即将处理的链接
        while (true) {
            String link = getNextLink(connection, "select link from links_to_be_processed");
            if (null == link) {
                break;
            }
            //获取link链接之后需要立刻进行删除。从ArrayList尾部进行删除更有效率 remove删除相关索引值并返回对应的元素
            //处理完链接之后出待处理的数据库池中删除
            try (PreparedStatement statement = connection.prepareStatement("delete from links_to_be_processed where link = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }
            if (link.startsWith("//")) {
                link = "https:" + link;
            }
            //判断链接是否被处理过  通过读取数据判断当前链接是否被处理过
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            //判断是否为我们想要处理的链接 暂时只处理 news.sina.cn 并且过滤掉一些不为新闻的链接
            if (isInterestingLink(link)) {
                //只有我们感兴趣的才处理他
                // 获取可用的a标签的href放入到链接池linkPool中
                Document doc = httpGetAndHtmlParse(link);
                for (Element aTag : doc.select("a")) {
                    String href = aTag.attr("href");
                    insertLinkIntoDatabase(connection, href, "insert into links_to_be_processed(link) values(?)");
                }
                //如果是一个新闻详情的界面 就存入数据库 否则什么都不做
                storeLinkIntoDatabaseIfIsNewsPage(doc);
                System.out.println(link);
                insertLinkIntoDatabase(connection, link, "insert into links_already_processed(link) values(?)");
            }
        }
    }

    private static Boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select link from links_already_processed where link=?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    private static void storeLinkIntoDatabaseIfIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        for (Element articleTag : articleTags) {
            //通过分析新闻的详情界面，分析接口并获取相关标题文本数据
            String title = articleTag.child(0).text();
            System.out.println(title);
        }
    }

    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
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
