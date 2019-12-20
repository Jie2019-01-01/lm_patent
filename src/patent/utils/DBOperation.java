package patent.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import patent.entity.IpcDict;
import patent.entity.Patent_Info;

/**
 * 数据库操作,包含CRUD
 * @author liuweijie
 *
 */
public class DBOperation {
	
	/**
	 * 查询一级分类
	 */
	public List<IpcDict> getLevel_1(String ipc_code){
		List<IpcDict> ipc_list = new ArrayList<IpcDict>();
		// 获取连接
		Connection con = DBUtilLocal.getConnection();
		String sql = "select IPC_CODE,IPC_LEVEL,UPPER_CODE from ipc_dict where UPPER_CODE=?;";
		PreparedStatement statement = null;
		try {
			statement = con.prepareStatement(sql);
			statement.setString(1, ipc_code);
			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next()) {
				IpcDict ipc = new IpcDict();
				ipc.setIpc_code(resultSet.getString("IPC_CODE"));
				ipc.setIpc_level(resultSet.getString("IPC_LEVEL"));
				ipc.setUpper_code(resultSet.getString("UPPER_CODE"));
				ipc_list.add(ipc);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ipc_list;
	}
	
	/**
	 * 通过日期和ipc查询记录是否已经存在
	 * @param date
	 * @param ipc_code
	 * @return
	 */
	public int alreadyExists(String date, String ipc_code) {
		int count = 0;
		Connection con = DBUtilLocal.getConnection();
		String sql = "select COUNT(1) as count from patent_his where IPC_CODE=? and DATE=?;";
		PreparedStatement statement = null;
		try {
			statement = con.prepareStatement(sql);
			statement.setString(1, date);
			statement.setString(2, ipc_code);
			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next()) {
				count = Integer.parseInt(resultSet.getString("count"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}
	
	/**
	 * 	添加记录
	 */
	public void addPatentInfo(Patent_Info patent) {
		try {
			// 获取连接
			Connection con = DBUtilLocal.getConnection();
			String sql = "insert patent_info set IPC_CODE=?, IPC_LEVEL=?, UPPER_CODE=?, ROW_COUNT=?, DATE=?, REMARKS=?";
			PreparedStatement statement = null;
			statement = con.prepareStatement(sql);
			statement.setString(1, patent.getIpc_code());
			statement.setString(2, patent.getIpc_level());
			statement.setString(3, patent.getUpper_code());
			statement.setInt(4, patent.getRow_count());
			statement.setString(5, patent.getDate());
			statement.setString(6, patent.getRemarks());
			statement.execute();
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
