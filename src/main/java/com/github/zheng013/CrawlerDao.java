package com.github.zheng013;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;

    String getNextLinkAndDeleteFromDatabase() throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    Boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;
}
