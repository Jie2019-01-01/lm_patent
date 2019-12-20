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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import patent.entity.IpcDict;
import patent.entity.Patent_Info;
import patent.utils.DBOperation;

public class GetParentData {

	static String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
	static String cookie = "__uzma=18ed9e9a-899b-8e7b-aa83-84c88318bd63; __uzmb=1576658704; LENS_SESSION_ID=7B871117169F8F09F84E0970EE151323; _pk_id.1.2a81=660327ac3d057366.1576658707.8.1576805371.1576805371.; _pk_ses.1.2a81=1; TZ=Asia%2FShanghai; __uzmc=7937488398428; uzdbm_a=3ba078e0-9434-5342-c361-f5ee6141d9f4; __uzmd=1576805429; AWSALB=Vr2BJT2Il9itt+H/vgzwaCuzVx7rxMfvLuws3Lj3tWyQbYeAk0PIv9oVl7sXeqC0J6VFhAfVLHwwMmege7OOpfD+5PtjSGhpVCxtF2W2uJfTPJIJmv5346bMdHID";
	private static DBOperation db = new DBOperation();	
	
	public static void main(String[] args) {
		// 定义时间范围
		Calendar c = Calendar.getInstance();
        String startTime = "2014-01-01"; // 起始时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String endTime = "2015-01-01"; // 结束时间
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
		// 查询一级分类
		find_childNode("[NULL]", date);
	}
	
	public static void find_childNode(String upper_code, String date) {
		
		List<IpcDict> ipc_list = db.getLevel_1(upper_code);
		if(ipc_list.size()>0) {
			for(IpcDict ipc: ipc_list) {
				int count = db.alreadyExists(date, ipc.getIpc_code());
				// 已经存在的记录不用继续查询
				if(count==0) {
					Patent_Info patent = new Patent_Info(ipc.getIpc_code(),ipc.getIpc_level(),ipc.getIpc_level(),date);
					int result = query(patent);
					patent.setRow_count(result);
					if(result>1000) {
						patent.setRemarks("数量大于1000");
						db.addPatentInfo(patent);
						System.out.println("日期：" + date + "----IPC_CODE：" + ipc.getIpc_code() + "===数量大于1000，结果数量：" + result);
						// 递归找子
						find_childNode(ipc.getIpc_code(), date);
			    	}else if(result>0&&result<=1000){
			    		patent.setRemarks("下载成功");
			    		db.addPatentInfo(patent);
			    		System.out.println("日期：" + date + "----IPC_CODE：" + ipc.getIpc_code() + "===下载成功，结果数量：" + result);
			    	}else if(result==0){
			    		patent.setRemarks("结果为0");
			    		db.addPatentInfo(patent);
			    		System.out.println("日期：" + date + "----IPC_CODE：" + ipc.getIpc_code() + "===结果为0");
			    	}else {
			    		System.out.println("频率过快，程序停止");
			    	}
				}
			}
		}else {
			System.out.println("IPC:" + upper_code + "---该分类下没有对应的子级");
		}
	}
	
	/**
	 *	 抓取指定日期中的数据
	 */
	public static int query(Patent_Info patent) {

        // 访问列表页的url
        String url = "https://www.lens.org/lens/search/facets?q=classification_ipcr:("
        		+ patent.getIpc_code()+"*)&l=en&st=true&dates=%2Bpub_date:" + patent.getDate().replaceAll("-", "")
        		+ "-"+patent.getDate().replaceAll("-", "") + "&preview=true&facets=cites_resolved_scholarly";
        
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
	        // 防止频率过快，每次查询结束都暂停10秒
	        Thread.sleep(30000);
		} catch (Exception e) {
			result = -1;
			e.printStackTrace();
		} 
        return result;
	}
}
