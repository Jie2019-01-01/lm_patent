package patent.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import patent.entity.Patent_Info;

/**
 * 数据库操作,包含CRUD
 * @author liuweijie
 *
 */
public class DBOperation {
	
	/**
	 * 获取数据库专利数据(by日期,level)
	 */
	public List<Patent_Info> getPatentInfo(String date,String level) {
		
		List<Patent_Info> list = new ArrayList<Patent_Info>();
		
		// 获取连接
		Connection con = DBUtilLocal.getConnection();
		String sql = "select IPC_CODE,IPC_LEVEL,UPPER_CODE from patent_info where DATE=? AND flag IN(0) AND IPC_LEVEL=?;";
		
		PreparedStatement statement = null;
		try {
			statement = con.prepareStatement(sql);
			statement.setString(1, date);
			statement.setString(2, level);
			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next()) {
				Patent_Info p = new Patent_Info();
				p.setDate(date);
				p.setIpc_code(resultSet.getString("IPC_CODE"));
				p.setIpc_level(resultSet.getString("IPC_LEVEL"));
				p.setUpper_code(resultSet.getString("UPPER_CODE"));
				list.add(p);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	/**
	 * 	更新数据库的标识
	 */
	public void update(Patent_Info p) {
		try {
			
			// 获取连接
			Connection con = DBUtilLocal.getConnection();
			String sql = "update patent_info set flag=?,REMARKS=?,row_count=? where IPC_CODE=? and DATE=?";
			
			PreparedStatement statement = null;
			
			statement = con.prepareStatement(sql);
			statement.setInt(1, p.getFlag());
			statement.setString(2, p.getRemarks());
			statement.setInt(3, p.getRow_count());
			statement.setString(4, p.getIpc_code());
			statement.setString(5, p.getDate());
			statement.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 	级联删除
	 */
	public void delete(Patent_Info p) {
		// 获取连接
		Connection con = DBUtilLocal.getConnection();
		String sql = "delete from patent_info where DATE=? and IPC_CODE IN("
				+ "select i1.IPC_CODE from ipc_dict i1 where i1.UPPER_CODE=?" + 
				"	UNION" + 
				" select i2.IPC_CODE from ipc_dict i1 LEFT JOIN ipc_dict i2 on i1.IPC_CODE=i2.UPPER_CODE where i1.UPPER_CODE=? and i2.IPC_LEVEL=3"
				+ ")";
				
		PreparedStatement statement = null;
		
		try {
			statement = con.prepareStatement(sql);
			statement.setString(1, p.getDate());
			statement.setString(2, p.getIpc_code());
			statement.setString(3, p.getIpc_code());
			statement.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
