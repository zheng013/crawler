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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {

        List<String> linkPool = new ArrayList<String>();
        Set<String> processedLinks = new HashSet<String>();
        linkPool.add("https://sina.cn");
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            //获取link链接之后需要立刻进行删除。从ArrayList尾部进行删除更有效率 remove删除相关索引值并返回对应的元素
            String link = linkPool.remove(linkPool.size() - 1);
            if (link.startsWith("//")) {
                link = "https:" + link;
            }
            //判断链接是否被处理过
            if (processedLinks.contains(link)) {
                continue;
            }
            //判断是否为我们想要处理的链接 暂时只处理 news.sina.cn 并且过滤掉一些不为新闻的链接
            if ((link.contains("news.sina.cn") && !link.contains("passport.sina.cn")) || "https://sina.cn" == link) {

                //只有我们感兴趣的才处理他
                // 获取可用的a标签的href放入到链接池linkPool中
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(link);
                // user-agent 代理添加表明为浏览器访问
                httpGet.setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
                System.out.println(link);
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    System.out.println(response.getStatusLine());
                    HttpEntity responseEntity = response.getEntity();
                    String htmlString = EntityUtils.toString(responseEntity);
                    Document doc = Jsoup.parse(htmlString);
                    ArrayList<Element> aTags = doc.select("a");
                    for (Element aTag : aTags) {
                        linkPool.add(aTag.attr("href"));
                    }
                    //如果是一个新闻详情的界面 就存入数据库 否则什么都不做
                    ArrayList<Element> articleTags = doc.select("article");
                    for (Element articleTag : articleTags) {
                      //通过分析新闻的详情界面，分析接口并获取相关标题文本数据
                        String title = articleTag.child(0).text();
                        System.out.println(title);
                    }
                    processedLinks.add(link);
                }
            }
        }
    }
}
