package patent.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import patent.entity.Patent_Info;
import patent.utils.DBOperation;

/**
 *	 下载文件
 * @author 123
 *
 */
public class Download {

	/**
	 *	 下载
	 */
	public static void download(Patent_Info p) {
		
		String downlaodurl = "https://www.lens.org/lens/export?q=classification_ipcr%3A("
				+p.getIpc_code()+"*)&l=en&st=true&dates=%2Bpub_date%3A"+p.getDate().replaceAll("-", "")
				+"-"+p.getDate().replaceAll("-", "")+"&preview=true&n=1000&ef=csv&efn=&undefined=&p=0&async=false";
        
        // 目录是否存在
        String path = "D:/liming/patent/2016";
//        String path = "data/liming/patent";
        File savePath = new File(path);
        if(!savePath.exists()) {
        	savePath.mkdirs();
        }

        InputStream is = null;
        OutputStream out = null;

        DBOperation db = new DBOperation();
        System.out.println(" ===============开始下载============");
        System.out.println("        下载地址: "+downlaodurl);
        try{
        	
        	// 文件是否存在
            String filename = p.getDate()+"-"+p.getIpc_code()+".csv";
            File file = new File(path + "/" + filename);
            if(file.exists()) {
            	db.update(p);
            	return;
            }
            
        	// 定义访问参数
            URL url = new URL(downlaodurl);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestProperty("user-agent", GetParentData.user_agent);
            con.setRequestProperty("cookie",GetParentData.cookie);            
            //设置是否要从 URL 连接读取数据,默认为true
            con.setDoInput(true);
            con.setReadTimeout(60000);
            con.setConnectTimeout(60000);
            
            // 根据响应码作出不同处理
            if(con.getResponseCode()==200) {
            	db.update(p);
            }else {
            	p.setFlag(2);
            	p.setRemarks("下载失败,响应码:"+con.getResponseCode());
            	db.update(p);
            }
            con.connect();

            is = con.getInputStream();
            // 创建字节输出流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bs = new byte[4*1024];
            int len;
            while ((len = is.read(bs)) != -1) {
                byteArrayOutputStream.write(bs, 0, len);
            }
            // 通过传入的文件路径进行写文件的操作
            out = new FileOutputStream(file);
            // 将字节输出流数据写入文件输出流
            out.write(byteArrayOutputStream.toByteArray());
            System.out.println("**************下载结束**************\r\n");
            
            out.flush();
            is.close();
            out.close();
            
        }catch (IOException e){
        	System.out.println("**************下载失败**************\r\n");
            try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
        }
	}
}
