package stocklevel;

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
 * Servlet implementation class trackStock
 */
@WebServlet("/trackStock")
public class trackStock extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String deviceID = "<device id>";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public trackStock() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/** check Stock level */
		response.getWriter().println(getReorderDetails("Test material name", getMaterialTypefromDevice(deviceID)));
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
	private String getMaterialTypefromDevice(String deviceid) {
		InitialContext ctx = null;
		DataSource ds = null;
		Connection con = null;
		String result = null;
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/default");
			con = ds.getConnection();
			PreparedStatement query = con.prepareStatement(
					"SELECT  TOP 1 \"C_MATERIALTYPE\" FROM \"SYSTEM\".\"T_LS_IOTMESSAGES\" WHERE \"G_DEVICE\" = '"+deviceid+"' ORDER BY \"G_CREATED\" desc");
			ResultSet rs = query.executeQuery();
			while (rs.next()) {
				result = rs.getString(1);
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
	 * Get the reorder type and details
	 * 
	 * @param materialtype
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getReorderDetails(String materialname, String materialtype)
			throws ClientProtocolException, IOException {
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHTTPClient();
			String rulesRuntimeUrl = "https://bpmrulesruntimebpm-s000xxxxxtrial.hanatrial.ondemand.com/";
			String xsrfTokenUrl = rulesRuntimeUrl + "rules-service/v1/rules/xsrf-token";
			String invokeUrl = rulesRuntimeUrl
					+ "rules-service/v1/rules/invoke?rule_service_name=Reorder::ReorderService";
			httpPost = new HttpPost(invokeUrl);

			httpPost.addHeader("Content-type", "application/json");
			String xsrfToken = getXSRFToken(xsrfTokenUrl, httpClient, httpContext);
			if (xsrfToken != null) {
				httpPost.addHeader("X-CSRF-Token", xsrfToken);
			}
			// replace value of authorizationHeader with base64 encoded value of
			// “<user-name>:<password>”
			String authorizationHeader =  "<base64 encoded value of <user-name>:<password>>";
			httpPost.addHeader("Authorization", "Basic " + authorizationHeader);

			// construct input data to the rules service
			String fact = "{ \"__type__\":\"Input\",\"materialtype\":\"" + materialtype + "\"}";
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
			String ordermode = jsonObject.get("ordermode").asText();
			int qty = jsonObject.get("qty").asInt();
			String ordertype = jsonObject.get("ordertype").asText();
			if (ordermode.equalsIgnoreCase("Auto")) {
				return "Order created - Material: " + materialname + ", Order Type: " + ordertype + ",Quantity: " + qty;
			} else {
				// trigger workflow
				String id = "Order sent to workflow. Workflow instance started with ID "
						+ triggerManualReOrder(materialname, materialtype, ordertype, qty);
				return id;
			}
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
			authorizationHeader =  "<base64 encoded value of <user-name>:<password>>";
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
	 * trigger the reorder process
	 * 
	 * @param temp
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String triggerManualReOrder(String materialname, String materialtype, String ordertype, int qty)
			throws ClientProtocolException, IOException {
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHTTPClient();
			String rulesRuntimeUrl = "https://bpmworkflowruntimewfs-s000xxxxtrial.hanatrial.ondemand.com/";
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
			String authorizationHeader =  "<base64 encoded value of <user-name>:<password>>";
			httpPost.addHeader("Authorization", "Basic " + authorizationHeader);

			// construct input data to the workflow service to create instance
			String fact = "{ \"definitionId\":\"reorder\",\"context\":{ \"materialname\":\"" + materialname
					+ "\",\"materialtype\":\"" + materialtype + "\",\"ordertype\":\"" + ordertype + "\",\"qty\":" + qty
					+ "}}";

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
