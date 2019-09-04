package patent.httpclient;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import patent.entity.Patent_Info;
import patent.utils.DBOperation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class GetParentData {

	static String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
	static String cookie = "__uzma=7aecbff9-8b85-9919-83a7-9e3b99a98d07; __uzmb=1567502787; LENS_SESSION_ID=E1AD5F14F5B581F8289A63E748698D04; TZ=Asia%2FShanghai; _pk_ses.1.2a81=1; _pk_id.1.2a81=717770ecfee259e5.1567502789.3.1567562236.1567560728.; __uzmd=1567562237; __uzmc=7208628966230; uzdbm_a=a9bacc48-9434-df91-ceb0-24674395b0c8; AWSALB=RGNI5Or/G+5DvCmklXKZqCetSrCa7fMY5YjJSrlVksGiTQSY2fo/J/+wquCWE6Kf4J+6Z0X7ZHUXnswAf7hPC90EqecSaNAxyK6NqLln8AGIa9FUEOBweS+y1Z1L";
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	public static void main(String[] args) {
		// 定义时间范围
		Calendar c = Calendar.getInstance();
        String startTime = "2016-01-01"; // 起始时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String endTime = "2017-01-01"; // 结束时间
        
        try {
	        // 以天为单位进行查询
	        while(!startTime.equals(endTime)) {
	        	execute(startTime);
	        	// 每抓取一天的数据后+1
	            Date date = format.parse(startTime);
	            c.setTime(date);
	            c.add(Calendar.DAY_OF_MONTH, 1);
	            startTime = format.format(c.getTime());
	        }
        } catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 该方法实现各个方法的组装
	 */
	public static void execute(String date) {		
		DBOperation db = new DBOperation();	
		// 定义level，一共3级
		for(int i=1;i<=3;i++) {
			// 以日期和level为条件查询当天的数据
			List<Patent_Info> patents = db.getPatentInfo(date,String.valueOf(i));
			for(Patent_Info p: patents) {
				int result = query(p);
				p.setRow_count(result);
				
				if(result>1000) {
					
		        	// 更新分类标识
					p.setFlag(3);
					p.setRemarks("数量大于1000");
		        	db.update(p);
		        	
		    	}else if(result>0&&result<=1000){
		    		
		    		p.setFlag(1);
		    		p.setRemarks("下载成功");
		    		// 删除该分类其下的子类
		    		db.delete(p); // 删除的是否有问题？
		    		Download.download(p); // 看看有没有下载的参数
		    		try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    		
		    	}else {
		    		p.setFlag(1);
		    		p.setRemarks("结果为0");
		    		db.update(p);
		    		db.delete(p);
		    	}
			}
		}
	}
	
	/**
	 *	 抓取指定日期中的数据
	 */
	public static int query(Patent_Info p) {

        // 访问列表页的url
        String url = "https://www.lens.org/lens/search/facets?q=classification_ipcr:("
        		+p.getIpc_code()+"*)&l=en&st=true&dates=%2Bpub_date:"+p.getDate().replaceAll("-", "")
        		+"-"+p.getDate().replaceAll("-", "")+"&preview=true&facets=cites_resolved_scholarly";
        
        BasicCookieStore cookieStore = new BasicCookieStore();
        // 创建HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
        		.setDefaultCookieStore(cookieStore)
        		.build();
        HttpGet httpGet = null;
        CloseableHttpResponse response = null;
        RequestConfig defaultConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
        
        
        int result = 0;
        // 定义请求信息
        try {
			httpGet = new HttpGet(new URI(url));
			httpGet.addHeader("user-agent",user_agent);
			httpGet.addHeader("cookie", cookie);
			httpGet.setConfig(defaultConfig);
	        response = httpClient.execute(httpGet);
	        // 得到html字符串
	        HttpEntity entity = response.getEntity();
	        String content = EntityUtils.toString(entity);
	        JSONObject jsonObject = JSON.parseObject(content);
	        result = (int) jsonObject.get("resultSize");
	        System.out.println("["+dateFormat.format(new Date())+"] "+url+"\t  === 数量: "+result);
	        // 防止频率过快，每次查询结束都暂停10秒
	        Thread.sleep(10000);
	        
		} catch (Exception e) {
			e.printStackTrace();
		} 
        return result;
	}
}
