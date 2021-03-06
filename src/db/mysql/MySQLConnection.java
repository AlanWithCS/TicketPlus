package db.mysql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

// This is a singleton pattern.
public class MySQLConnection implements DBConnection {
	private Connection conn;
    
    public MySQLConnection() {
	   	 try {
	   		 Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
	   		 conn = DriverManager.getConnection(MySQLDBUtil.URL);
	   		 
	   	 } catch (Exception e) {
	   		 e.printStackTrace();
	   	 }
    }
    
    @Override
    public void close() {
	   	 if (conn != null) {
	   		 try {
	   			 conn.close();
	   		 } catch (Exception e) {
	   			 e.printStackTrace();
	   		 }
	   	 }
    }
    
	@Override
	// pack serveral items and save them to the favorite
	// When user click "favorite - love", it will not save the favorite immediately
	// instead, when user click "my favorite", the packed favorite itmes will be saved
	public void setFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if (conn == null) { System.err.println("DB Connection failed..."); return; }
		try {
			String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUES (?, ?)";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			for (String itemId : itemIds) {
				ps.setString(2, itemId);
				ps.execute();
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	@Override
	// return true if succedd
	public boolean setNewUser(String userId, String password, String firstName, String lastName) {
		if (conn == null) { System.err.println("DB Connection failed..."); return false; }
		try {
			if (userId == null || password == null) return false;
			if (userId.length() == 0 || password.length() == 0) return false;
			String sql = "SELECT user_id FROM users WHERE user_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			ResultSet rs = stmt.executeQuery();
			// return false if already exists user_id
			if (rs.next()) { 
				System.out.println("userID: "+userId+" already exists");
				return false;}
			
			sql = "INSERT INTO users VALUES(?,?,?,?)";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			stmt.setString(2, password);
			stmt.setString(3, firstName);
			stmt.setString(4, lastName);
			System.out.println("insert success");
			int affectedRows = stmt.executeUpdate();
			if (affectedRows == 1) return true;
			return false;
		} catch (Exception e) {
	   		 e.printStackTrace();
	   	}
		return false;
	}

	@Override
	// the logic is quite similar to the setFavorite items
	// the front end will pack the information and set a delay to set/unset the items, not in-time
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		 if (conn == null) {
	   		 System.err.println("DB connection failed");
	   		 return;
	   	 }
	   	 
	     try {
			 String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
			 PreparedStatement ps = conn.prepareStatement(sql);
			 ps.setString(1, userId);
			 for (String itemId : itemIds) {
				 ps.setString(2, itemId);
				 ps.execute();
			 }
	     } catch (Exception e) {
	    	 e.printStackTrace();
	     }
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}		
		Set<String> favoriteItems = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				favoriteItems.add(itemId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		return favoriteItems;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, itemId);
				
				ResultSet rs = stmt.executeQuery();
				
				ItemBuilder builder = new ItemBuilder();
				
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rs.getDouble("distance"));
					builder.setRating(rs.getDouble("rating"));
					
					favoriteItems.add(builder.build());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItems;
	}


	@Override
	public Set<String> getCategories(String itemId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return null;
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? ";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return categories;
	}

	@Override
	// everytime user search for something, the server will save the result into the database
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub
		TicketMasterAPI ticketMasterAPI = new TicketMasterAPI();
		List<Item> items = ticketMasterAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub
		if (item == null) return;
		if (conn == null) { System.err.println("DB connection failed"); }
		try {
			 // if any parameter is invalid, neglect the whole row
	   		 String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
	   		 PreparedStatement ps = conn.prepareStatement(sql);
	   		 ps.setString(1, item.getItemId());
	   		 ps.setString(2, item.getName());
	   		 ps.setDouble(3, item.getRating());
	   		 ps.setString(4, item.getAddress());
	   		 ps.setString(5, item.getImageUrl());
	   		 ps.setString(6, item.getUrl());
	   		 ps.setDouble(7, item.getDistance());
	   		 ps.execute();
	   		 
	   		 sql = "INSERT IGNORE INTO categories VALUES(?, ?)";
	   		 ps = conn.prepareStatement(sql);
	   		 ps.setString(1, item.getItemId());
	   		 for(String category : item.getCategories()) {
	   			 // ps will be covered, but after execution, the database will not be covered
	   			 // 生成了多条category
	   			 ps.setString(2, category);
	   			 ps.execute();
	   		 }
	   		 
	   	 } catch (Exception e) {
	   		 e.printStackTrace();
	   	 }		
	}

	@Override
	public String getFullname(String userId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return "";
		}
		
		String name = "";
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ? ";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				name = rs.getString("first_name") + " " + rs.getString("last_name");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return false;
		}
		try {
			String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return false;
	}

}
