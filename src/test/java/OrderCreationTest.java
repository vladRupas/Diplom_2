import com.github.javafaker.Faker;
import com.google.gson.Gson;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class OrderCreationTest {

    private final Faker faker = new Faker();
    private final Gson gson = new Gson();

    private String email;
    private String password;
    private String name;
    private String accessToken = null;

    private List<String> ingredientIds;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        generateTestUserData();
        registerUser();
        fetchIngredientIds();
    }

    @Test
    @DisplayName("Create order with authorization")
    @Description("Checks that a user can create an order when authorized and sends valid ingredients")
    public void createOrderWithAuthorization() {

        OrderRequest order = new OrderRequest(ingredientIds);
        String orderJson = gson.toJson(order);

        Response response = sendCreateOrderRequest(orderJson, accessToken);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("name", notNullValue())
                .body("order.number", notNullValue());
    }

    @Test
    @DisplayName("Create order without authorization")
    @Description("Checks that an order can be created without providing authorization")
    public void createOrderWithoutAuthorization() {

        OrderRequest order = new OrderRequest(ingredientIds);
        String orderJson = gson.toJson(order);

        Response response = sendCreateOrderRequest(orderJson, null);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("name", notNullValue())
                .body("order.number", notNullValue());
    }

    @Test
    @DisplayName("Create order with ingredients")
    @Description("Create an order with valid ingredients")
    public void createOrderWithIngredients() {

        OrderRequest order = new OrderRequest(ingredientIds);
        String json = gson.toJson(order);

        Response response = sendCreateOrderRequest(json, accessToken);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("name", notNullValue())
                .body("order.number", notNullValue());
    }

    @Test
    @DisplayName("Create order without ingredients")
    @Description("Try to create an order with empty ingredient list")
    public void createOrderWithoutIngredients() {

        String json = gson.toJson(new OrderRequest(List.of()));

        Response response = sendCreateOrderRequest(json, accessToken);
        response.then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Create order with invalid ingredient hashes")
    @Description("Try to create an order with invalid ingredient IDs")
    public void createOrderWithInvalidIngredients() {

        List<String> invalidIngredients = List.of("invalid_hash_1", "invalid_hash_2");
        OrderRequest order = new OrderRequest(invalidIngredients);
        String json = gson.toJson(order);

        Response response = sendCreateOrderRequest(json, accessToken);
        response.then()
                .statusCode(500);
    }

    @Step("Generate test user data")
    private void generateTestUserData() {
        email = faker.internet().emailAddress();
        password = faker.internet().password();
        name = faker.name().firstName();
    }

    @Step("Register test user and retrieve access token")
    private void registerUser() {
        NewUser user = new NewUser(email, password, name);
        String json = gson.toJson(user);

        Response response = given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");

        response.then().statusCode(200);
        accessToken = response.path("accessToken");
    }

    @Step("Fetch ingredient IDs for order")
    private void fetchIngredientIds() {
        Response response = given()
                .get("/api/ingredients");

        response.then().statusCode(200);
        ingredientIds = response.jsonPath().getList("data._id");
    }

    @Step("Send request to create order")
    private Response sendCreateOrderRequest(String json, String token) {
        if (token != null) {
            return given()
                    .header("Content-type", "application/json")
                    .header("Authorization", token)
                    .body(json)
                    .post("/api/orders");
        } else {
            return given()
                    .header("Content-type", "application/json")
                    .body(json)
                    .post("/api/orders");
        }
    }

    @Step("Deleting user by accessToken")
    private void deleteUser(String token) {
        given()
                .header("Authorization", token)
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    @After
    public void cleanup() {
        if (accessToken != null) {
            deleteUser(accessToken);
        }
    }
}