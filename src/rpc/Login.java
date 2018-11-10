package rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;

/**
 * Servlet implementation class Login
 */
@WebServlet("/login")
public class Login extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// Judge if the request has session
		DBConnection connection = DBConnectionFactory.getConnection();
		try {
			HttpSession session = request.getSession(false);
			JSONObject obj = new JSONObject();
			if (session != null) {
				String userId = session.getAttribute("user_id").toString();
				obj.put("result",  "SUCCESS").put("user_id", userId).put("name", connection.getFullname(userId));
			} else {
				response.setStatus(403);
				obj.put("result", "Session Doesn't Exist");
			}
			RpcHelper.writeJsonObject(response, obj);
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
		DBConnection connection = DBConnectionFactory.getConnection();
		try {
			// change the input to the List<String> items
			JSONObject input = RpcHelper.readJSONObject(request);
			String userId = input.getString("user_id");
			String password = input.getString("password");
			
			JSONObject obj = new JSONObject();
			if (connection.verifyLogin(userId, password)) {
				// create session
				HttpSession session = request.getSession(); // create a session
				session.setAttribute("user_id", userId);
				session.setMaxInactiveInterval(600); // set a valid session lasting for 600s
				// debug part
				obj.put("result",  "SUCCESS").put("user_id", userId).put("name", connection.getFullname(userId));
			} else {
				response.setStatus(401);
				obj.put("result", "User Doesn't Exist");
			}
			RpcHelper.writeJsonObject(response, obj);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}
	}
}




