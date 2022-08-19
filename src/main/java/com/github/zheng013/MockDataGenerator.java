package com.github.zheng013;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    private static final int TARGET_ROW_COUNT = 100_0000;

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        String resource = "db/mybatis/config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mockData(sqlSessionFactory);

    }

    public static void mockData(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> newsList = sqlSession.selectList("com.github.zheng013.MockMapper.selectNews");
            int count = TARGET_ROW_COUNT - newsList.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(newsList.size());
                    News news = new News(newsList.get(index));
                    Instant currentCreatedAt = news.getCreatedAt();
                    currentCreatedAt = currentCreatedAt.minusSeconds(new Random().nextInt(3600 * 24 * 365));
                    news.setCreatedAt(currentCreatedAt);
                    news.setModifiedAt(currentCreatedAt);
                    sqlSession.insert("com.github.zheng013.MockMapper.insertNews", news);
                    if (count % 2000 == 0) {
                        System.out.println("left  " + count);
                        sqlSession.flushStatements();
                    }
                }
                sqlSession.commit();
            } catch (Exception e) {
                sqlSession.rollback();
                throw new RuntimeException(e);
            }
        }
    }
}
