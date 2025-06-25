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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class NewUserTest {

    private final Gson gson = new Gson();
    private final Faker faker = new Faker();

    private String email;
    private String password;
    private String name;
    private String accessToken = null;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @Test
    @DisplayName("User Creation")
    @Description("Testing new user creation with various scenarios")
    public void createUser() {

        generateTestData();

        NewUser validUser = new NewUser(email, password, name);
        String userJson = gson.toJson(validUser);

        // Успешное создание
        Response createResponse = createUserStep(userJson);
        createResponse.then()
                .statusCode(200)
                .body("success", equalTo(true));

        accessToken = createResponse.path("accessToken");

        // Попытка создать дубликат
        createDuplicateUserStep(userJson)
                .then()
                .statusCode(403)
                .body("message", equalTo("User already exists"));

        // Создание без поля email
        String noEmailJson = gson.toJson(new NewUser(null, password, name));
        createUserWithoutEmailStep(noEmailJson)
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));

        // Создание без поля password
        String noPasswordJson = gson.toJson(new NewUser(email, null, name));
        createUserWithoutPasswordStep(noPasswordJson)
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));

        // Создание без поля name
        String noNameJson = gson.toJson(new NewUser(email, password, null));
        createUserWithoutNameStep(noNameJson)
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Step("Generating test data")
    private void generateTestData() {
        email = faker.internet().emailAddress();
        password = faker.internet().password();
        name = faker.name().firstName();
    }

    @Step("Creating a user with valid data")
    private Response createUserStep(String json) {
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Attempting to recreate user")
    private Response createDuplicateUserStep(String json) {
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Creating a user without email")
    private Response createUserWithoutEmailStep(String json) {
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Creating a user without password")
    private Response createUserWithoutPasswordStep(String json) {
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Creating a user without name")
    private Response createUserWithoutNameStep(String json) {
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Deleting user by accessToken")
    private void deleteUserByToken(String accessToken) {
        given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    @After
    public void cleanup() {
        if (accessToken != null) {
            deleteUserByToken(accessToken);
        }
    }
}