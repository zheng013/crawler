package com.github.zheng013;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public static void main(String[] args) throws SQLException {
        MyBatisCrawlerDao myBatisCrawlerDao = new MyBatisCrawlerDao();
        System.out.println(myBatisCrawlerDao.getNextNewsLink());
    }

    public MyBatisCrawlerDao() {
        String resource = "db/mybatis/config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String getNextNewsLink() throws SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return (String) sqlSession.selectOne("com.github.zheng013.MyMapper.selectNextLink");
        }
    }

    @Override
    public String getNextLinkAndDeleteFromDatabase() throws SQLException {
        String link = null;
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            link = (String) sqlSession.selectOne("com.github.zheng013.MyMapper.selectNextLink");
            if (link != null) {
                sqlSession.delete("com.github.zheng013.MyMapper.deleteLink", link);
            }
        }
        return link;
    }

    @Override
    public void insertIntoAlreadyProcessedLink(String link) throws SQLException {
        HashMap map = new HashMap<>();
        map.put("tableName", "links_already_processed");
        map.put("link", link);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.zheng013.MyMapper.insertLink", map);
        }
    }

    @Override
    public void insertIntoToBeProcessedLink(String href) throws SQLException {
        HashMap map = new HashMap<>();
        map.put("tableName", "links_to_be_processed");
        map.put("link", href);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.zheng013.MyMapper.insertLink", map);
        }
    }

    @Override
    public Boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            int count = sqlSession.selectOne("com.github.zheng013.MyMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.zheng013.MyMapper.insertNews", new News(title, content, link));
        }
    }
}
