package edu.teco.smartlambda.integrationTest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.teco.smartlambda.Application;
import edu.teco.smartlambda.authentication.entities.User;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.HashMap;

/**
 * Created on 04.04.17.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntegrationTest {
	private static final String smartLambdaURL = "http://localhost:8080";
	private static final String testUserName   = "IntegrationTestUser";
	private static String testUserPrimaryKey;
	private static String testUserDeveloperKey;
	private static final String testUserDeveloperKeyName = "IntegrationTestDeveloper";
	
	@BeforeClass
	public static void setupTestUser() throws Exception {
		//Delete User with the same name as testUserName, if present.
		Application.getInstance().getSessionFactory().getCurrentSession().beginTransaction();
		User.getByName(testUserName).ifPresent(user -> Application.getInstance().getSessionFactory().getCurrentSession().delete(user));
		final Transaction transaction = Application.getInstance().getSessionFactory().getCurrentSession().getTransaction();
		if (transaction.isActive()) transaction.commit();
		
		//Create new Account
		final HttpResponse response = registerTestUser(testUserName);
		assert response.getStatus() == 201;
		final Gson       gson       = new Gson();
		final JsonObject jsonObject = gson.fromJson(response.getBody().toString(), JsonObject.class);
		testUserPrimaryKey = jsonObject.get("primaryKey").getAsString();
	}
	
	private static HttpResponse registerTestUser(final String name) throws UnirestException {
		final HashMap<String, Object> body       = new HashMap<>();
		final HashMap<String, String> parameters = new HashMap<>();
		parameters.put("name", name);
		body.put("parameters", parameters);
		body.put("identityProvider", "null");
		
		final Gson   gson       = new Gson();
		final String bodyString = gson.toJson(body);
		
		return Unirest.post(smartLambdaURL + "/register").body(bodyString).asString();
	}
	
	@Test
	public void _1_registerUserViaNullIdentityProvider() throws Exception { //TF010
		final String userName = "IntegrationTest.registerUserViaNullIdentityProvider";
		
		Application.getInstance().getSessionFactory().getCurrentSession().beginTransaction();
		User.getByName(userName).ifPresent(user -> Application.getInstance().getSessionFactory().getCurrentSession().delete(user));
		final Transaction transaction = Application.getInstance().getSessionFactory().getCurrentSession().getTransaction();
		if (transaction.isActive()) transaction.commit();
		
		final HttpResponse response = registerTestUser(userName);
		Assert.assertEquals(response.getStatus(), 201);
		Assert.assertEquals(response.getStatusText(), "Created");
		final Gson       gson       = new Gson();
		final JsonObject jsonObject = gson.fromJson(response.getBody().toString(), JsonObject.class);
		Assert.assertEquals(jsonObject.get("name").getAsString(), userName);
		testUserPrimaryKey = jsonObject.get("primaryKey").getAsString();
	}
	
	@Test
	public void _2_createDeveloperKey() throws Exception { //TF020
		//final HashMap<String, Object> body = new HashMap<>();
		//body.put("SmartLambda-Key", testUserPrimaryKey);
		
		final Gson gson = new Gson();
		//final String bodyString = gson.toJson(body);
		
		final HttpResponse<String> response =
				Unirest.put(smartLambdaURL + "/key/" + testUserDeveloperKeyName).header("SmartLambda-Key", testUserPrimaryKey).asString();
		Assert.assertEquals(201, response.getStatus());
		Assert.assertEquals("Created", response.getStatusText());
		
		final JsonPrimitive jsonPrimitive = gson.fromJson(response.getBody(), JsonPrimitive.class);
		final String        key           = jsonPrimitive.getAsString();
		Assert.assertNotNull(key);
		testUserDeveloperKey = key;
	}
}