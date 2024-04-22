package org.aurora.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.aurora.exception.OrderBusinessException;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Slf4j
public class GetDistance {
    @Value("${aurora.gaode.key}")
    private static String key;

//    public static void main(String[] args) {
//        //1根据经纬度查询距离的
//        //注意:高德最多取小数点后六位//格式:经度,纬度
//        String origin = "108.960747" + "," + "34.266451";//陕西省西安市新城区
//        String destination = "117.150738" + "," + "39.138203";//天津市南开区
//        String distance1 = distance(origin, destination);
//        log.info("距离为:{}", distance1);
//        //2根据地址查询距离的
//        String start = "北京市海淀区西三旗永泰庄路甲6号一层 ";
//        String end = "北京市海淀区西三旗北京信息科技大学";
//        String startLonLat = getLonLat(start);//获取到开始地址的经纬度
//        String endLonLat = getLonLat(end);//获取到达地址的经纬度
//        String distance2 = distance(startLonLat, endLonLat);
//        log.info("距离为:{}", distance2);
//    }

    public static String checkOutOfRange(String shopAddress, String userAddress) {
        // 获取店铺经纬度坐标
        String shopLngLat = getLonLat(shopAddress);

        // 获取用户收货地址的经纬度坐标
        String userLngLat = getLonLat(userAddress);

        // 调用距离计算方法，获取店铺到用户地址的距离
        String distance = distance(shopLngLat, userLngLat);

        // 转换距离为整数
        int distanceInt = Integer.parseInt(distance);

        // 如果距离超过5000米，则抛出异常
        if (distanceInt > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }

        return distance;
    }

    // 调用高德地图API获取两点间距离
    public static String distance(String origins, String destination) {
        int type = 3;
        String url = "https://restapi.amap.com/v3/distance?"
                + "origins=" + origins
                + "&destination=" + destination
                + "&output=json"
                + "&type=" + type
                + "&key=" + key;

        String jsonResult = getLoadJson(url);
        System.out.println("jsonResult " + jsonResult);
        JSONObject jsonObject = JSON.parseObject(jsonResult);
        JSONArray results = jsonObject.getJSONArray("results");
        JSONObject result = results.getJSONObject(0);
        return result.getString("distance"); // 获取距离信息
    }

    // 调用高德地图API获取地址的经纬度坐标
    public static String getLonLat(String address) {
        try {
            address = URLEncoder.encode(address, "UTF-8"); // 对地址进行编码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "https://restapi.amap.com/v3/geocode/geo?"
                + "address=" + address
                + "&output=json"
                + "&key=" + key;
        System.out.println(" url " + url);
        String jsonResult = getLoadJson(url);
        JSONObject jsonObject = JSON.parseObject(jsonResult);
        JSONArray geocodes = jsonObject.getJSONArray("geocodes");
        JSONObject geocode = geocodes.getJSONObject(0);
        return geocode.getString("location"); // 获取经纬度坐标
    }

    // 发送HTTP GET请求并获取响应
    public static String getLoadJson(String url) {
        StringBuilder json = new StringBuilder();
        System.out.println("url " + url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet httpGet = new HttpGet(url);
            // 设置头部信息
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537");
            httpGet.setHeader("Accept-Language", "en-US,en;q=0.5");
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                String responseString = EntityUtils.toString(httpEntity);
                json.append(responseString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return json.toString();
    }

//    public static String distance(String origin, String destination) {
//        int type = 1;
//
//        String url = "http://restapi.amap.com/v3/distance?"
//                + "origins=" + origin
//                + "&destination=" + destination
//                + "&type=" + type
//                + "&key=" + key;
//        StringBuilder json = new StringBuilder();
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        try {
//            HttpGet httpGet = new HttpGet(url);
//            HttpResponse httpResponse = httpClient.execute(httpGet);
//            HttpEntity httpEntity = httpResponse.getEntity();
//            if (httpEntity != null) {
//                String responseString = EntityUtils.toString(httpEntity);
//                json.append(responseString);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                httpClient.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        Gson gson = new Gson();
//        Map map = gson.fromJson(json.toString(), Map.class);
//        List list = (List) map.get("results");
//        Map<String, String> map1 = (Map) list.get(0);
//        String distance = map1.get("distance");
//        return distance;
//    }
//
//    public static String getLoadJson(String url) {
//        StringBuilder json = new StringBuilder();
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        try {
//            HttpGet httpGet = new HttpGet(url);
//            HttpResponse httpResponse = httpClient.execute(httpGet);
//            HttpEntity httpEntity = httpResponse.getEntity();
//            if (httpEntity != null) {
//                String responseString = EntityUtils.toString(httpEntity);
//                json.append(responseString);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                httpClient.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return json.toString();
//    }
//
//    public static String getLonLat(String address) {
//        String url = "http://restapi.amap.com/v3/geocode/geo?"
//                + "address=" + address
//                + "&key=" + key;
//        StringBuilder json = new StringBuilder();
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        try {
//            HttpGet httpGet = new HttpGet(url);
//            HttpResponse httpResponse = httpClient.execute(httpGet);
//            HttpEntity httpEntity = httpResponse.getEntity();
//            if (httpEntity != null) {
//                String responseString = EntityUtils.toString(httpEntity);
//                json.append(responseString);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                httpClient.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        JSONObject jsonObject = JSONObject.fromObject(json.toString());
//        JSONArray geocodesArray = jsonObject.getJSONArray("geocodes");
//        JSONObject geocodesObject = geocodesArray.getJSONObject(0);
//        return geocodesObject.getString("location");
//    }
}
