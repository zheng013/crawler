package com.github.zheng013;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkAndDeleteFromDatabase() throws SQLException;

    Boolean isLinkProcessed(String link) throws SQLException;

    void insertIntoAlreadyProcessedLink(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    void insertIntoToBeProcessedLink(String href) throws SQLException;
}
