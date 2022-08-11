package com.github.zheng013;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseAccessObject {
    private static final String user = "root";
    private static final String password = "root";
    private final Connection connection;

    public DatabaseAccessObject() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:C:\\Users\\gyenno\\Desktop\\crawler\\news", user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public String getNextLink(String sql) throws SQLException {
        String link = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                link = resultSet.getString(1);
            }
        }
        return link;
    }


    public String getNextLinkAndDeleteFromDatabase() throws SQLException {
        String link = getNextLink("select link from links_to_be_processed limit 1");
        if (null != link) {
            updateDatabase(link, "delete from links_to_be_processed where link = ?");
        }
        //获取link链接之后需要立刻进行删除。从ArrayList尾部进行删除更有效率 remove删除相关索引值并返回对应的元素
        //处理完链接之后出待处理的数据库池中删除
        return link;
    }

    public void parseUrlsFromPageStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            updateDatabase(href, "insert into links_to_be_processed(link) values(?)");
        }
    }

    public void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public Boolean isLinkProcessed(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select link from links_already_processed where link=?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    public void storeLinkIntoDatabaseIfIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        for (Element articleTag : articleTags) {
            //通过分析新闻的详情界面，分析接口并获取相关标题文本数据
            String title = articleTag.child(0).text();
            List<Element> paragraphs = articleTag.select("p");
            String content = paragraphs.stream().map(Element::text).collect(Collectors.joining("\n"));
            try (PreparedStatement statement = connection.prepareStatement("insert into news(title,content,url,created_at,modified_at) values(?,?,?,now(),now())")) {
                statement.setString(1, title);
                statement.setString(2, content);
                statement.setString(3, link);
                statement.executeUpdate();
            }
            System.out.println(content);
            System.out.println(title);
        }
    }
}
