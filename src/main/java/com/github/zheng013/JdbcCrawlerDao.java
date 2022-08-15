package com.github.zheng013;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String user = "root";
    private static final String password = "root";
    private final Connection connection;
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:C:\\Users\\gyenno\\Desktop\\crawler\\news", user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void insertIntoAlreadyProcessedLink(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into links_already_processed(link) values(?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    @Override
    public void insertIntoToBeProcessedLink(String href) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into links_to_be_processed(link) values(?)")) {
            statement.setString(1, href);
            statement.executeUpdate();
        }
    }

    @Override
    public String getNextLinkAndDeleteFromDatabase() throws SQLException {
        String link = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("select link from links_to_be_processed limit 1")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                link = resultSet.getString(1);
            }
        }
        if (null != link) {
            try (PreparedStatement statement = connection.prepareStatement("delete from links_to_be_processed where link = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }
        }
        return link;
        //获取link链接之后需要立刻进行删除。从ArrayList尾部进行删除更有效率 remove删除相关索引值并返回对应的元素
        //处理完链接之后出待处理的数据库池中删除
    }


    @Override
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

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
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
