package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
import external.TicketMasterAPI;

/**
 * Servlet implementation class SearchItem
 */
@WebServlet("/search")
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }

	/*
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));
		String term = request.getParameter("term");
		String userId = request.getParameter("user_id");
		
		DBConnection connection = DBConnectionFactory.getConnection();
		try {
			List<Item> itemList = connection.searchItems(lat, lon, term);
			Set<String> favoriteItemIds = connection.getFavoriteItemIds(userId);
	
			JSONArray jsonItems = new JSONArray();
			try {
				for (Item item : itemList) {
					JSONObject obj = item.toJSONObject();
					// send back the "favorite" information to the front-end
					obj.put("favorite", favoriteItemIds.contains(item.getItemId()));
					jsonItems.put(obj);
				}
				RpcHelper.writeJsonArray(response, jsonItems);	
			} catch (Exception e) {
				e.printStackTrace();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
