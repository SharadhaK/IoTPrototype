package temphumid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Servlet implementation class trackTH
 */
@WebServlet("/trackTH")
public class trackTH extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String deviceID = "4812e758-e062-4e0b-baca-ab143b4dd131";

       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public trackTH() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().println("Temperature from device " + getTempfromDevice(deviceID) + "--"
				+ getDevicename(deviceID));
        
		//** Temperature Alert*//
		Double temp = getTempfromDevice(deviceID);
		if (checkTemp(temp)) {

			// 1. Get device name from where the alert has come
			String devicename = getDevicename(deviceID);
			// 2.trigger alert process
			String instanceID = triggerTempAlertProcess(devicename, temp);
			response.getWriter().println("Temperature alert process triggered with instance " + instanceID);
		} else {
			response.getWriter().println("Temperature under control " + getTempfromDevice(deviceID));
		}
		
		//** Temperature Alert*//
		Double humid = getHumidityfromDevice(deviceID);
		if (checkHumidity(humid)) {
			// 1. Get device name from where the alert has come
			String devicename = getDevicename(deviceID);
			// 2.trigger alert process
			String instanceID = triggerHumidityAlertProcess(devicename,humid);
			response.getWriter().println("Humidity alert process triggered with instance " + instanceID);
		} else {
			response.getWriter().println("Humidity under control");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	/**
	 * read data from device
	 * 
	 * @return
	 */
	private Double getTempfromDevice(String deviceid) {
		InitialContext ctx = null;
		DataSource ds = null;
		Connection con = null;
		Double result = null;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/default");
			con = ds.getConnection();
			PreparedStatement query = con.prepareStatement(
					"SELECT  TOP 1 \"C_TEMPERATURE\" FROM \"SYSTEM\".\"T_TH_IOTMESSAGES\" WHERE \"G_DEVICE\" = '"+deviceid+"' ORDER BY \"G_CREATED\" desc");
			ResultSet rs = query.executeQuery();
			while (rs.next()) {
				result = rs.getDouble(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;

	}
	
	/**
	 * read data from device
	 * 
	 * @return
	 */
	private Double getHumidityfromDevice(String deviceid) {
		InitialContext ctx = null;
		DataSource ds = null;
		Connection con = null;
		Double result = null;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/default");
			con = ds.getConnection();
			PreparedStatement query = con.prepareStatement(
					"SELECT  TOP 1 \"C_HUMIDITY\" FROM \"SYSTEM\".\"T_TH_IOTMESSAGES\" WHERE \"G_DEVICE\" = '"+deviceid+"' ORDER BY \"G_CREATED\" desc");
			ResultSet rs = query.executeQuery();
			while (rs.next()) {
				result = rs.getDouble(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;

	}
	
	/**
	 * Reads device name for the device id using Remote Device management API
	 * 
	 * @param deviceid
	 * @return
	 */
	private String getDevicename(String deviceid) throws ClientProtocolException, IOException {
		String devicename = null;
		HttpGet httpGet = null;
		String authorizationHeader = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		try {

			httpClient = getHTTPClient();
			String RDMSUrl = "https://iotrdmsiotservices-s0008289464trial.hanatrial.ondemand.com/com.sap.iotservices.dms/v2/api";
			String invokeUrl = RDMSUrl + "/devices/" + deviceID;

			httpGet = new HttpGet(invokeUrl);
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”

			authorizationHeader = "UzAwMDgyODk0NjQ6cEByQGRveDI=";
			httpGet.addHeader("Authorization", "Basic " + authorizationHeader);
			httpGet.addHeader("X-CSRF-Token", "Fetch");
			response = httpClient.execute(httpGet, httpContext);

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			InputStream inputStream = response.getEntity().getContent();
			byte[] data = new byte[1024];
			int length = 0;
			while ((length = inputStream.read(data)) > 0) {
				bytes.write(data, 0, length);
			}
			String respBody = new String(bytes.toByteArray(), "UTF-8");

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonObject = objectMapper.readValue(respBody, JsonNode.class);
			return jsonObject.get("name").asText();

		} finally {
			if (response != null) {
				response.close();
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}
	
	/**
	 * Gets the xsrf token for business rules/workflow
	 * 
	 * @param requestURL
	 * @param client
	 * @param httpContext
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private String getXSRFToken(String requestURL, CloseableHttpClient client, HttpContext httpContext)
			throws ClientProtocolException, IOException {
		HttpGet httpGet = null;
		CloseableHttpResponse response = null;
		String xsrfToken = null;
		String authorizationHeader = null;
		try {
			httpGet = new HttpGet(requestURL);
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”
			authorizationHeader = "UzAwMDgyODk0NjQ6cEByQGRveDI=";
			httpGet.addHeader("Authorization", "Basic " + authorizationHeader);
			httpGet.addHeader("X-CSRF-Token", "Fetch");
			response = client.execute(httpGet, httpContext);
			// Fetch the token from response and return
			Header xsrfTokenheader = response.getFirstHeader("X-CSRF-Token");
			if (xsrfTokenheader != null) {
				xsrfToken = xsrfTokenheader.getValue();
			}
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
			if (response != null) {
				response.close();
			}
		}
		return xsrfToken;
	}
	/**
	 * Checks whether the temp alert message needs to be triggered by reading
	 * the business rules
	 * 
	 * @param temp
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Boolean checkTemp(Double temp) throws ClientProtocolException, IOException {
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHTTPClient();
			String rulesRuntimeUrl = "https://bpmrulesruntimebpm-s0008289464trial.hanatrial.ondemand.com/";
			String xsrfTokenUrl = rulesRuntimeUrl + "rules-service/v1/rules/xsrf-token";
			String invokeUrl = rulesRuntimeUrl
					+ "rules-service/v1/rules/invoke?rule_service_name=TemperatureHumidityCheck::TExceededService";
			httpPost = new HttpPost(invokeUrl);

			httpPost.addHeader("Content-type", "application/json");
			String xsrfToken = getXSRFToken(xsrfTokenUrl, httpClient, httpContext);
			if (xsrfToken != null) {
				httpPost.addHeader("X-CSRF-Token", xsrfToken);
			}
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”
			String authorizationHeader = "UzAwMDgyODk0NjQ6cEByQGRveDI=";
			httpPost.addHeader("Authorization", "Basic " + authorizationHeader);

			// construct input data to the rules service
			String fact = "{ \"__type__\":\"measures\",\"temperature\":" + temp + ",\"humidity\":" + 50 + "}";
			StringEntity stringEntity = new StringEntity(fact);
			httpPost.setEntity(stringEntity);

			response = httpClient.execute(httpPost, httpContext);
			// process your response here
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			InputStream inputStream = response.getEntity().getContent();
			byte[] data = new byte[1024];
			int length = 0;
			while ((length = inputStream.read(data)) > 0) {
				bytes.write(data, 0, length);
			}
			String respBody = new String(bytes.toByteArray(), "UTF-8");
			// The respBody is a JSON and parse is to get discount
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonObject = objectMapper.readValue(respBody, JsonNode.class);
			return jsonObject.get("exceeded").asBoolean();
		} finally {
			if (httpPost != null) {
				httpPost.releaseConnection();
			}
			if (response != null) {
				response.close();
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}
	

	/**
	 * Checks whether the humidity alert message needs to be triggered by
	 * reading the business rules
	 * 
	 * @param humidity
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Boolean checkHumidity(Double humidity) throws ClientProtocolException, IOException {
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHTTPClient();
			String rulesRuntimeUrl = "https://bpmrulesruntimebpm-s0008289464trial.hanatrial.ondemand.com/";
			String xsrfTokenUrl = rulesRuntimeUrl + "rules-service/v1/rules/xsrf-token";
			String invokeUrl = rulesRuntimeUrl
					+ "rules-service/v1/rules/invoke?rule_service_name=TemperatureHumidityCheck::HExceededService";
			httpPost = new HttpPost(invokeUrl);

			httpPost.addHeader("Content-type", "application/json");
			String xsrfToken = getXSRFToken(xsrfTokenUrl, httpClient, httpContext);
			if (xsrfToken != null) {
				httpPost.addHeader("X-CSRF-Token", xsrfToken);
			}
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”
			String authorizationHeader = "UzAwMDgyODk0NjQ6cEByQGRveDI=";
			httpPost.addHeader("Authorization", "Basic " + authorizationHeader);

			// construct input data to the rules service
			String fact = "{ \"__type__\":\"measures\",\"temperature\":" + 100 + ",\"humidity\":" + humidity + "}";
			StringEntity stringEntity = new StringEntity(fact);
			httpPost.setEntity(stringEntity);

			response = httpClient.execute(httpPost, httpContext);
			// process your response here
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			InputStream inputStream = response.getEntity().getContent();
			byte[] data = new byte[1024];
			int length = 0;
			while ((length = inputStream.read(data)) > 0) {
				bytes.write(data, 0, length);
			}
			String respBody = new String(bytes.toByteArray(), "UTF-8");
			// The respBody is a JSON and parse is to get discount
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonObject = objectMapper.readValue(respBody, JsonNode.class);
			return jsonObject.get("exceeded").asBoolean();
		} finally {
			if (httpPost != null) {
				httpPost.releaseConnection();
			}
			if (response != null) {
				response.close();
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}
	/**
	 * Checks whether the temp alert message needs to be triggered by reading
	 * the business rules
	 * 
	 * @param temp
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String triggerTempAlertProcess(String devicename, Double temp) throws ClientProtocolException, IOException {
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHTTPClient();
			String rulesRuntimeUrl = "https://bpmworkflowruntimewfs-s0008289464trial.hanatrial.ondemand.com/";
			String xsrfTokenUrl = rulesRuntimeUrl + "workflow-service/rest/v1/xsrf-token";
			String invokeUrl = rulesRuntimeUrl + "workflow-service/rest/v1/workflow-instances";
			httpPost = new HttpPost(invokeUrl);

			httpPost.addHeader("Content-type", "application/json");
			String xsrfToken = getXSRFToken(xsrfTokenUrl, httpClient, httpContext);
			if (xsrfToken != null) {
				httpPost.addHeader("X-CSRF-Token", xsrfToken);
			}
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”
			String authorizationHeader = "UzAwMDgyODk0NjQ6cEByQGRveDI=";
			httpPost.addHeader("Authorization", "Basic " + authorizationHeader);

			// construct input data to the workflow service to create instance
			String fact = "{ \"definitionId\":\"temperaturealert\",\"context\":{ \"devicename\":\"" + devicename
					+ "\",\"text\":\"Temperature has risen above the set limit. Please check the device\"}}";

			StringEntity stringEntity = new StringEntity(fact);
			httpPost.setEntity(stringEntity);

			response = httpClient.execute(httpPost, httpContext);
			// process your response here
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			InputStream inputStream = response.getEntity().getContent();
			byte[] data = new byte[1024];
			int length = 0;
			while ((length = inputStream.read(data)) > 0) {
				bytes.write(data, 0, length);
			}
			String respBody = new String(bytes.toByteArray(), "UTF-8");
			// The respBody is a JSON and parse is to get discount
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonObject = objectMapper.readValue(respBody, JsonNode.class);
			return jsonObject.get("id").asText();

		} finally {
			if (httpPost != null) {
				httpPost.releaseConnection();
			}
			if (response != null) {
				response.close();
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}
	

	/**
	 * Checks whether the humidity alert message needs to be triggered by
	 * reading the business rules
	 * 
	 * @param temp
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String triggerHumidityAlertProcess(String devicename, Double humidity)
			throws ClientProtocolException, IOException {
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHTTPClient();
			String rulesRuntimeUrl = "https://bpmworkflowruntimewfs-s0008289464trial.hanatrial.ondemand.com/";
			String xsrfTokenUrl = rulesRuntimeUrl + "workflow-service/rest/v1/xsrf-token";
			String invokeUrl = rulesRuntimeUrl + "workflow-service/rest/v1/workflow-instances";
			httpPost = new HttpPost(invokeUrl);

			httpPost.addHeader("Content-type", "application/json");
			String xsrfToken = getXSRFToken(xsrfTokenUrl, httpClient, httpContext);
			if (xsrfToken != null) {
				httpPost.addHeader("X-CSRF-Token", xsrfToken);
			}
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”
			String authorizationHeader = "UzAwMDgyODk0NjQ6cEByQGRveDI=";
			httpPost.addHeader("Authorization", "Basic " + authorizationHeader);

			// construct input data to the workflow service to create instance
			String fact = "{ \"definitionId\":\"pressurealertprocess\",\"context\":{ \"devicename\":\"" + devicename
					+ "\",\"text\":\"Pressure is not with in the operating limits. Please check the device\"}}";

			StringEntity stringEntity = new StringEntity(fact);
			httpPost.setEntity(stringEntity);

			response = httpClient.execute(httpPost, httpContext);
			// process your response here
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			InputStream inputStream = response.getEntity().getContent();
			byte[] data = new byte[1024];
			int length = 0;
			while ((length = inputStream.read(data)) > 0) {
				bytes.write(data, 0, length);
			}
			String respBody = new String(bytes.toByteArray(), "UTF-8");
			// The respBody is a JSON and parse is to get discount
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonObject = objectMapper.readValue(respBody, JsonNode.class);
			return jsonObject.get("id").asText();

		} finally {
			if (httpPost != null) {
				httpPost.releaseConnection();
			}
			if (response != null) {
				response.close();
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}
	/**
	 * 
	 * @return
	 */
	private CloseableHttpClient getHTTPClient() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		// If you are behind proxy, put your proxy details here
		// Example:
		// clientBuilder.setProxy(new HttpHost(<proxy-host>, <proxy-port>));
		CloseableHttpClient httpClient = clientBuilder.build();
		return httpClient;
	}


}
