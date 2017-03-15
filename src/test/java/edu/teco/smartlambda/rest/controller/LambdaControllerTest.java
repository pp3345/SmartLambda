package edu.teco.smartlambda.rest.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.teco.smartlambda.authentication.entities.User;
import edu.teco.smartlambda.lambda.AbstractLambda;
import edu.teco.smartlambda.lambda.LambdaFacade;
import edu.teco.smartlambda.lambda.LambdaFactory;
import edu.teco.smartlambda.rest.exception.InvalidLambdaDefinitionException;
import edu.teco.smartlambda.rest.exception.LambdaNotFoundException;
import edu.teco.smartlambda.runtime.ExecutionResult;
import edu.teco.smartlambda.runtime.Runtime;
import edu.teco.smartlambda.runtime.RuntimeRegistry;
import edu.teco.smartlambda.shared.ExecutionReturnValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.AtMost;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import spark.Request;
import spark.Response;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LambdaFacade.class, User.class, RuntimeRegistry.class})
public class LambdaControllerTest {
	private static final Gson   gson                  = new Gson();
	private static final String TEST_USER_NAME        = "TestUser";
	private static final String TEST_RUNTIME          = "TestRuntime";
	private static final byte[] TEST_SRC              = "TestSource".getBytes(Charset.forName("US-ASCII"));
	private static final String TEST_LAMBDA_NAME      = "TestLambda";
	private static final String TEST_PARAMETER_NAME   = "TestParameter";
	private static final String TEST_PARAMETER_VALUE  = "TestParameterValue";
	private static final String TEST_EXECUTION_RESULT = "TestResult";
	
	private User          testUser;
	private Runtime       testRuntime;
	private LambdaFactory lambdaFactory;
	
	@RequiredArgsConstructor
	private static class LambdaRequest {
		private final Boolean async;
		private final String  runtime;
		private final byte[]  src;
	}
	
	@RequiredArgsConstructor
	private static class LambdaExecutionRequest {
		private final Boolean    async;
		private final JsonObject parameters;
	}
	
	@Data
	@AllArgsConstructor
	private static class LambdaResponse {
		private String  user;
		private String  name;
		private Boolean async;
		private String  runtime;
	}
	
	@Before
	public void setUp() throws Exception {
		PowerMockito.mockStatic(User.class);
		PowerMockito.mockStatic(RuntimeRegistry.class);
		PowerMockito.mockStatic(LambdaFacade.class);
		
		this.testUser = mock(User.class);
		when(User.getByName(TEST_USER_NAME)).thenReturn(Optional.ofNullable(this.testUser));
		when(this.testUser.getName()).thenReturn(TEST_USER_NAME);
		
		this.testRuntime = mock(Runtime.class);
		when(this.testRuntime.getName()).thenReturn(TEST_RUNTIME);
		
		final RuntimeRegistry runtimeRegistry = mock(RuntimeRegistry.class);
		when(RuntimeRegistry.getInstance()).thenReturn(runtimeRegistry);
		
		when(runtimeRegistry.getRuntimeByName(TEST_RUNTIME)).thenReturn(this.testRuntime);
		
		final LambdaFacade lambdaFacade = mock(LambdaFacade.class);
		when(LambdaFacade.getInstance()).thenReturn(lambdaFacade);
		this.lambdaFactory = mock(LambdaFactory.class);
		when(lambdaFacade.getFactory()).thenReturn(this.lambdaFactory);
	}
	
	private Pair<Response, AbstractLambda> doCreateLambda(final LambdaRequest lambdaRequest) throws Exception {
		final AbstractLambda lambda = mock(AbstractLambda.class);
		when(this.lambdaFactory.createLambda()).thenReturn(lambda);
		
		final Request request = mock(Request.class);
		
		when(request.body()).thenReturn(gson.toJson(lambdaRequest));
		when(request.params(":user")).thenReturn(TEST_USER_NAME);
		when(request.params(":name")).thenReturn(TEST_LAMBDA_NAME);
		
		final Response response = mock(Response.class);
		
		assertSame(Object.class, LambdaController.createLambda(request, response).getClass());
		
		return new ImmutablePair<>(response, lambda);
	}
	
	@Test
	public void createLambda() throws Exception {
		final Pair<Response, AbstractLambda> result   = this.doCreateLambda(new LambdaRequest(true, TEST_RUNTIME, TEST_SRC));
		final Response                       response = result.getLeft();
		final AbstractLambda                 lambda   = result.getRight();
		
		verify(response).status(201);
		verify(lambda).setName(TEST_LAMBDA_NAME);
		verify(lambda).setAsync(true);
		verify(lambda).setOwner(this.testUser);
		verify(lambda).setRuntime(this.testRuntime);
		verify(lambda).deployBinary(TEST_SRC);
		verify(lambda).save();
		verifyNoMoreInteractions(lambda);
	}
	
	@Test(expected = InvalidLambdaDefinitionException.class)
	public void createLambdaInvalidRuntime() throws Exception {
		this.doCreateLambda(new LambdaRequest(true, "does_not_exist", TEST_SRC));
	}
	
	@Test(expected = InvalidLambdaDefinitionException.class)
	public void createLambdaMissingSource() throws Exception {
		this.doCreateLambda(new LambdaRequest(true, TEST_RUNTIME, new byte[] {}));
	}
	
	private Pair<Response, AbstractLambda> doUpdateLambda(final LambdaRequest lambdaRequest, final String lambdaName) throws Exception {
		final AbstractLambda lambda = mock(AbstractLambda.class);
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), anyString())).thenReturn(Optional.empty());
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), eq(TEST_LAMBDA_NAME))).thenReturn(Optional.ofNullable(lambda));
		
		final Request request = mock(Request.class);
		
		when(request.body()).thenReturn(gson.toJson(lambdaRequest));
		when(request.params(":user")).thenReturn(TEST_USER_NAME);
		when(request.params(":name")).thenReturn(lambdaName);
		
		final Response response = mock(Response.class);
		
		assertSame(Object.class, LambdaController.updateLambda(request, response).getClass());
		
		return new ImmutablePair<>(response, lambda);
	}
	
	private Pair<Response, AbstractLambda> doUpdateLambda(final LambdaRequest lambdaRequest) throws Exception {
		return this.doUpdateLambda(lambdaRequest, TEST_LAMBDA_NAME);
	}
	
	@Test
	public void updateLambda() throws Exception {
		final Pair<Response, AbstractLambda> result   = this.doUpdateLambda(new LambdaRequest(true, TEST_RUNTIME, TEST_SRC));
		final Response                       response = result.getLeft();
		final AbstractLambda                 lambda   = result.getRight();
		
		verify(response).status(200);
		verify(lambda).setAsync(true);
		verify(lambda).setRuntime(this.testRuntime);
		verify(lambda).deployBinary(TEST_SRC);
		verify(lambda).update();
		verifyNoMoreInteractions(lambda);
	}
	
	@Test(expected = InvalidLambdaDefinitionException.class)
	public void updateLambdaInvalidRuntime() throws Exception {
		this.doUpdateLambda(new LambdaRequest(true, "does_not_exist", TEST_SRC));
	}
	
	@Test(expected = InvalidLambdaDefinitionException.class)
	public void updateLambdaMissingSource() throws Exception {
		this.doUpdateLambda(new LambdaRequest(true, TEST_RUNTIME, new byte[] {}));
	}
	
	@Test(expected = LambdaNotFoundException.class)
	public void updateLambdaUnknownLambda() throws Exception {
		this.doUpdateLambda(new LambdaRequest(true, TEST_RUNTIME, TEST_SRC), "does_not_exist");
	}
	
	@Test
	public void updateLambdaNoChanges() throws Exception {
		final Pair<Response, AbstractLambda> result   = this.doUpdateLambda(new LambdaRequest(null, null, null));
		final Response                       response = result.getLeft();
		final AbstractLambda                 lambda   = result.getRight();
		
		verify(response).status(200);
		verify(lambda).update();
		verifyNoMoreInteractions(lambda);
	}
	
	private Pair<Response, LambdaResponse> doReadLambda(final String lambdaName) throws Exception {
		final AbstractLambda lambda = mock(AbstractLambda.class);
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), anyString())).thenReturn(Optional.empty());
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), eq(TEST_LAMBDA_NAME))).thenReturn(Optional.ofNullable(lambda));
		
		when(lambda.getName()).thenReturn(TEST_LAMBDA_NAME);
		when(lambda.getOwner()).thenReturn(this.testUser);
		when(lambda.getRuntime()).thenReturn(this.testRuntime);
		when(lambda.isAsync()).thenReturn(true);
		
		final Request request = mock(Request.class);
		
		when(request.params(":user")).thenReturn(TEST_USER_NAME);
		when(request.params(":name")).thenReturn(lambdaName);
		
		final Response response = mock(Response.class);
		final Object   object   = LambdaController.readLambda(request, response);
		
		final Field user    = object.getClass().getDeclaredField("user");
		final Field name    = object.getClass().getDeclaredField("name");
		final Field async   = object.getClass().getDeclaredField("async");
		final Field runtime = object.getClass().getDeclaredField("runtime");
		
		assertEquals(4, object.getClass().getDeclaredFields().length);
		
		assertNotNull(user);
		assertSame(String.class, user.getType());
		assertNotNull(name);
		assertSame(String.class, name.getType());
		assertNotNull(async);
		assertSame(boolean.class, async.getType());
		assertNotNull(runtime);
		assertSame(String.class, runtime.getType());
		
		user.setAccessible(true);
		name.setAccessible(true);
		async.setAccessible(true);
		runtime.setAccessible(true);
		
		return new ImmutablePair<>(response,
				new LambdaResponse((String) user.get(object), (String) name.get(object), (Boolean) async.get(object),
						(String) runtime.get(object)));
	}
	
	@Test
	public void readLambda() throws Exception {
		final Pair<Response, LambdaResponse> result = this.doReadLambda(TEST_LAMBDA_NAME);
		
		assertEquals(TEST_USER_NAME, result.getRight().getUser());
		assertEquals(TEST_LAMBDA_NAME, result.getRight().getName());
		assertEquals(TEST_RUNTIME, result.getRight().getRuntime());
		assertEquals(true, result.getRight().getAsync());
		
		verify(result.getLeft()).status(200);
	}
	
	@Test(expected = LambdaNotFoundException.class)
	public void readLambdaUnknownLambda() throws Exception {
		this.doReadLambda("does_not_exist");
	}
	
	private Pair<Response, AbstractLambda> doDeleteLambda(final String lambdaName) throws Exception {
		final AbstractLambda lambda = mock(AbstractLambda.class);
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), anyString())).thenReturn(Optional.empty());
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), eq(TEST_LAMBDA_NAME))).thenReturn(Optional.ofNullable(lambda));
		
		final Request request = mock(Request.class);
		when(request.params(":user")).thenReturn(TEST_USER_NAME);
		when(request.params(":name")).thenReturn(lambdaName);
		
		final Response response = mock(Response.class);
		assertSame(Object.class, LambdaController.deleteLambda(request, response).getClass());
		
		return new ImmutablePair<>(response, lambda);
	}
	
	@Test
	public void deleteLambda() throws Exception {
		final Pair<Response, AbstractLambda> result = this.doDeleteLambda(TEST_LAMBDA_NAME);
		
		verify(result.getLeft()).status(200);
		verify(result.getRight()).delete();
		verifyNoMoreInteractions(result.getRight());
	}
	
	@Test(expected = LambdaNotFoundException.class)
	public void deleteLambdaUnknownLambda() throws Exception {
		this.doDeleteLambda("does_not_exist");
	}
	
	private Triple<Response, AbstractLambda, Object> doExecuteLambda(final LambdaExecutionRequest executionRequest,
			final boolean defaultAsync, final ExecutionReturnValue returnValue, final String lambdaName) throws Exception {
		final AbstractLambda lambda = mock(AbstractLambda.class);
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), anyString())).thenReturn(Optional.empty());
		when(this.lambdaFactory.getLambdaByOwnerAndName(eq(this.testUser), eq(TEST_LAMBDA_NAME))).thenReturn(Optional.ofNullable(lambda));
		
		when(lambda.isAsync()).thenReturn(defaultAsync);
		
		final Request request = mock(Request.class);
		when(request.params(":user")).thenReturn(TEST_USER_NAME);
		when(request.params(":name")).thenReturn(lambdaName);
		when(request.body()).thenReturn(gson.toJson(executionRequest));
		
		final Response response = mock(Response.class);
		if (returnValue != null) {
			final ExecutionResult executionResult = new ExecutionResult();
			executionResult.setExecutionReturnValue(returnValue);
			
			when(lambda.executeSync(anyString())).thenReturn(executionResult);
		}
		
		return new ImmutableTriple<>(response, lambda, LambdaController.executeLambda(request, response));
	}
	
	private Triple<Response, AbstractLambda, Object> doExecuteLambda(final LambdaExecutionRequest executionRequest,
			final boolean defaultAsync, final ExecutionReturnValue returnValue) throws Exception {
		return this.doExecuteLambda(executionRequest, defaultAsync, returnValue, TEST_LAMBDA_NAME);
	}
	
	@Test
	public void executeLambda() throws Exception {
		final JsonObject parameters = new JsonObject();
		parameters.addProperty(TEST_PARAMETER_NAME, TEST_PARAMETER_VALUE);
		
		Triple<Response, AbstractLambda, Object> result = this.doExecuteLambda(new LambdaExecutionRequest(null, parameters), true, null);
		verify(result.getMiddle()).executeAsync(gson.toJson(parameters));
		verify(result.getMiddle()).isAsync();
		verifyNoMoreInteractions(result.getMiddle());
		
		verify(result.getLeft()).status(202);
		assertEquals("", result.getRight());
		
		final ExecutionReturnValue returnValue = new ExecutionReturnValue(TEST_EXECUTION_RESULT, "");
		
		result = this.doExecuteLambda(new LambdaExecutionRequest(null, parameters), false, returnValue);
		verify(result.getMiddle()).executeSync(gson.toJson(parameters));
		verify(result.getMiddle()).isAsync();
		verifyNoMoreInteractions(result.getMiddle());
		
		verify(result.getLeft()).status(200);
		assertEquals(TEST_EXECUTION_RESULT, result.getRight());
	}
	
	@Test
	public void executeLambdaExplicitAsync() throws Exception {
		final Triple<Response, AbstractLambda, Object> result = this.doExecuteLambda(new LambdaExecutionRequest(true, null), false, null);
		verify(result.getMiddle()).executeAsync("");
		verify(result.getMiddle(), new AtMost(1)).isAsync();
		verifyNoMoreInteractions(result.getMiddle());
		
		verify(result.getLeft()).status(202);
		assertEquals("", result.getRight());
	}
	
	@Test
	public void executeLambdaExplicitSync() throws Exception {
		final ExecutionReturnValue executionReturnValue = new ExecutionReturnValue(TEST_EXECUTION_RESULT, "");
		
		final Triple<Response, AbstractLambda, Object> result =
				this.doExecuteLambda(new LambdaExecutionRequest(false, null), true, executionReturnValue);
		verify(result.getMiddle()).executeSync("");
		verify(result.getMiddle(), new AtMost(1)).isAsync();
		verifyNoMoreInteractions(result.getMiddle());
		
		verify(result.getLeft()).status(200);
		assertEquals(TEST_EXECUTION_RESULT, result.getRight());
	}
	
	@Test
	public void executeLambdaException() throws Exception {
		final ExecutionReturnValue executionReturnValue = new ExecutionReturnValue("", new Exception().fillInStackTrace());
		
		final Triple<Response, AbstractLambda, Object> result =
				this.doExecuteLambda(new LambdaExecutionRequest(false, null), true, executionReturnValue);
		verify(result.getMiddle()).executeSync("");
		verify(result.getMiddle(), new AtMost(1)).isAsync();
		verifyNoMoreInteractions(result.getMiddle());
		
		verify(result.getLeft()).status(502);
		assertEquals("", result.getRight());
	}
	
	@Test
	public void getLambdaList() throws Exception {
		
	}
	
	@Test
	public void getStatistics() throws Exception {
		
	}
}