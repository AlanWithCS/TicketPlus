package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import entity.Item;
import entity.Item.ItemBuilder;


public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "khQT3g9FpGWKCdQ1MRXVcGSjzWGLy9dM";
	
	// constant keys to get the value of JSON events
	private static final String EMBEDDED = "_embedded";
	private static final String EVENTS = "events";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String URL_STR = "url";
	private static final String RATING = "rating";
	private static final String DISTANCE = "distance";
	private static final String VENUES = "venues";
	private static final String ADDRESS = "address";
	private static final String LINE1 = "line1";
	private static final String LINE2 = "line2";
	private static final String LINE3 = "line3";
	private static final String CITY = "city";
	private static final String IMAGES = "images";
	private static final String CLASSIFICATIONS = "classifications";
	private static final String SEGMENT = "segment";

	// return the result of json array of with parameters of lat, lon, keyword in the format of List<Item>
	public List<Item> search(double lat, double lon, String keyword) {
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			// make the whitespace to %20
			// encode will throw exception, which means that we need to catch the exception
			keyword = URLEncoder.encode(keyword, "UTF-8"); // Rick Sun => Rick%20Sun
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// use the hash function to translate the latitude and longitude
		String geoHash = GeoHash.encodeGeohash(lat, lon, 9);
		
		// build url 
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=50", API_KEY, geoHash, keyword);

	    // send HTTP
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
			connection.setRequestMethod("GET");
			
			int responseCode = connection.getResponseCode();
			System.out.println("Sending 'GET' request to URL: " + URL);
			System.out.println("Response Code: " + responseCode);
		
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder response = new StringBuilder();
			
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			JSONObject obj = new JSONObject(response.toString());
			if (!obj.isNull("_embedded")) {
				JSONObject embbeded = obj.getJSONObject("_embedded");
				return getItemList(embbeded.getJSONArray("events"));
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<Item>();	
	}
	
	// Convert JSONArray to a list of item objects
	private List<Item> getItemList(JSONArray events) {
		List<Item> itemList  = new ArrayList<Item>();
		try {
			for (int i = 0; i < events.length(); i++) {
				JSONObject obj = events.getJSONObject(i);
				ItemBuilder builder = new ItemBuilder();
				if (!obj.isNull(NAME)) {
					builder.setName(obj.getString(NAME));
				}
				if (!obj.isNull(ID)) {
					builder.setItemId(obj.getString(ID));
				}
				if (!obj.isNull(URL_STR)) {
					builder.setUrl(obj.getString(URL_STR));
				}
				if (!obj.isNull(DISTANCE)) {
					builder.setDistance(obj.getDouble(DISTANCE));
				}
				if (!obj.isNull(DISTANCE)) {
					builder.setDistance(obj.getDouble(DISTANCE));
				}
				
				builder.setAddress(getAddress(obj));
				builder.setCategories(getCategories(obj));
				builder.setImageUrl(getImageUrl(obj));
				
				itemList.add(builder.build());
			}
			
			
		}catch (Exception e) {
			System.out.println("Met with exception: "+e);
		}
		
		return itemList;
	}
	
	private String getImageUrl(JSONObject event) {
		try {
			if (!event.isNull(IMAGES)) {
				JSONArray images = event.getJSONArray(IMAGES);
				for (int i = 0; i < images.length(); i++) {
					JSONObject image = images.getJSONObject(i);
					if (!image.isNull(URL_STR)) {
						return image.getString(URL_STR);
					}
				}
			}
		} catch (Exception e) {
			e.getStackTrace();
		}
		return "";
	}
	
	private Set<String> getCategories(JSONObject event) {
		Set<String> categories = new HashSet<String>();
		try {
			if (!event.isNull(CLASSIFICATIONS)) {
				JSONArray classifications = event.getJSONArray(CLASSIFICATIONS);
				for (int i = 0; i < classifications.length(); i++) {
					JSONObject classification = classifications.getJSONObject(i);
					if (!classification.isNull(SEGMENT)) {
						JSONObject segment = classification.getJSONObject(SEGMENT);
						if (!segment.isNull(NAME)) {
							categories.add(segment.getString(NAME));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return categories;
	}
	
	//  {
	//    "name": "laioffer",
              //    "id": "12345",
              //    "url": "www.laioffer.com",
	//    ...
	//    "_embedded": {
	//	    "venues": [
	//	        {
	//		        "address": {
	//		           "line1": "101 First St,",
	//		           "line2": "Suite 101",
	//		           "line3": "...",
	//		        },
	//		        "city": {
	//		        	"name": "San Francisco"
	//		        }
	//		        ...
	//	        },
	//	        ...
	//	    ]
	//    }
	//    ...
	//  }
	// get the first not null address
	private String getAddress(JSONObject event) {		
		try {
			if (!event.isNull(EMBEDDED)) {
				JSONObject embedded = event.getJSONObject(EMBEDDED);
				if (!embedded.isNull(VENUES)) {
					JSONArray venues = embedded.getJSONArray(VENUES);
					for (int i = 0; i < venues.length(); i++) {
						JSONObject venue = venues.getJSONObject(i);
						StringBuilder sb = new StringBuilder();
						if (!venue.isNull(ADDRESS)) {	
							JSONObject address = venue.getJSONObject(ADDRESS);
							if (!address.isNull(LINE1)) {
								sb.append(address.getString(LINE1));
							}
							if (!address.isNull(LINE2)) {
								sb.append(address.getString(LINE2));
							}
							if (!address.isNull(LINE3)) {
								sb.append(address.getString(LINE3));
							}		
						}
						if (!venue.isNull(CITY)) {
							JSONObject city = venue.getJSONObject(CITY);
							if (!city.isNull(NAME)) {
								sb.append(city.getString(NAME));
							}
						}
						if (sb.length() > 0) return sb.toString();
					}
				}
			}
		} catch (Exception e) {
			e.getStackTrace();
		}
		return "";
	}

	// debug part
	private void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, null);
		try {
			for (int i = 0; i < itemList.size(); ++i) {
				Item item = itemList.get(i);
				System.out.println(item.toJSONObject());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}

	
}
